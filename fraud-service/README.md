# Fraud Service

Real-time fraud risk scoring and decisioning for card authorizations, plus case management and rule/list administration
for fraud analysts.

## Role in the platform

- Exposes `POST /api/fraud/evaluate`, called synchronously by `payment-service`'s `FraudClient` on every card
  authorization before the payment proceeds.
- Runs an in-process, ordered pipeline of `SignalEvaluator`s (rule engine, block/allow lists, velocity, geo mismatch,
  AML/entity risk, ML scoring) that build up a shared feature map (`RiskContext`) and emit `RuleHit`s.
- Aggregates rule points and an ML probability into a 0–100 risk score, then applies `DecisionPolicy` to resolve one of
  `APPROVE`, `CHALLENGE`, `REVIEW`, `DECLINE`, `ESCALATE` — including a mandatory-review floor for high-value
  transactions.
- Persists every evaluation as an audit record (`RiskAssessment`) with the score, decision, fired rules, and the full
  feature snapshot (as JSON text), and publishes an in-process domain event (`FraudEvent`) reflecting the outcome.
- Provides full CRUD administration APIs for the underlying fraud data: devices, blacklist/whitelist entries,
  data-driven rules, ML model registry entries, fraud investigation cases, and risk-assessment audit records.
- Fails closed within the service (it always returns a decision), but the caller (`payment-service`) is configured to
  fail *open* (auto-approve) if fraud-service is unreachable or slow, so a risk-scoring outage does not halt all
  payments.

## Tech stack

- Java 21, Spring Boot 3.3.2 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`)
- Default port: **8088** (`server.port`, overridable via `SERVER_PORT`)
- Datastore: **PostgreSQL** (`fraud_db`), accessed via Spring Data JPA/Hibernate (`ddl-auto: validate` — schema is owned
  by Flyway, not Hibernate)
- Schema migrations: **Flyway** (`flyway-core`, `flyway-database-postgresql`), `baseline-on-migrate: true`, single
  migration `V1__init_schema.sql`
- Connection pool: HikariCP (max 20, min-idle 5)
- Lombok for entity boilerplate (`@Getter/@Setter/@Builder`)
- Jackson `ObjectMapper` used directly (not just via Spring MVC) to serialize rule hits/features into the audit record
- No messaging broker dependency — no Kafka producer/consumer in this service; eventing is in-process (
  `ApplicationEventPublisher`), not cross-service

## API surface

All controllers are plain `@RestController`s following the same CRUD shape (`GET` list, `POST` create, `GET /{id}`,
`PUT /{id}`, `DELETE /{id}` → 204) unless noted:

| Base path                  | Entity           | Notes                                                                                                                                                                                                                                                                                           |
|----------------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST /api/fraud/evaluate` | —                | The one non-CRUD, non-obvious endpoint. Real-time scoring entry point; request includes amount, currency, card BIN, IP/billing country, device signals; response includes score, decision, ML contribution, confidence, fired rules, and (for `REVIEW`/`ESCALATE`) a generated `reviewQueueId`. |
| `/api/devices`             | `Device`         | Device fingerprints per merchant, used by velocity/device-risk evaluators                                                                                                                                                                                                                       |
| `/api/cases`               | `FraudCase`      | Analyst-facing fraud investigation cases (table `cases`)                                                                                                                                                                                                                                        |
| `/api/lists`               | `ListEntry`      | Blacklist/whitelist entries (table `lists`; kind stored in `list_kind`)                                                                                                                                                                                                                         |
| `/api/model-registry`      | `ModelRegistry`  | Deployed/deployable ML model versions                                                                                                                                                                                                                                                           |
| `/api/risk-assessments`    | `RiskAssessment` | Read/write access to the audit trail produced by `/api/fraud/evaluate`                                                                                                                                                                                                                          |
| `/api/rules`               | `Rule`           | Data-driven, versioned fraud rules (SpEL expressions evaluated by `RuleEngineEvaluator`)                                                                                                                                                                                                        |

Note: the CRUD controllers accept raw JPA entities as request/response bodies (no separate DTOs), so callers of the
admin APIs see the JPA field names directly (e.g. `merchantId`, `createdAt`).

## Data model

All entities use a `UUID` primary key (`GenerationType.UUID`) and are independent, unrelated tables — there are **no
JPA `@ManyToOne`/`@OneToMany` relationships**; cross-entity association (e.g. a `RiskAssessment` to the `Rule`s that
fired) is done by storing IDs/names as JSON text, not foreign keys.

- **Device** (`devices`) — `fingerprint`, `merchantId`, `firstSeen`, `lastSeen`, `metadata` (free-text). Indexed on
  fingerprint and merchant.
- **FraudCase** (`cases`) — `merchantId`, `intentId`, `status`, `assignee`, `createdAt`. Indexed on intent, merchant,
  status.
- **ListEntry** (`lists`) — `merchantId`, `list` (mapped from column `list_kind`, e.g. BLACKLIST/WHITELIST),
  `attribute` (e.g. CARD/IP/USER/DEVICE/EMAIL_DOMAIN/MERCHANT), `value`, `reason`, `expiresAt`. Indexed on (attribute,
  value) and merchant.
