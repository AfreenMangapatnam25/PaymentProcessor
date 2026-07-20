# Limit Service

The financial guardrail that enforces spending/transaction limits before a payment is authorized, via a **reserve →
commit → release** capacity lifecycle.

> This document describes how to build, run and call the service. The functional specification lives in [
`LimitReadme.md`](./LimitReadme.md).

---

## 1. Role in the platform

- Evaluates a transaction against configured limits (amount/count, over `PER_TRANSACTION`/`DAILY`/`WEEKLY`/`MONTHLY`/
  `LIFETIME` windows, scoped to `GLOBAL`/`CUSTOMER`/`MERCHANT`/`CURRENCY`/`COUNTRY`) and returns `APPROVED`/`FLAGGED`/
  `DECLINED`.
- Holds capacity against a transaction via `LimitReservation` + `ReservationLine` rows before a payment is authorized,
  so two concurrent payments cannot both consume the last of a limit (pessimistic `SELECT … FOR UPDATE` locking on
  `usage_counter`, deterministic lock order to avoid deadlocks, plus an optimistic `@Version` column).
- Moves reserved capacity to committed on capture (`commit`, supports partial capture) or gives it back on
  failure/void/manual override (`release`).
- Auto-expires stale reservations past their TTL via `ReservationExpiryScheduler`.
- Exposes admin CRUD for limit configurations (`LimitConfigController`) and read-only usage/headroom queries.
- Publishes best-effort domain events to Kafka and writes an append-only audit trail (`LimitAuditLog`) for every
  decision.

---

## 2. Tech stack

| Concern            | Choice                                                                                    |
|--------------------|-------------------------------------------------------------------------------------------|
| Language / runtime | Java 21 (Gradle toolchain), Spring Boot 3.3.2                                             |
| Persistence        | PostgreSQL + Spring Data JPA (Hibernate)                                                  |
| Schema management  | Flyway (`V1__init_limit_schema.sql`, `V2__seed_default_limits.sql`), `ddl-auto: validate` |
| Caching / atomics  | Redis (Spring Data Redis / Jedis)                                                         |
| Messaging          | Apache Kafka (`spring-kafka`, JSON-serialized events, best-effort publish)                |
| Service discovery  | Netflix Eureka client                                                                     |
| API docs           | springdoc-openapi (Swagger UI)                                                            |
| Observability      | Spring Boot Actuator + Micrometer/Prometheus                                              |

---

## 3. API surface

Base path `/api/v1`. Default port **8085**.

**`LimitCheckController`** (`/api/v1/limits`)

- `POST /check` — dry-run evaluation (no capacity held); returns `decision` (`APPROVED`/`FLAGGED`/`DECLINED`) plus
  violations.
- `GET /usage?customerId=&merchantId=&currency=` — current consumption/headroom.

**`ReservationController`** (`/api/v1/reservations`) — called by payment-service

- `POST /` — evaluate + reserve capacity; `201` with the reservation, or `422 LIMIT_EXCEEDED` on a hard breach. Accepts
  an `idempotencyKey` so retries are safe.
- `POST /{reservationId}/commit` — capture (optionally partial via `capturedAmount`).
- `POST /{reservationId}/release` — release held capacity (`reason` optional).
- `GET /{reservationId}` — fetch by id.
- `GET /?transactionId=` — fetch reservations for a transaction.

**`LimitConfigController`** (`/api/v1/limit-configs`) — admin CRUD

- `POST /`, `PUT /{id}`, `DELETE /{id}` (soft-delete/disable), `GET /{id}`, `GET /` (list).

---

## 4. Data model

- `LimitConfiguration` — one rule: `dimension` (`AMOUNT`|`COUNT`) × `window` × `scope` (+ optional `scopeId` for
  entity-specific overrides) × `enforcementMode` (`HARD`|`SOFT`).
- `UsageCounter` — the mutable per-window/per-scope consumption row that reserve/commit/release mutate under lock.
- `LimitReservation` + `ReservationLine` — one reservation per transaction, one line per limit it was checked/held
  against; carries `ReservationStatus` (reserved/committed/released/expired) and a TTL.
- `LimitAuditLog` — append-only record of every limit decision/action (`AuditAction`).

---

## 5. Inter-service integration

