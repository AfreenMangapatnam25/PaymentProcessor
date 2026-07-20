# Settlement Service

Computes what each merchant is owed after fees, reserves and adjustments, batches it into payouts, and posts every one of those money movements to the ledger as balanced double-entry journals.

## Role in the platform

- Runs settlement cycles that turn captured payments into merchant payouts: fee calculation, reserve holds, net payout amount, and settlement-batch grouping (`SettlementBatchController`, `SettlementRunController`, `SettlementItemController`).
- Holds back a configurable reserve per merchant (`ReserveController`) and releases it on a schedule (`reserve-release-cron`, default daily at 02:00) or on demand via `POST /api/reserves/release-due`.
- Confirms payouts and processes payout returns/NSF (`PayoutController`, `PayoutReturnController`).
- Runs a multi-tier approval workflow for manual balance adjustments (`AdjustmentController`) before they are posted.
- Posts a real, balanced double-entry journal to ledger-service for every settlement event — captures, reserve hold/release, payout, payout return, reversal, and adjustment — via `WebClientLedgerClient`.
- Looks up merchant status, currency, and default settlement/payout account from merchant-service via `WebClientMerchantClient`, since settlement decisions depend on whether a merchant is active and where funds should land.
- Retries failed downstream calls (ledger/merchant/rail) on a fixed backoff schedule and classifies failures as retryable vs. terminal.
- Exposes merchant statements, daily summaries, pending-settlement and exception reports (`ReportController`).

## Tech stack

- Java 21, Spring Boot 3.3.2 (Web, Data JPA, Validation, Actuator), Gradle
- WebClient (Spring WebFlux client, used reactively for outbound HTTP only — the service itself is a classic servlet app)
- Default port **8091** (`server.port: ${SERVER_PORT:8091}`)
- **Dev/prod profile split** (single `application.yml`, `spring.profiles.active` default `dev`):
  - `dev`: H2 in-memory (`jdbc:h2:mem:settlement`, PostgreSQL compatibility mode), `ddl-auto: update`, **Flyway disabled**, H2 console enabled. Self-contained — no external DB required.
  - `prod`: real PostgreSQL (`jdbc:postgresql://localhost:5432/settlement`), `ddl-auto: validate`, **Flyway enabled** (`baseline-on-migrate: true`), Hikari pool sized 20/idle 5.
- Transactional outbox pattern (`OutboxEvent` + `OutboxRelay`) for in-process domain events via a `MessageBus` abstraction — no Kafka dependency in this service today.
- No Dockerfile/docker-compose in this service (unlike ledger-service and others) — run via Gradle or the platform's shared compose setup.

## API surface

All controllers are mounted at `/api/*` (no `/v1` prefix, unlike ledger-service/reconciliation-service).

| Controller | Base path | Notable endpoints |
|---|---|---|
| `SettlementRunController` | `/api/settlement-runs` | `POST` — kick off a settlement run |
| `SettlementBatchController` | `/api/settlement-batches` | `GET`, `GET /{id}`, `GET /{id}/payouts`, `POST /{id}/initiate`, `POST /{id}/reconcile`, `POST /{id}/reverse` |
| `SettlementItemController` | `/api/settlement-items` | `POST`, `GET`, `GET /{id}` |
| `PayoutController` | `/api/payouts` | `GET`, `GET /{id}`, `POST /{id}/confirm`, `POST /{id}/returns` |
| `PayoutReturnController` | `/api/payout-returns` | `GET` |
| `ReserveController` | `/api/reserves` | `GET`, `GET /{id}`, `POST /release-due` — releases every reserve past its hold-until date |
| `AdjustmentController` | `/api/adjustments` | `POST`, `GET`, `GET /{id}`, `POST /{id}/approve`, `POST /{id}/reject` — multi-tier approval workflow |
| `ReportController` | `/api/reports` | `GET /merchant-statement/{merchantId}`, `GET /daily-summary`, `GET /pending-settlements`, `GET /reserve-release-schedule/{merchantId}`, `GET /exceptions` |

Reachable through the API gateway at these same paths; the gateway wraps them in a `settlementCircuitBreaker` with a `/fallback/settlement-service` fallback.

## Data model

- **SettlementBatch / SettlementItem** — a batch groups the settlement items computed for a settlement run; batches move through a state machine (`BatchStatus`) with initiate/reconcile/reverse transitions; illegal transitions raise `InvalidStateTransitionException`.
- **Payout / PayoutReturn** — a payout is created from a settled batch and confirmed against the merchant's payout account; returns represent bounced/NSF payouts and feed back into the merchant balance.
- **Reserve** — reserve holds per merchant, released automatically after `reserve-hold-days` (default 90) or on-demand via `/api/reserves/release-due`.
- **Adjustment** — manual balance corrections with a required approval level (`ApprovalLevel`) driven by thresholds in `application.yml`:
  - amounts above **100,000 minor units ($1,000)** require supervisor approval,
  - amounts above **1,000,000 minor units ($10,000)** require finance-director approval,
  - reversal-type adjustments above **500,000 minor units ($5,000)** require finance-director approval regardless of the general threshold.
  Adjustments start `PENDING_APPROVAL` and only post to the ledger after `POST /{id}/approve`.
- **Retry / backoff** — downstream call failures are retried up to **5 attempts**, with a fixed backoff schedule of **1h, 4h, 12h, 24h, 48h** (`60, 240, 720, 1440, 2880` minutes). Failures are classified: `TRANSIENT`/`RECOVERABLE`/`RAIL_SIDE` are retried; `MERCHANT_SIDE`/`FATAL` terminate without retry (e.g. an invalid/closed payout account is merchant-side and does not retry; a sanctions hit is fatal).
- **IdempotencyRecord / OutboxEvent** — every mutating operation is idempotent, and every domain event is written transactionally alongside the state change before being relayed.

