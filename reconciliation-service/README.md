# Reconciliation Service

Production-ready Spring Boot microservice that reconciles the payment platform's internal financial
records against external statements from banks, acquirers, card networks and gateways. It implements
the **match → detect → resolve** model described in [`ReconciliationReadme.md`](./ReconciliationReadme.md):
ingest records, match them with configurable rules, classify and score the discrepancies that remain,
and drive each exception through to resolution with a full audit trail and domain events.

## Tech stack

- Java 17, Spring Boot 3.3.2 (Web, Data JPA, Validation, Actuator)
- PostgreSQL with Flyway-versioned schema migrations
- Apache Kafka (spring-kafka) for domain events
- Gradle build, Lombok for boilerplate

## Architecture

The reconciliation pipeline is orchestrated by `ReconRunService.execute()`:

1. **Ingest** – internal records (from Payment/Ledger/Settlement) and external records (statements)
   are attached to a `ReconRun`. External statements can be uploaded as CSV and are normalized into
   records automatically.
2. **Duplicate detection** – records repeating on the same side (same reference + amount) are flagged
   as `DUPLICATE`; the first occurrence is kept for matching.
3. **Matching** – `MatchingEngine` applies ordered `MatchRule`s (highest confidence first) to pair
   internal and external records:
   `AMOUNT_AND_REFERENCE` → `AMOUNT_AND_DATE` → `AMOUNT_MERCHANT_DATE` →
   `REFERENCE_AMOUNT_TOLERANCE` (FX rounding) → `AMOUNT_AND_LAST4`. Amount and date tolerances are
   configurable.
4. **Mismatch classification** – unmatched records become exceptions:
   `AMOUNT_MISMATCH` (same reference, different amount), `MISSING_INTERNAL`, `MISSING_EXTERNAL`.
5. **Severity scoring** – each exception is scored
   `(amount × materialityWeight) + (age × agingWeight) + categoryBaseScore`, clamped to 0–100, mapped
   to a `SeverityLevel`, routed to a review queue and given an SLA.
6. **Events** – a `ReconciliationCompleted` event is published per run and a `MismatchDetected` event
   per exception (Kafka, JSON).

Exceptions then flow through the manual workflow (assign → resolve / escalate / defer) and can be
resolved by posting an `Adjustment`. Adjustments above the configured threshold require dual approval
with segregation of duties (the approver must differ from the creator) before posting.

## Configuration

All settings are environment-overridable (see `src/main/resources/application.yml`).

| Variable                      | Default                                           | Purpose                                                    |
|-------------------------------|---------------------------------------------------|------------------------------------------------------------|
| `DB_URL`                      | `jdbc:postgresql://localhost:5432/reconciliation` | Datasource URL                                             |
| `DB_USERNAME` / `DB_PASSWORD` | `recon` / `recon`                                 | Datasource credentials                                     |
| `KAFKA_BOOTSTRAP_SERVERS`     | `localhost:9092`                                  | Kafka brokers                                              |
| `KAFKA_ENABLED`               | `true`                                            | Set `false` to run with no broker (events are logged only) |
| `SERVER_PORT`                 | `8093`                                            | HTTP port                                                  |

Engine tuning lives under the `recon.*` keys: `recon.matching.amount-tolerance`,
`recon.matching.amount-tolerance-percent`, `recon.matching.date-tolerance-days`,
`recon.severity.materiality-weight`, `recon.severity.aging-weight`,
`recon.severity.senior-review-amount`, `recon.severity.dual-approval-amount`.

## Running locally

Prerequisites: JDK 17, a PostgreSQL database named `reconciliation`, and (optionally) Kafka.

```bash
# generate the Gradle wrapper once if it is not present
gradle wrapper --gradle-version 8.8

# run with Kafka disabled (no broker needed)
KAFKA_ENABLED=false ./gradlew bootRun

# or run the full stack
./gradlew bootRun
```

Flyway applies `db/migration/V1__initial_schema.sql` on startup, and Hibernate validates the schema
(`ddl-auto: validate`). Health is at `GET /actuator/health`. Unlike settlement-service, this service
has a single `application.yml` with no dev/prod profile split — it always talks to Postgres, so a
running database is required even for local development.

