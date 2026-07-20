# Dispute Service

Manages the full chargeback / dispute lifecycle — intake, evidence collection, representment, arbitration, and liability
settlement — for the payment platform.

## Role in the platform

- Owns the chargeback lifecycle: receives inbound chargebacks/retrievals, tracks status through a formal state machine,
  and enforces which transitions are legal (`DisputeStateMachine`).
- Manages evidence packages (upload, review, malware-scan/OCR metadata) submitted by merchants to fight a dispute.
- Tracks representments (evidence submissions to the card network), issuer responses, and escalation to
  pre-arbitration/arbitration.
- Computes and records **liability** (who eats the loss — merchant, platform, or issuer) and drives fund movement:
  posting chargeback debits, recovering funds from settlement/reserve, and reversing/releasing them on a win.
- Runs a scheduled deadline sweep (`DeadlineMonitorJob`) that auto-loses disputes past their response deadline and sends
  reminder notifications.
- Publishes in-process domain events (`DisputeCreatedEvent`, `ChargebackReceivedEvent`, `DisputeWonEvent`,
  `DisputeLostEvent`) consumed by a logging listener that stands in for a future bridge to
  Reporting/Analytics/Notification/Audit.
- Maintains a reason-code catalog per card network (win rates, required/optional evidence, response-day windows) to
  guide merchants.

## Tech stack

- **Java / Spring Boot 3.3.2**, `spring-boot-starter-web` + `spring-boot-starter-webflux` (WebClient for outbound calls;
  the service itself is a servlet/MVC app), `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`.
- **Default port: 8090** (`server.port`, overridable via `SERVER_PORT`).
- **Datastore: PostgreSQL** (`org.postgresql:postgresql`, driver `org.postgresql.Driver`), migrated with **Flyway** (
  `flyway-core` + `flyway-database-postgresql`, `V1__init_schema.sql`, `ddl-auto: validate`,
  `baseline-on-migrate: true`). This is a real database — the service was migrated off in-memory H2 today.
- No messaging broker dependency in `build.gradle` — inter-service "events" beyond synchronous HTTP are in-process
  Spring `ApplicationEvent`s only (see below), not Kafka.

## API surface

Base path `/api/v1` (with slight variation per controller):