## Inter-service integration

**Outbound (new today):**

- `WebClientLedgerClient` (`integration/ledger/WebClientLedgerClient.java`) posts real balanced journals to ledger-service's `POST /api/v1/journals` for every settlement event: `postSettlement`, `postReserveHold`/`postReserveRelease`, `postPayout`/`postPayoutReturn`, `postReversal`, `postAdjustment`. It addresses ledger accounts by a **deterministic naming convention** rather than looking them up: `merchant:<id>:settlement_liability`, `merchant:<id>:reserve`, and the platform control accounts `platform:cash`, `platform:fee_revenue`, `platform:payout_payable`, `platform:adjustment_expense`. These accounts are **not created by this client** — they must be pre-provisioned in ledger-service (via `POST /api/v1/accounts`, e.g. at onboarding or from seed data) or postings will fail. Idempotency key on each post is `eventType:externalRef`, so the ledger's own idempotency guard prevents duplicate postings on retry.
- `WebClientMerchantClient` (`integration/merchant/WebClientMerchantClient.java`) reads merchant status/currency from merchant-service's `GET /api/v1/merchants/{id}` and the default settlement/payout account from `GET /api/v1/merchants/{id}/settlement-accounts`. Fee and reserve rate configuration (platform fee bps, fixed fee, reserve percentage, hold days) **still falls back to platform defaults** — the class-level comment notes merchant-service has no fee-schedule endpoint yet for this client to call, so per-merchant fee/reserve overrides are not honored until one exists.
- `LEDGER_SERVICE_URL` (default `http://localhost:8092`, 5s timeout) and `MERCHANT_SERVICE_URL` (default `http://localhost:8083`, 3s timeout) are the configured base URLs.
- The legacy `SimulatedLedgerClient` / `SimulatedMerchantClient` still exist as `@Profile("dev")` beans, but both `WebClientLedgerClient` and `WebClientMerchantClient` are annotated `@Primary` with **no profile restriction**, so they are the beans Spring wires by default in every profile, dev included. Running the `dev` profile with a self-contained H2 database does not by itself make settlement-service self-contained end-to-end — ledger-service and merchant-service still need to be reachable for the real integrations to succeed, since the `@Primary` Web clients outrank the simulated ones even under `dev`. To get the fully offline demo behavior the simulated beans historically provided, they would need to be made primary (or the Web clients scoped to a non-`dev` profile).

**Inbound:** dispute-service calls into settlement-service (`WebSettlementClient`) to post adjustments (`POST /api/adjustments`) as part of dispute resolution.

## Running locally

```bash
# dev profile — H2, self-contained except for the WebClient calls noted above
SPRING_PROFILES_ACTIVE=dev gradle bootRun

# prod profile — real Postgres + Flyway, required for the live ledger/merchant
# integrations to be meaningfully exercised end-to-end
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://localhost:5432/settlement \
LEDGER_SERVICE_URL=http://localhost:8092 \
MERCHANT_SERVICE_URL=http://localhost:8083 \
gradle bootRun
```

Key environment variables: `SERVER_PORT` (default `8091`), `SPRING_PROFILES_ACTIVE` (`dev`/`prod`), `LEDGER_SERVICE_URL`, `MERCHANT_SERVICE_URL`, `DB_URL`/credentials for `prod`. Health at `GET /actuator/health`.

## Design notes

- **Deterministic account naming vs. dynamic lookup.** Addressing ledger accounts as `merchant:<id>:settlement_liability` etc. instead of storing/looking up an account ID per merchant is a deliberate tradeoff: it means settlement-service needs zero round trips or local caching to know which account to post to, and account IDs never need to be synchronized between the two services. The cost is that ledger-service has no concept of "the settlement account for merchant X" — provisioning must happen out-of-band (onboarding flow or seed data) and stay in lock-step with the naming convention, and renaming/re-parenting an account requires a migration on both sides. Making this fully dynamic would mean ledger-service exposing an account-lookup-by-owner endpoint (or settlement-service persisting a merchant→account-id mapping) that this client resolves before posting, at the cost of an extra network hop (or a stale-cache risk) on the settlement hot path.
- **Fee/reserve config fallback is a known gap, not a silent bug.** `WebClientMerchantClient` documents in its class comment that per-merchant fee schedules aren't real yet; every merchant is settled against the same platform-default bps/reserve rate until merchant-service grows a fee-schedule endpoint. Keeping this as an explicit, single fallback point rather than scattering defaults through the codebase makes it a one-line swap later.
- **Retry classification lives at the boundary, not in the business logic.** Failures from ledger/merchant/rail calls are classified into retryable (`TRANSIENT`/`RECOVERABLE`/`RAIL_SIDE`) vs. terminal (`MERCHANT_SIDE`/`FATAL`) categories at the integration layer, so the settlement state machine only ever sees "retry later" or "stop" rather than embedding HTTP/network semantics in domain code.
- **`@Primary` Web clients with no profile guard is worth flagging, not just documenting.** Because the new WebClient beans aren't scoped to `prod`, `dev` doesn't actually give you the previous fully-offline demo experience — it's a live integration test against whatever ledger-service/merchant-service you have reachable. That's a reasonable end state (dev should exercise real wiring) but it's a behavior change worth calling out explicitly since the simulated clients are still present and could be mistaken for the active path.