## API

Base path `/api/v1`. All list endpoints are paged (`?page=&size=&sort=`).

| Method & path                                                              | Description                                                     |
|----------------------------------------------------------------------------|-----------------------------------------------------------------|
| `POST /recon-runs`                                                         | Create a reconciliation run                                     |
| `GET /recon-runs/{id}` · `GET /recon-runs/uuid/{uuid}` · `GET /recon-runs` | Fetch / list runs                                               |
| `POST /recon-runs/{id}/records`                                            | Attach internal/external records (batch)                        |
| `GET /recon-runs/{id}/records`                                             | List a run's records                                            |
| `POST /recon-runs/{id}/execute`                                            | Run the match–detect pipeline                                   |
| `GET /recon-runs/{id}/matches` · `GET /recon-runs/{id}/exceptions`         | Results                                                         |
| `POST /statements/ingest?runId={id}`                                       | Ingest a CSV statement as external records                      |
| `GET /exceptions`                                                          | List/filter exceptions (`status`, `severity`, `queue`, `runId`) |
| `POST /exceptions/{id}/assign` · `/resolve` · `/escalate` · `/defer`       | Manual workflow                                                 |
| `POST /exceptions/{id}/adjustments` · `GET /exceptions/{id}/adjustments`   | Adjustments                                                     |
| `POST /adjustments/{id}/approve` · `/post` · `/reject`                     | Adjustment lifecycle                                            |

### Example

```bash
# 1) create a run
curl -sX POST localhost:8093/api/v1/recon-runs -H 'Content-Type: application/json' -d '{
  "reconType":"GATEWAY","channel":"STRIPE","businessDate":"2026-07-18","currency":"USD","triggeredBy":"ops"
}'

# 2) add internal records
curl -sX POST localhost:8093/api/v1/recon-runs/1/records -H 'Content-Type: application/json' -d '{
  "records":[
    {"source":"INTERNAL","externalReference":"TXN-1001","amount":120.50,"currency":"USD","transactionDate":"2026-07-18"},
    {"source":"INTERNAL","externalReference":"TXN-1002","amount":80.00,"currency":"USD","transactionDate":"2026-07-18"}
  ]
}'

# 3) ingest an external statement (see sample-statement.csv)
curl -sX POST 'localhost:8093/api/v1/statements/ingest?runId=1' -H 'Content-Type: application/json' -d '{
  "statementReference":"STMT-2026-07-18","bankName":"STRIPE","accountType":"SETTLEMENT",
  "accountNumber":"acct_123","format":"CSV","currency":"USD","statementDate":"2026-07-18",
  "csvContent":"reference,amount,currency,transactionDate\nTXN-1001,120.50,USD,2026-07-18\nTXN-9999,42.00,USD,2026-07-18"
}'

# 4) execute and read exceptions
curl -sX POST localhost:8093/api/v1/recon-runs/1/execute
curl -s 'localhost:8093/api/v1/recon-runs/1/exceptions'
```

In this example `TXN-1001` matches, `TXN-1002` becomes `MISSING_EXTERNAL`, and `TXN-9999` becomes
`MISSING_INTERNAL`.

## Domain events

Published to Kafka (topics configurable under `recon.events.topic.*`):

- `reconciliation.completed` — one `ReconciliationCompletedEvent` per finished run.
- `reconciliation.mismatch-detected` — one `MismatchDetectedEvent` per exception.

## Notes

- No test suite is included in this build by request; the code is structured for straightforward
  unit/integration testing (rules, engine and scoring are pure and independently testable).
- Ledger posting for adjustments is represented by a generated `ledgerReference`; wire this to the
  Ledger Service in production. Unlike settlement-service, reconciliation-service does not yet have a
  real WebClient integration into ledger-service — this remains a stub.
- Verified against source as of this revision: no other platform service calls into
  reconciliation-service directly (it is reachable only through the API Gateway's
  `/api/v1/recon-runs/**`, `/api/v1/records/**`, `/api/v1/matches/**`, `/api/v1/exceptions/**`,
  `/api/v1/statements/**` and `/api/v1/adjustments/**` routes); nothing changed here in the same pass
  that overhauled settlement-service.