| Controller                    | Base path                      | Endpoints                                                                                                                                                                                                                                                                                                                                                                        |
|-------------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DisputeController`           | `/api/v1/disputes`             | `POST /` create from chargeback · `GET /` list (optional `merchantId` filter) · `GET /{id}` full detail (dispute + liability + evidence + representments + timeline) · `GET /{id}/status` · `POST /{id}/request-evidence` (OPEN→PENDING_EVIDENCE) · `POST /{id}/review` (→EVIDENCE_REVIEW) · `POST /{id}/accept` (accept liability, optional `actor` param) · `POST /{id}/close` |
| `EvidenceController`          | `/api/v1`                      | `POST /disputes/{disputeId}/evidence` · `GET /disputes/{disputeId}/evidence` · `GET /evidence/{evidenceId}` · `POST /evidence/{evidenceId}/review`                                                                                                                                                                                                                               |
| `RepresentmentController`     | `/api/v1/disputes/{disputeId}` | `POST /representments` · `GET /representments` · `POST /issuer-response` · `POST /arbitration` · `POST /arbitration-decision`                                                                                                                                                                                                                                                    |
| `LiabilityController`         | `/api/v1/disputes/{disputeId}` | `GET /liability` (current) · `GET /liability/history`                                                                                                                                                                                                                                                                                                                            |
| `DisputeEventController`      | `/api/v1/disputes/{disputeId}` | `GET /timeline`                                                                                                                                                                                                                                                                                                                                                                  |
| `ReasonCodeCatalogController` | `/api/v1/reason-codes`         | `GET /` · `GET /{network}` · `GET /{network}/{code}`                                                                                                                                                                                                                                                                                                                             |

Non-obvious bits: `GET /api/v1/disputes/{id}` is a heavier "detail" endpoint that fan-outs across four services
internally (dispute + liability + evidence + representments + timeline) rather than a plain entity fetch; evidence
review and arbitration decisions are separate endpoints from creation, reflecting a two-actor (submitter/reviewer)
workflow.

## Data model

Key entities (see `V1__init_schema.sql`): `disputes` (root aggregate — chargeback id, network, type, stage, status,
amount, deadline, liability party), `dispute_events` (append-only timeline, FK to dispute, records `from_status`/
`to_status`), `evidence` (file metadata, OCR text, malware-scan flag, review lifecycle), `liability` (disputed/fee/total
amounts, reserve tier, ledger hold/journal ids, reversed flag), `representments` (network submission, issuer response,
arbitration outcome fields), `reason_code_catalog` (composite PK `network+code`, win rate, evidence requirements).

### Dispute lifecycle state machine (`DisputeStatus` / `DisputeStateMachine`)

States:
`OPEN, PENDING_EVIDENCE, EVIDENCE_REVIEW, REPRESENTED, PRE_ARBITRATION, ARBITRATION, ACCEPTED, WON, LOST, CLOSED`.
Terminal states are `WON`, `LOST`, `ACCEPTED`, `CLOSED` (all four, not just WON/LOST).

Verified transitions actually encoded in `DisputeStateMachine`:

```
OPEN            -> PENDING_EVIDENCE | ACCEPTED | LOST
PENDING_EVIDENCE-> EVIDENCE_REVIEW  | ACCEPTED | LOST
EVIDENCE_REVIEW -> REPRESENTED      | ACCEPTED | LOST
REPRESENTED     -> WON | LOST | PRE_ARBITRATION
PRE_ARBITRATION -> ARBITRATION | ACCEPTED | LOST | WON
ARBITRATION     -> WON | LOST
ACCEPTED        -> CLOSED
WON             -> CLOSED
LOST            -> CLOSED
CLOSED          -> (none)
```

Notably: a dispute can be lost directly from `OPEN`/`PENDING_EVIDENCE`/`EVIDENCE_REVIEW` (e.g. deadline blown before
evidence is even submitted — this is how `DeadlineMonitorJob`'s auto-lose sweep works), and `PRE_ARBITRATION` can
resolve straight to `WON` without formally entering `ARBITRATION`. All four terminal-ish outcomes (`ACCEPTED`, `WON`,
`LOST`) still require an explicit `close` transition to reach the true terminal `CLOSED` state — they are not themselves
final in storage even though `DisputeStatus.isTerminal()` treats them as terminal for transition purposes.

## Inter-service integration

Every outbound port lives in `integration/` (interfaces) with real implementations in `integration/web/` wired via named
`WebClient` beans (`WebClientConfig`, base URLs from `application.yml`, 5s timeout, errors caught and logged rather than
propagated — calls degrade to a synthetic fallback id instead of failing the request).

| Client                                                                  | Method                    | Status                    | Detail                                                                                                                                                                                                                                                                                                                                       |
|-------------------------------------------------------------------------|---------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LedgerClient` (`WebLedgerClient` → ledger-service `:8092`)             | `postChargebackDebit`     | **REAL**                  | `POST /api/v1/journals` — posts a balanced debit/credit journal (merchant reserve debit, platform chargeback-clearing credit)                                                                                                                                                                                                                |
|                                                                         | `reverseChargeback`       | **REAL**                  | `POST /api/v1/journals/{id}/reverse`                                                                                                                                                                                                                                                                                                         |
|                                                                         | `finalizeLoss`            | **STUBBED**               | No dedicated "finalize loss" endpoint on ledger-service, and the method signature has no amount to post a balanced journal with. Logs + returns a synthetic journal id instead of guessing.                                                                                                                                                  |
|                                                                         | `postFee`                 | **REAL**                  | `POST /api/v1/journals` (fee expense/revenue lines)                                                                                                                                                                                                                                                                                          |
| `PaymentClient` (`WebPaymentClient` → payment-service `:8087`)          | `flagTransactionDisputed` | **STUBBED**               | No matching endpoint on `PaymentController` to mark a transaction disputed. Logs only.                                                                                                                                                                                                                                                       |
|                                                                         | `clearDisputedFlag`       | **STUBBED**               | Same reason — no endpoint exists. Logs only.                                                                                                                                                                                                                                                                                                 |
|                                                                         | `getTransactionDetail`    | **REAL** (partial)        | `GET /v1/payments/{id}`. AVS/CVV/3DS/auth-code fields are always null/false — `PaymentResponse` doesn't expose them (they live on the internal `Authorization` entity).                                                                                                                                                                      |
| `SettlementClient` (`WebSettlementClient` → settlement-service `:8091`) | `recoverFromSettlement`   | **REAL**                  | `POST /api/adjustments` (DEBIT adjustment, idempotency key)                                                                                                                                                                                                                                                                                  |
|                                                                         | `releaseToMerchant`       | **REAL**                  | `POST /api/adjustments` (CREDIT adjustment, idempotency key)                                                                                                                                                                                                                                                                                 |
|                                                                         | `adjustReserve`           | **STUBBED**               | `ReserveController` only supports list/get/release-due, no endpoint to set a reserve percentage directly. Logs only.                                                                                                                                                                                                                         |
| `NotificationClient` (`LoggingNotificationClient`, `integration/stub/`) | `notifyMerchant`          | **STUBBED (all methods)** | notification-service's `POST /api/messages` requires a resolved recipient address (email/phone) + template key/locale for server-side rendering; dispute-service only has a merchant id and free-text subject/body, with no lookup for a contact address. Intentionally left as a logging stub rather than fabricating a recipient/template. |

Note: `LedgerClient` account ids (`merchant:<id>:reserve`, `platform:chargeback_clearing`, etc.) are a deterministic
convention, not looked up — ledger-service exposes no account-discovery-by-merchant endpoint.