- **ModelRegistry** (`model_registry`) — `name`, `version`, `status`, `createdAt`. Indexed on name and status.
- **RiskAssessment** (`risk_assessments`) — `intentId`, `merchantId`, `score` (`NUMERIC(5,2)`), `decision`,
  `triggeredRules` (JSON text), `features` (JSON text snapshot of the full feature map), `model`, `latencyMs`,
  `createdAt`. Indexed on intent, merchant, created_at. This is the audit trail written by `RiskScoringEngine` on every
  `/api/fraud/evaluate` call (persistence failures are swallowed/logged so they never block the decision).
- **Rule** (`rules`) — `scope`, `name`, `expr` (SpEL expression text evaluated against the feature map), `action` (maps
  to `RuleAction`), `priority`, `enabled`, `version`. Indexed on name, enabled, scope.

## Inter-service integration

**Inbound:**

- `payment-service` → `POST /api/fraud/evaluate` via `payment-service`'s `FraudClient` (
  `payment-service/src/main/java/.../connector/FraudClient.java`). Called before authorizing every card payment, with a
  5-second timeout. `FraudClient` fails open (treats fraud-service as unreachable ⇒ auto-approve) on any exception or
  timeout — this is a `payment-service`-side policy, not something fraud-service does. `FraudClient` maps
  fraud-service's `APPROVE`/`CHALLENGE` to a proceed decision and `REVIEW`/`DECLINE`/`ESCALATE` to a block, and sends
  amounts in **major units** (e.g. `54.00` for $54), converted from `payment-service`'s minor-unit representation.
- No gateway route for fraud-service was found in `gateway-service/src/main/resources/application.yml` beyond generic
  references — fraud-service is invoked service-to-service, not exposed through the edge gateway for evaluation traffic.
  The admin CRUD endpoints (`/api/cases`, `/api/rules`, etc.) have no evidence of an external caller in this codebase;
  they read as back-office/analyst APIs.

**Outbound:**

- None. fraud-service does not call any other service via HTTP/WebClient — the pipeline evaluators (rule engine, lists,
  velocity, geo, AML, ML scoring) all operate on data already present in the request or in fraud-service's own Postgres
  tables (`InMemoryVelocityStore` for velocity, no external ML service call — `MlScorer` is local).
- Domain events (`FraudEvent`, e.g. `FRAUD_DETECTED`, `MANUAL_REVIEW_REQUIRED`, `FRAUD_ESCALATED`) are published only on
  the local Spring `ApplicationEventPublisher` — **no Kafka topic is produced or consumed** by this service (no
  `KafkaTemplate`/`@KafkaListener` anywhere in `fraud-service`, and no Kafka dependency in `build.gradle`). Any
  cross-service fan-out of these events would need to be added.

## Running locally

Environment variables (all optional, with defaults suited to local dev):

| Variable      | Default     | Purpose           |
|---------------|-------------|-------------------|
| `DB_HOST`     | `localhost` | Postgres host     |
| `DB_PORT`     | `5432`      | Postgres port     |
| `DB_NAME`     | `fraud_db`  | Database name     |
| `DB_USERNAME` | `postgres`  | Postgres user     |
| `DB_PASSWORD` | `postgres`  | Postgres password |
| `SERVER_PORT` | `8088`      | HTTP port         |

Prerequisites: a running Postgres instance with a `fraud_db` database (Flyway will create the schema on startup via
`V1__init_schema.sql`; `baseline-on-migrate: true` so it's safe to point at an existing empty DB).

```bash
# from repo root
./gradlew :fraud-service:bootRun

# or, from fraud-service/
../gradlew bootRun
```

Service listens on `http://localhost:8088` (adjust `SERVER_PORT` if 8088 is taken — note it's fraud-service's assigned
port in the platform port map).

## Design notes

- **Ordered, fault-isolated evaluator pipeline.** `SignalEvaluator`s run in `order()` sequence over a shared, mutable
  `RiskContext`, but `RiskScoringEngine.evaluate()` wraps each evaluator call in a try/catch so one failing signal (e.g.
  a bug in `MlScoringEvaluator`) degrades the score rather than throwing a 500 for the whole authorization — a
  defensible tradeoff for a service sitting in the hot path of every card payment.
- **Score aggregation is transparent and replayable.** The final score is `rulePoints + mlProbability * mlMaxPoints`,
  clamped to [0,100] and scaled down for whitelisted entities; the full feature map and fired-rule list are serialized
  to the `risk_assessments` audit row, so any decision can be reconstructed/explained after the fact without re-running
  the pipeline.
- **Audit persistence is explicitly best-effort.** `RiskScoringEngine.persist()` catches and logs any failure rather
  than propagating it — the design deliberately prioritizes returning a timely fraud decision to `payment-service` (
  which itself has only a 5s budget) over guaranteeing every evaluation is durably recorded.
- **No cross-service relational integrity.** Every entity is a flat, independent table (no FKs, no JPA associations)
  with references to other concepts (rules fired, model used) stored as denormalized IDs/names/JSON text. This keeps
  writes cheap and avoids N+1s on the hot evaluate path, at the cost of needing application-level joins for analyst
  tooling.
- **Fail-open contract lives in the caller, not here.** fraud-service itself always computes and returns a real
  decision; the "fail open on fraud-service being down" policy is implemented entirely in `payment-service`'s
  `FraudClient`, which is a deliberate separation — fraud-service doesn't need to know or care how conservatively its
  unavailability is handled.
