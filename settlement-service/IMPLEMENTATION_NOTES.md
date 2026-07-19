# Settlement Service — Implementation Notes

Production-ready implementation of the Settlement Service described in
`SettlementReadme.md`. Built on Spring Boot 3.3 / Java 17 with a clean hexagonal
split between the settlement domain and pluggable external integrations.

## Architecture

```
web (controllers, DTOs, error handling, idempotency)
        │
domain services
  ├── calculation        net = gross − refunds − chargebacks − fees − interchange − reserve − settlementFee + adjustments
  ├── batch              aggregate items + adjustments → SettlementBatch + Payout
  ├── initiation         pre-validate → ledger → rail submit → state + events
  ├── retry              failure classification + exponential backoff
  ├── reversal           fund recovery with approval thresholds
  ├── reconciliation     match internal vs bank records
  ├── reserve            rolling reserve hold + scheduled release
  ├── adjustment         manual credits/debits with approval workflow
  └── report             merchant statement, daily summary, pending, reserves, exceptions
        │
integration (interfaces + in-process simulators)
  ├── RailGateway    → SimulatedRailGateway (+ RailRouter rail selection)
  ├── LedgerClient   → SimulatedLedgerClient
  ├── MerchantClient → SimulatedMerchantClient
  └── DomainEventPublisher → transactional outbox → MessageBus (LoggingMessageBus)
```

Every external dependency is an interface with a simulated implementation, so the
service runs end-to-end with no real banks or brokers. Swap in HTTP/Kafka
implementations without touching the domain.

## Key design decisions

- **Money as integer minor units.** All arithmetic goes through `common.Money`
  with `Math.*Exact` overflow checks — no floating point.
- **Explicit state machines.** `BatchStatus` / `PayoutStatus` encode legal
  transitions; illegal transitions throw `InvalidStateTransitionException`.
- **Transactional outbox.** Domain events are written in the same transaction as
  the state change (`OutboxEvent`) and relayed asynchronously by `OutboxRelay`
  for at-least-once delivery without dual-write inconsistency.
- **Idempotency** at two levels: settlement-item ingestion (unique
  `idempotency_key`) and mutating API calls (`Idempotency-Key` header via
  `IdempotencyService`).
- **Optimistic locking** (`@Version`) and JPA auditing (`created_at`/`updated_at`)
  on every entity.
- **Retry policy** driven by `FailureCategory` (TRANSIENT/RECOVERABLE/RAIL_SIDE
  retry with backoff `1h,4h,12h,24h,48h`; MERCHANT_SIDE/FATAL terminate).
- **Approval thresholds** for adjustments (supervisor > $1k, finance director
  > $10k) and reversals (finance director > $5k), all configurable.

## Running

Requires JDK 17+. Dev profile uses in-memory H2 (no external DB), simulated rails,
and enabled schedulers.

```bash
gradle bootRun            # starts on http://localhost:8085 (dev profile)
# or
gradle build && java -jar build/libs/settlement-service-1.0.0.jar
```

Production profile (`SPRING_PROFILES_ACTIVE=prod`) uses PostgreSQL with Flyway
migrations (`src/main/resources/db/migration/V1__initial_schema.sql`) and
`ddl-auto=validate`. Configure via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Example flow

```bash
BASE=http://localhost:8085/api

# 1. Ingest captured transactions (positive amounts, minor units)
curl -s -XPOST $BASE/settlement-items -H 'Content-Type: application/json' -d '{
  "merchantId":"m1","type":"CAPTURE","sourceType":"PAYMENT","sourceId":"p1",
  "amountMinor":1000000,"currency":"USD","idempotencyKey":"cap-p1"}'

curl -s -XPOST $BASE/settlement-items -H 'Content-Type: application/json' -d '{
  "merchantId":"m1","type":"REFUND","sourceType":"REFUND","sourceId":"r1",
  "amountMinor":20000,"currency":"USD","idempotencyKey":"ref-r1"}'

# 2. Run settlement for the merchant (builds batch, holds reserve, pays out)
curl -s -XPOST $BASE/settlement-runs -H 'Content-Type: application/json' -d '{
  "scheduleType":"DAILY","merchantId":"m1","currency":"USD"}'

# 3. Inspect results
curl -s $BASE/settlement-batches?merchantId=m1
curl -s $BASE/payouts?merchantId=m1
curl -s $BASE/reports/merchant-statement/m1
```

Simulated rail failures are triggered by markers in the payout account id:
`invalid`/`closed` → merchant-side (no retry), `sanction` → fatal, `timeout` →
transient (retry), `nsf` → recoverable.

## API surface

| Area | Endpoints |
|------|-----------|
| Items | `POST/GET /api/settlement-items` |
| Batches | `GET /api/settlement-batches`, `.../{id}`, `.../{id}/payouts`, `POST .../{id}/initiate|reconcile|reverse` |
| Payouts | `GET /api/payouts`, `.../{id}`, `POST .../{id}/confirm|returns` |
| Reserves | `GET /api/reserves`, `POST /api/reserves/release-due` |
| Adjustments | `POST /api/adjustments`, `POST .../{id}/approve|reject`, `GET ...` |
| Runs | `POST /api/settlement-runs` |
| Reports | `GET /api/reports/{merchant-statement/{id},daily-summary,pending-settlements,reserve-release-schedule/{id},exceptions}` |
| Ops | `GET /actuator/health`, `/actuator/prometheus` |

## Scheduling

`SettlementScheduler` runs the settlement cycle, retry sweep, and reserve-release
sweep on configurable crons (`settlement.scheduler.*`). Disable with
`settlement.scheduler.enabled=false`.

## Not included

Automated tests (per request). External clients are simulated; production builds
supply real `RailGateway`/`LedgerClient`/`MerchantClient`/`MessageBus` beans.