**Who calls dispute-service:** the API gateway (`gateway-service/src/main/resources/application.yml`) routes
`Path=/api/v1/disputes/**,/api/v1/reason-codes/**` to `dispute-service:8080` behind a Resilience4j circuit breaker (
`disputeCircuitBreaker`, fallback `forward:/fallback/dispute-service`) and a Redis rate limiter. No other service in the
repo references dispute-service directly (grepped for `dispute-service`/`8090`).

**Kafka:** none — no Kafka dependency in `build.gradle`, no `@KafkaListener`/`KafkaTemplate` usage anywhere in the
module. The only "eventing" is Spring's in-process `ApplicationEventPublisher`/`@EventListener` (
`DisputeEventPublisher` → `DisputeEventListener`), which today just logs `DisputeCreatedEvent`,
`ChargebackReceivedEvent`, `DisputeWonEvent`, `DisputeLostEvent` and is explicitly documented in code as a stand-in for
a real outbound bridge to Reporting/Analytics/Merchant Portal/Notification/Audit.

## Running locally

```
./gradlew :dispute-service:bootRun
```

Environment variables (all optional, defaults shown):

| Var                               | Default                               | Purpose                                                          |
|-----------------------------------|---------------------------------------|------------------------------------------------------------------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `dispute_db`   | PostgreSQL connection                                            |
| `DB_USERNAME` / `DB_PASSWORD`     | `dispute_service` / `dispute_service` | DB credentials                                                   |
| `DB_POOL_SIZE`                    | `20`                                  | Hikari max pool size                                             |
| `SERVER_PORT`                     | `8090`                                | HTTP port                                                        |
| `PAYMENT_SERVICE_URL`             | `http://localhost:8087`               | payment-service base URL                                         |
| `LEDGER_SERVICE_URL`              | `http://localhost:8092`               | ledger-service base URL                                          |
| `SETTLEMENT_SERVICE_URL`          | `http://localhost:8091`               | settlement-service base URL                                      |
| `NOTIFICATION_SERVICE_URL`        | `http://localhost:8094`               | notification-service base URL (bean wired, client still stubbed) |

Flyway runs automatically on startup (`flyway.enabled: true`, `baseline-on-migrate: true`); JPA is `ddl-auto: validate`
only, so schema changes must go through a new migration under `src/main/resources/db/migration`.

Deadline sweep cadence: `dispute.deadline.sweep-interval-ms` (default 3,600,000 ms / hourly),
`dispute.deadline.sweep-initial-delay-ms` (default 60,000 ms).

## Design notes

- **Errors degrade to logging + synthetic ids, not exceptions.** Every `WebClient` call in `integration/web/` wraps
  failures in `onErrorResume`/`try-catch` and returns a fallback (`jrn_<uuid>`, `stl_<uuid>`) rather than throwing. This
  keeps the dispute lifecycle moving even if ledger/settlement is briefly down, at the cost of the returned id being
  fictitious until reconciled — a deliberate availability-over-consistency tradeoff worth defending in review (the
  alternative, failing the whole request, would block chargeback intake on a downstream blip).
- **The state machine is the single source of truth for legality**, not scattered `if` checks in services —
  `DisputeStateMachine.assertCanTransition` is the only place transitions are validated, so adding a new transition is a
  one-line change in one file instead of an audit across services.
- **Stub boundaries are honest, not silent.** Every stubbed method has a code comment naming the exact missing
  downstream capability (no endpoint, no signature match, no recipient-resolution path) rather than a generic "TODO" —
  this made writing this README a matter of reading comments, not guessing intent.
- **How I'd finish the remaining stubbed integrations:**
    - `NotificationClient.notifyMerchant`: needs a merchant-id → contact-address resolution step, most plausibly a
      `GET /merchants/{id}/contacts` call to merchant-service (not currently wired here) to get an email/phone, plus a
      small set of dispute-specific templates (`dispute.opened`, `dispute.evidence_requested`,
      `dispute.deadline_reminder`, `dispute.won`, `dispute.lost`) registered on notification-service so
      `POST /api/messages` can be called with `recipient` + `templateKey` + template params instead of free-text
      subject/body.
    - `PaymentClient.flagTransactionDisputed` / `clearDisputedFlag`: needs payment-service to add a `disputed` boolean (
      or status) field to the transaction/authorization record and a small `PATCH /v1/payments/{id}/dispute-flag`
      endpoint; today there's simply nothing on that side to call.
    - `SettlementClient.adjustReserve`: needs settlement-service's `ReserveController` extended with a
      `PUT /api/reserves/{merchantId}` (or similar) to set percentage/rolling-days directly, since it currently only
      lists and releases due reserves.
    - `LedgerClient.finalizeLoss`: either add an `amountMinor`/`currency` parameter to the interface so a real balanced
      journal can be posted, or add a ledger-service endpoint that finalizes a hold by referencing the original
      journal (letting ledger-service look up the amount itself) — the current signature genuinely can't be satisfied
      without one of those two changes.