**payment-service now really calls this service.** As of today's wiring, `payment-service`'s new `LimitClient` (in its
`connector` package) is a real WebClient integration — not a placeholder — against these exact endpoints:

- `POST /api/v1/reservations` — called from `PaymentOrchestrationService` **before the fraud check**, to reserve
  capacity for the transaction.
- `POST /api/v1/reservations/{id}/release` — called on fraud block, connector decline, or void, to give the held
  capacity back.
- `POST /api/v1/reservations/{id}/commit` — called on successful capture.

Because reservation is a *hard* control (not an advisory signal like fraud), `LimitClient.reserve` fails **closed**: if
limit-service is unreachable or times out (5s), the reservation is treated as a decline (`LIMIT_UNAVAILABLE`/
`LIMIT_EXCEEDED`) rather than letting spend bypass limits during an outage. `commit`/`release` are best-effort — a
failure there is logged, not surfaced, and the reservation will still eventually auto-expire via the TTL sweeper.

Routed through gateway-service at `/api/v1/limits/**`, `/api/v1/limit-configs/**`, `/api/v1/reservations/**` (see
`gateway-service/src/main/resources/application.yml`, `LIMIT_SERVICE_URI`, default `http://limit-service:8080`).

Kafka events (`limit.reserved`, `limit.released`, `limit.exceeded`) are published best-effort for downstream consumers;
a publish failure never fails an already-persisted decision.

---

## 6. Running locally

Prerequisites: JDK 21, PostgreSQL, Redis, and (optionally) Kafka + Eureka.

```bash
createdb limit_local
gradle clean build            # or ./gradlew clean build if a wrapper is present
SPRING_PROFILES_ACTIVE=local gradle bootRun
```

Profiles: `local` (localhost, verbose SQL), `dev`, `prod` (SSL, tuned pools, Prometheus). Secrets in `dev`/`prod` come
from env vars (`DB_USERNAME`, `DB_PASSWORD`, `REDIS_PASSWORD`, `SSL_KEYSTORE_*`).

Default port: **8085**. Once running:

- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`
- Health: `http://localhost:8085/actuator/health`

---

## 7. Design notes

- **Reserve → commit → release** as a first-class state machine (not just a status flag) lets partial capture, TTL
  expiry, and manual release all converge on the same capacity-accounting code path instead of being bolted on
  separately.
- **`SELECT … FOR UPDATE` with deterministic lock ordering** on `usage_counter` closes the double-spend race across
  concurrent instances without needing a distributed lock service; the `@Version` column is a cheap second line of
  defense against any residual anomaly.
- **Fail-closed reservation, fail-open advisory checks** — the split between the hard reserve/commit/release control and
  the softer `/limits/check` dry-run reflects that a limit breach must never be silently bypassed, whereas an advisory
  check can degrade gracefully.
- **Best-effort Kafka + durable DB audit log** — the audit trail is the source of truth for compliance, while Kafka is
  treated as a notification side-channel that must never block or fail a decision.

---

## Project layout

```
src/main/java/com/paymentprocessor/limit
├── config        LimitProperties, OpenApiConfig
├── controller    Check, Reservation, LimitConfig REST controllers
├── domain
│   ├── entity    LimitConfiguration, UsageCounter, LimitReservation,
│   │             ReservationLine, LimitAuditLog
│   └── enums     scope / dimension / window / enforcement / status / decision
├── dto           request & response records + ApiError
├── event         LimitEvent records + LimitEventPublisher
├── exception     typed exceptions + GlobalExceptionHandler
├── repository    Spring Data JPA repositories (incl. locking query)
└── service       LimitResolver, WindowResolver, LimitEvaluationService,
                  ReservationService, LimitConfigService, AuditService,
                  ReservationExpiryScheduler
src/main/resources/db/migration   Flyway V1 (schema) + V2 (seed defaults)
```

## Scope

This build implements the core engine: limit configuration, usage counters, per-transaction/daily/weekly/monthly
amount & count limits, the full reserve/commit/release/expire lifecycle, audit trail, Kafka events and REST APIs.
Velocity, geographic/country and credit-line limits described in `LimitReadme.md` are intentionally left as follow-on
work; the resolver/evaluation pipeline are structured so they slot in as additional scopes/dimensions without reworking
the core.
