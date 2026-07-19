# Limit Service

Production-ready implementation of the platform **Limit Service** — the financial
guardrail that enforces spending and transaction limits before a payment is
authorized. It validates per-transaction, daily, weekly and monthly amount/count
limits across customer, merchant, currency and global scopes, and manages a
**reserve → commit → release** lifecycle that prevents race conditions and double
spending.

> This document describes how to build, run and call the service. The functional
> specification lives in [`LimitReadme.md`](./LimitReadme.md).

---

## Tech stack

| Concern            | Choice                                            |
|--------------------|---------------------------------------------------|
| Language / runtime | Java 17, Spring Boot 3.3                           |
| Persistence        | PostgreSQL + Spring Data JPA (Hibernate)          |
| Schema management  | Flyway versioned migrations                        |
| Caching / atomics  | Redis (Spring Data Redis)                          |
| Messaging          | Apache Kafka (domain events)                       |
| Service discovery  | Netflix Eureka client                             |
| API docs           | springdoc-openapi (Swagger UI)                    |
| Observability      | Spring Boot Actuator + Micrometer/Prometheus      |

---

## Architecture

```
Payment Service ──HTTP──▶ LimitController(s)
                              │
              ┌───────────────┼─────────────────┐
              ▼               ▼                 ▼
     LimitEvaluationSvc  ReservationSvc    LimitConfigSvc
       (dry-run check)   (reserve/commit/  (admin CRUD)
              │            release)             │
              ▼               ▼                 ▼
        LimitResolver ── UsageCounter (SELECT … FOR UPDATE) ── PostgreSQL
                              │
                              ├── LimitEventPublisher ──▶ Kafka (limit.reserved/released/exceeded)
                              └── AuditService ─────────▶ limit_audit_log
```

### Concurrency model

Correctness under concurrency is guaranteed by a **pessimistic write lock**
(`SELECT … FOR UPDATE`) taken on each affected `usage_counter` row before it is read
and mutated. Because the lock is held by the database it serialises competing
transactions across **every service instance**, closing the race that would
otherwise let two payments each consume the last of a limit. Counters are locked in
a deterministic order (by limit-config id) to avoid deadlocks, and an optimistic
`@Version` column provides a second line of defence.

### Reserve → commit → release lifecycle

1. **Reserve** — evaluate every applicable limit under lock; if all *hard* limits
   pass, hold the amount/count against each counter, persist a `LimitReservation`
   (+ per-limit `reservation_line`s) with a TTL, and emit `LimitReserved`. If a hard
   limit is breached the transaction rolls back and `LimitExceeded` is emitted.
2. **Commit** — on capture, move the captured portion from *reserved* to *committed*
   and release any uncaptured remainder (partial capture supported).
3. **Release** — on failure/cancellation/manual override, return the held capacity.
4. **Expire** — a scheduled sweeper auto-releases reservations past their TTL.

---

## Running locally

Prerequisites: JDK 17, PostgreSQL, Redis, and (optionally) Kafka + Eureka.

```bash
# 1. Create the database
createdb limit_local

# 2. Build (Flyway runs the migrations on startup)
gradle clean build            # or ./gradlew clean build if a wrapper is present

# 3. Run with the local profile
SPRING_PROFILES_ACTIVE=local gradle bootRun
```

Profiles: `local` (localhost, verbose SQL), `dev`, `prod` (SSL, tuned pools,
Prometheus). Secrets in `dev`/`prod` are supplied via environment variables
(`DB_USERNAME`, `DB_PASSWORD`, `REDIS_PASSWORD`, `SSL_KEYSTORE_*`).

Flyway applies `V1__init_limit_schema.sql` (tables + indexes) and
`V2__seed_default_limits.sql` (sensible default limits). Hibernate is set to
`ddl-auto: validate`, so the entities are checked against — never allowed to
diverge from — the migrated schema.

> Note: this project targets Java 17 and downloads its dependencies from Maven
> Central at build time. If your repo uses a Gradle wrapper, run `gradle wrapper`
> once to generate `gradlew`.

Once running:
- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`
- Health: `http://localhost:8085/actuator/health`

---

## API

Base path `/api/v1`. Port `8085`.

### Evaluate (dry run, no capacity held)

```http
POST /api/v1/limits/check
{
  "customerId": "cust-123",
  "merchantId": "merch-9",
  "currency": "USD",
  "amount": 2000.00
}
```
Returns `decision` = `APPROVED` | `FLAGGED` (soft breach) | `DECLINED` (hard breach)
plus the hard/soft violations.

### Reserve capacity

```http
POST /api/v1/reservations
{
  "transactionId": "txn-abc",
  "customerId": "cust-123",
  "currency": "USD",
  "amount": 2000.00,
  "idempotencyKey": "txn-abc-attempt-1"
}
```
`201` with the reservation, or `422 LIMIT_EXCEEDED` with the offending limits.
The `idempotencyKey` makes retries safe.

### Commit / release

```http
POST /api/v1/reservations/{reservationId}/commit   { "capturedAmount": 1500.00 }
POST /api/v1/reservations/{reservationId}/release   { "reason": "auth-failed" }
GET  /api/v1/reservations/{reservationId}
GET  /api/v1/reservations?transactionId=txn-abc
```

### Usage / headroom

```http
GET /api/v1/limits/usage?customerId=cust-123&currency=USD
```

### Admin — limit configuration

```http
POST   /api/v1/limit-configs        # create
PUT    /api/v1/limit-configs/{id}   # replace
DELETE /api/v1/limit-configs/{id}   # disable (soft delete)
GET    /api/v1/limit-configs        # list
```

A configuration constrains one **dimension** (`AMOUNT` | `COUNT`) over one
**window** (`PER_TRANSACTION` | `DAILY` | `WEEKLY` | `MONTHLY` | `LIFETIME`) for one
**scope** (`GLOBAL` | `CUSTOMER` | `MERCHANT` | `CURRENCY` | `COUNTRY`), with a
`HARD` or `SOFT` enforcement mode. A `scopeId` of `null` is a scope-wide default; a
specific `scopeId` overrides that default for the entity.

---

## Domain events (Kafka)

| Topic            | Emitted when                                             |
|------------------|----------------------------------------------------------|
| `limit.reserved` | Capacity successfully reserved for a transaction.        |
| `limit.released` | Reserved capacity freed (release, expiry, partial capture). |
| `limit.exceeded` | A hard limit was breached and the transaction declined.  |

Publication is best-effort: a Kafka failure is logged but never fails a decision
that has already been persisted.

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

---

## Scope

This build implements the README's core engine: limit configuration, usage
counters, per-transaction/daily/weekly/monthly amount & count limits, the full
reserve/commit/release/expire lifecycle, audit trail, Kafka events and REST APIs.
Velocity, geographic/country and credit-line limits described in the specification
are intentionally left as follow-on work; the resolver and evaluation pipeline are
structured so they slot in as additional scopes/dimensions without reworking the
core.
