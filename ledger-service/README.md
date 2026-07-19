# Ledger Service

Production-ready implementation of the immutable, double-entry financial book of
record described in [`LedgerReadme.md`](./LedgerReadme.md). It records every
monetary movement as a balanced set of debits and credits, maintains real-time
account balances, and publishes domain events for downstream services.

> The domain reference (accounting principles, chart of accounts, event
> catalogue) lives in **`LedgerReadme.md`**. This file covers how to build, run,
> and use the service.

---

## Highlights

- **Double-entry posting engine** — atomic, balanced (debits = credits per
  currency), and **idempotent** via `idempotencyKey`.
- **Immutability** — journals and entries are append-only. There are no update
  or delete endpoints; corrections are made only by posting a **reversal**.
- **Real-time balances** — maintained synchronously under a pessimistic lock and
  optimistic `@Version`, exposing posted / held / available amounts.
- **Holds** — reserve against available balance without touching posted balance.
- **Trial balance, statements, and immutable snapshots** for reporting and audit.
- **Accounting periods** — postings are rejected into `CLOSED` / `LOCKED` periods.
- **Transactional outbox** — events (`LedgerPosted`, `LedgerReversed`,
  `BalanceUpdated`) are written in the same transaction as the ledger change and
  relayed by a background publisher (at-least-once, broker-agnostic).
- **Money is integer-exact** — all amounts are **minor units** (e.g. cents);
  there is no floating point in the accounting path.

## Tech stack

Java 17 · Spring Boot 3.3 (Web, Data JPA, Validation, Actuator) · Flyway ·
PostgreSQL (prod) / H2 (local) · springdoc OpenAPI · Gradle.

---

## Running

### Option A — local, zero dependencies (H2 in-memory)

The default `local` profile boots a self-contained in-memory database, seeded
with the full chart of accounts.

```bash
gradle bootRun
# or, after building:
gradle bootJar && java -jar build/libs/ledger-service-1.0.0.jar
```

Then open:

- Swagger UI … http://localhost:8080/swagger-ui.html
- OpenAPI ……… http://localhost:8080/v3/api-docs
- Health ………… http://localhost:8080/actuator/health
- H2 console … http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:ledger`)

### Option B — production-like (PostgreSQL + Flyway) via Docker

```bash
docker compose up --build
```

This starts PostgreSQL and the service on the `prod` profile, applying the
Flyway migrations in `src/main/resources/db/migration`.

### Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | `local` | `local` (H2) or `prod` (Postgres) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ledger` | prod datasource URL |
| `DB_USERNAME` / `DB_PASSWORD` | `ledger` / `ledger` | prod credentials |
| `SERVER_PORT` | `8080` | HTTP port |
| `ledger.reversal.approval-threshold-minor` | `100000` | reversals above this (per line) require an approver |
| `ledger.outbox.poll-interval-ms` | `2000` | outbox relay interval |

> **Build note:** this project uses Gradle but does not ship the wrapper binary.
> Use a locally installed Gradle 8.x (`gradle …`), or build via Docker (the
> Dockerfile uses a Gradle image). To generate the wrapper locally, run
> `gradle wrapper --gradle-version 8.10.2`.

---

## Core API

Base path `/api/v1`. All amounts are in minor units.

### Post a journal (double-entry)

```http
POST /api/v1/journals
Content-Type: application/json

{
  "eventType": "PAYMENT_CAPTURED",
  "externalRef": "PAY-2026-07-17-8842",
  "idempotencyKey": "PAY-8842-capture",
  "description": "Capture of PAY-8842",
  "createdBy": "payment-service",
  "lines": [
    { "accountId": "1101", "direction": "DEBIT",  "amountMinor": 9700, "description": "Settlement cash" },
    { "accountId": "1103", "direction": "DEBIT",  "amountMinor": 300,  "description": "Fee receivable" },
    { "accountId": "2102", "direction": "CREDIT", "amountMinor": 9700, "description": "Owed to merchant" },
    { "accountId": "4101", "direction": "CREDIT", "amountMinor": 300,  "description": "Fee revenue" }
  ]
}
```

The engine validates that debits equal credits per currency, that accounts exist
and are active, resolves the accounting period, persists the journal and entries,
updates balances, and enqueues events — all in one transaction. Re-posting with
the same `idempotencyKey` returns the original journal.

### Reverse a journal

```http
POST /api/v1/journals/{id}/reverse
{ "reason": "TRANSACTION_REVERSED", "createdBy": "refund-service", "approvedBy": "ops-jane" }
```

Creates a compensating journal with debits and credits swapped; the original is
marked `REVERSED` and linked to its reversal but otherwise untouched.

### Other endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /api/v1/accounts` | Create a ledger account |
| `GET  /api/v1/accounts/{id}` | Get an account |
| `GET  /api/v1/accounts/{id}/balance` | Real-time balance (posted / held / available) |
| `GET  /api/v1/accounts/{id}/statement?from=&to=` | Statement with running balance |
| `GET  /api/v1/accounts/{id}/holds` | Holds for an account |
| `GET  /api/v1/accounts/{id}/snapshots` | Snapshots for an account |
| `GET  /api/v1/journals/{id}` | Journal with its entries |
| `GET  /api/v1/journals?externalRef=` | Find journals by business reference |
| `GET  /api/v1/entries?accountId=` | Paged entries for an account |
| `POST /api/v1/holds` · `POST /api/v1/holds/{id}/release` | Place / release a hold |
| `GET  /api/v1/trial-balance?asOf=` | Trial balance (debits must equal credits) |
| `POST /api/v1/snapshots` | Generate balance snapshots as of a date |
| `POST /api/v1/periods` · `POST /api/v1/periods/{id}/state` | Manage accounting periods |
| `GET  /api/v1/account-types` · `GET /api/v1/currencies` | Reference data |

Errors use a consistent envelope: `{ timestamp, status, error, code, message, path, details }`.

---

## Balance semantics

`postedMinor` is expressed in the account's **normal-balance** direction: a
positive value is a balance on the account's normal side. For a debit-normal
account a debit increases it; for a credit-normal account a credit increases it.
`availableMinor = postedMinor − heldMinor`.

## Domain events

Written to the `outbox_events` table inside the posting transaction and relayed
by `OutboxPublisher` as in-process `OutboxMessage` events. Attach a broker
adapter (Kafka, SNS, …) by listening for `OutboxMessage` to forward them
onward.

| Event | Trigger |
|-------|---------|
| `LedgerPosted` | a journal is posted |
| `LedgerReversed` | a journal is reversed |
| `BalanceUpdated` | an account balance changes (posting, reversal, or hold) |

---

## Project layout

```
src/main/java/com/paymentprocessor/ledgerservice/
├── config/        # Clock, scheduling, OpenAPI
├── controller/    # REST controllers (/api/v1)
├── domain/enums/  # accounting enums
├── entity/        # JPA entities (immutable ledger tables)
├── event/         # domain event records
├── repository/    # Spring Data repositories
├── service/       # posting engine, reversals, balances, snapshots, outbox …
├── support/       # id generation
└── web/dto,error/ # request/response DTOs, exceptions, global handler
src/main/resources/
├── application*.yml          # base / local / prod profiles
├── db/migration/             # Flyway (prod)
└── db/local/data.sql         # H2 seed (local)
```

No automated tests are included by request; the domain invariants
(balanced-debits-equal-credits, immutability, idempotency) are enforced in the
posting engine.
