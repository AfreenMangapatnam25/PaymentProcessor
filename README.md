# Payment Processor Platform

A 17-service Spring Boot payment processing platform covering the full lifecycle of a card
transaction: identity, tokenization, risk/limits, authorization, clearing, settlement,
double-entry ledger, disputes, reconciliation, notifications, and audit.

This document is the **authoritative, verified-from-source** description of the platform —
built by reading controllers, entities, config, and integration code directly, not by
restating design intent. Each service also has its own `README.md` with full detail; this
file covers the platform as a whole: the service catalog, the request flows that cross
service boundaries, and how to talk about this project in a technical interview.

> If you're looking for the original green-field design proposal (service-boundary
> rationale, datastore trade-off discussion), see [`docs/DESIGN_PROPOSAL.md`](docs/DESIGN_PROPOSAL.md).
> That document predates this implementation and describes an idealized target, not the
> current codebase — this README describes what's actually built and wired today.

---

## 1. Service catalog

| #  | Service                  | Port | Datastore                  | Flyway   | Owns                                                                    |
|----|--------------------------|------|----------------------------|----------|-------------------------------------------------------------------------|
| 1  | `gateway-service`        | 8443 | Redis (rate limiting only) | —        | Edge routing, JWT/API-key validation, rate limiting, circuit breaking   |
| 2  | `authentication-service` | 8081 | Postgres                   | ✅        | Identities, credentials, MFA, API keys, JWT issuance (JWKS)             |
| 3  | `user-service`           | 8082 | Postgres                   | —        | Platform users, merchant-scoped customers, PII/GDPR                     |
| 4  | `merchant-service`       | 8083 | Postgres                   | —        | Merchant onboarding, KYB, API keys, fees, webhooks, settlement accounts |
| 5  | `tokenization-service`   | 8084 | Postgres                   | —        | PAN/bank-detail vaulting, network tokens, DEK registry (PCI scope)      |
| 6  | `limit-service`          | 8085 | Postgres + Redis           | ✅        | Transaction/velocity limits, two-phase reserve/commit/release           |
| 7  | `authorization-service`  | 8086 | Postgres                   | ✅        | Card authorization holds (real Stripe integration) + RBAC/policy admin  |
| 8  | `payment-service`        | 8087 | Postgres                   | —        | Payment orchestration, intent state machine, idempotency                |
| 9  | `fraud-service`          | 8088 | Postgres                   | ✅        | Rule engine, velocity checks, risk scoring, case management             |
| 10 | `clearing-service`       | 8089 | Postgres                   | ✅        | Network file formatting (ISO 8583/20022, NACHA, CSV), batch clearing    |
| 11 | `dispute-service`        | 8090 | Postgres                   | ✅        | Chargeback lifecycle, evidence, representment, arbitration              |
| 12 | `settlement-service`     | 8091 | Postgres (H2 in `dev`)     | ✅ (prod) | Payout batches, reserves, adjustments, merchant statements              |
| 13 | `ledger-service`         | 8092 | Postgres                   | ✅        | Double-entry accounting, trial balance, accounting periods              |
| 14 | `reconciliation-service` | 8093 | Postgres                   | —        | Statement-vs-ledger matching, exception workflow                        |
| 15 | `notification-service`   | 8094 | Postgres                   | ✅        | Email/SMS/webhook delivery, templates, suppression list                 |
| 16 | `audit-service`          | 8095 | Postgres                   | ✅        | Tamper-evident, hash-chained, Merkle-sealed audit trail                 |
| 17 | `reporting-service`      | 8096 | Postgres + ClickHouse      | ✅        | Report scheduling/export; ClickHouse as read-only OLAP source           |

All services are Spring Boot 3.3.2 on Java 21. Every service builds and runs independently;
`settings.gradle` at the repo root wires all 17 into one Gradle build (`./gradlew build`
compiles the whole platform in one pass).

---

## 2. How the services actually talk to each other

This platform was originally scaffolded with each service's downstream calls pointing at
simulators, wrong ports, or logging stubs — it looked wired together but wasn't. As of this
revision, the **core payment path is wired for real**, using:

- **WebClient** (Spring reactive HTTP client) for synchronous, request-path calls between
  services — used where a caller needs an answer before it can proceed (a limit check, a
  fraud decision, a ledger posting).
- **Kafka + the transactional outbox pattern** for asynchronous, fire-and-forget fan-out —
  used where the caller doesn't need to wait (an audit trail entry, a merchant webhook
  notification). Every producer writes to its own `outbox_events`/`OutboxEvent` table in the
  same DB transaction as the state change, then a scheduled relay publishes to Kafka —
  this avoids the dual-write problem (DB commit succeeds, Kafka publish fails, and the two
  go out of sync).

### What's real vs. what's still a boundary for future work

| Integration                                                                   | Status                                                                                                                                    |
|-------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `payment-service` → `limit-service` (reserve/commit/release)                  | ✅ real WebClient                                                                                                                          |
| `payment-service` → `fraud-service` (evaluate)                                | ✅ real WebClient (fails open if unreachable — see §4)                                                                                     |
| `payment-service` → `tokenization-service` (masked instrument lookup)         | ✅ real WebClient                                                                                                                          |
| `payment-service` → card/UPI networks                                         | Simulated PSPs on separate ports — these represent **external** networks, not platform services, and are out of scope for internal wiring |
| `settlement-service` → `ledger-service` (post balanced journals)              | ✅ real WebClient                                                                                                                          |
| `settlement-service` → `merchant-service` (settlement account lookup)         | ✅ real WebClient                                                                                                                          |
| `dispute-service` → `payment-service`, `ledger-service`, `settlement-service` | ✅ real WebClient for most methods; a few stayed logging stubs where no matching downstream endpoint exists yet (documented in code)       |
| `dispute-service` → `notification-service`                                    | Still a logging stub — no resolvable recipient address available from a dispute record                                                    |
| `gateway-service` → all 16 downstream services                                | ✅ routed, with per-route rate limiting, circuit breaking, and retry                                                                       |
| `merchant-service` → Kafka (`merchant.events`) → `notification-service`       | ✅ real, only cross-service Kafka *consumer* wiring outside audit-service                                                                  |
| every service → Kafka → `audit-service`                                       | ✅ audit-service is the platform's only general-purpose event consumer                                                                     |

Being able to point at this table and explain *why* each boundary is real, stubbed, or
intentionally external is one of the most interview-relevant things about this codebase —
see §5.

---

## 3. End-to-end flows

### 3.1 Card payment: create → authorize → capture → settle

```
Client
  │  POST /v1/payments  (via gateway-service, JWT validated against authentication-service JWKS)
  ▼
payment-service                     PaymentIntent created, status=CREATED
  │
  │  POST /v1/payments/{id}/authorize
  ▼
payment-service (authorizeCard)
  ├──▶ limit-service         POST /api/v1/reservations         reserve funds against daily/monthly/velocity limits
  │                          → LimitDecision: APPROVED | DECLINED | FLAGGED
  │                          (DECLINED → payment fails here, reservation never opened)
  │
  ├──▶ fraud-service         POST /api/fraud/evaluate           risk decision
  │                          → APPROVE | CHALLENGE | REVIEW | DECLINE | ESCALATE
  │                          (unreachable → fails OPEN, i.e. auto-approved — deliberate
  │                           availability-over-strictness trade-off)
  │                          (DECLINE → limit reservation released, payment fails)
  │
  ├──▶ tokenization-service  masked instrument lookup (never raw PAN)
  │
  └──▶ card network (external simulator, standing in for Visa/Mastercard/etc.)
                             AUTHORIZED | PENDING (3DS) | DECLINED
       status=AUTHORIZED
  │
  │  POST /v1/payments/{id}/capture
  ▼
payment-service (doCapture)
  ├──▶ card network          capture
  ├──▶ limit-service         POST /api/v1/reservations/{id}/commit   finalize the hold
       status=CAPTURED
  │
  ▼  (batch cycle, clearing-service.ClearingScheduler, every 10 min)
clearing-service              formats transaction per network (ISO 8583/20022/NACHA/CSV),
                               submits to acquirer (mock transport by default)
  │
  ▼  (settlement cycle, settlement-service.scheduler, hourly by default)
settlement-service
  ├──▶ merchant-service       GET settlement account + merchant status
  └──▶ ledger-service         POST /api/v1/journals   balanced double-entry journal:
                               DEBIT merchant:<id>:settlement_liability
                               CREDIT platform:fee_revenue, merchant:<id>:reserve,
                                      platform:payout_payable
       Payout scheduled, net of reserve holds and fees
  │
  ▼
notification-service          (via merchant.events Kafka topic, if the event is
                               merchant-scoped) webhook/email fan-out to merchant
```

Every step in this chain also writes to its own outbox table and, on a schedule, publishes
an event to Kafka; `audit-service` consumes those events into its hash-chained trail
regardless of which path above the transaction took.

### 3.2 Failure paths (equally important to be able to explain)

- **Limit declined**: `payment-service` never opens an authorization; attempt recorded with
  `LIMIT_EXCEEDED` or `LIMIT_SERVICE_UNAVAILABLE` (the two are distinguished deliberately —
  an unreachable limit-service is treated as a decline-and-retry condition, not silently
  approved, unlike the fraud check).
- **Fraud blocked**: the limit reservation already taken is explicitly released
  (`limitClient.release(reservationId, "FRAUD_BLOCK")`) before the payment fails — this is
  the kind of compensating-action detail worth walking through in an interview.
- **Network decline**: same release-then-fail pattern (`"AUTH_DECLINED"`).
- **Void**: releases the limit reservation (`"VOIDED"`).
- **Dispute opened** (`dispute-service`): independent chargeback state machine
  (`OPEN → PENDING_EVIDENCE → EVIDENCE_REVIEW → REPRESENTED → PRE_ARBITRATION → ARBITRATION
  → WON/LOST → CLOSED`), pulls transaction detail from `payment-service`, posts recovery/fee
  journals to `ledger-service`, and recovers funds via `settlement-service` adjustments.
- **Reconciliation exception** (`reconciliation-service`): bank statement doesn't match
  ledger/clearing records → `ExceptionRecord` created → assign → resolve/escalate/defer,
  can spawn an `Adjustment` requiring approve/post/reject.

### 3.3 Identity & audit (cross-cutting on every request)

```
Client → gateway-service
           │  validates JWT against authentication-service's JWKS
           │  strips spoofed X-User-Id / X-User-Roles / X-Merchant-Id headers
           │  applies Redis-backed rate limit + circuit breaker for the target route
           ▼
      downstream service
           │  every state change → outbox table (same DB transaction)
           ▼
      scheduled relay → Kafka topic
           ▼
      audit-service (AuditEventConsumer, manual offset ack)
           │  SHA-256 hash chain, canonical JSON, unique constraint on `seq`
           ▼
      daily Merkle-sealed batch (POST /api/v1/audit/batches/{date}/seal)
```

---

## 4. Core functionality, service by service

Full detail lives in each service's own `README.md` — linked below. Short version:

- **[gateway-service](gateway-service/README.md)** — single entry point; routes to all 16
  downstream services with per-route rate limiting, circuit breaking, and retry; strips
  spoofed identity headers before they reach a service.
- **[authentication-service](authentication-service/README.md)** — RSA-signed JWTs (own
  JWKS endpoint), TOTP MFA, device trust, API keys, account lockout, password policy.
- **[user-service](user-service/README.md)** — platform users and merchant-scoped
  customers; hexagonal/DDD layout; field-level PII encryption; GDPR erasure endpoint.
- **[merchant-service](merchant-service/README.md)** — merchant onboarding/KYB, API key
  rotation with IP allowlisting, fee configuration, webhook endpoints, settlement accounts.
- **[tokenization-service](tokenization-service/README.md)** — PCI-scoped card/bank
  instrument vaulting, network tokens, envelope-encrypted DEK registry.
- **[limit-service](limit-service/README.md)** — daily/monthly/per-transaction/velocity
  limits via a two-phase reserve → commit/release pattern (like an inventory hold), not a
  single stateless check.
- **[authorization-service](authorization-service/README.md)** — real Stripe-backed card
  authorization holds/captures/reversals, *plus* an unrelated full RBAC/policy admin API in
  the same deployable.
- **[payment-service](payment-service/README.md)** — the orchestrator; DB-backed state
  machine (transitions validated against a table, not a hardcoded switch), idempotency keys,
  transactional outbox, auto-expiry of stale authorizations.
- **[fraud-service](fraud-service/README.md)** — rule engine + velocity checks + ML scoring
  hook, five-way decision (APPROVE/CHALLENGE/REVIEW/DECLINE/ESCALATE).
- **[clearing-service](clearing-service/README.md)** — real ISO 8583/ISO 20022/NACHA/CSV
  formatters behind a registry, pluggable transport (mock by default).
- **[dispute-service](dispute-service/README.md)** — full chargeback lifecycle including
  representment and arbitration; network reason-code catalog.
- **[settlement-service](settlement-service/README.md)** — payout batching, rolling
  reserves, multi-tier adjustment approval, exponential retry backoff.
- **[ledger-service](ledger-service/README.md)** — real double-entry accounting: journals
  must balance to zero or the post is rejected; trial balance; period close/lock.
- **[reconciliation-service](reconciliation-service/README.md)** — statement-vs-ledger
  matching with a genuine exception lifecycle, not just CRUD.
- **[notification-service](notification-service/README.md)** — pluggable
  email/SMS/webhook delivery, suppression list, versioned/localized templates.
- **[audit-service](audit-service/README.md)** — SHA-256 hash-chained, Merkle-sealed
  tamper-evident audit log; the platform's only general-purpose Kafka consumer.
- **[reporting-service](reporting-service/README.md)** — Postgres for job/schedule
  metadata, ClickHouse for OLAP report content; PDF/Excel/CSV export.

---

## 5. How to explain this project in an interview

### Elevator pitch (30 seconds)

> "It's a 17-service Spring Boot payment platform covering the full transaction lifecycle —
> authentication, tokenization, risk and limits, authorization, clearing, settlement, a
> real double-entry ledger, disputes, reconciliation, and a tamper-evident audit trail. Each
> service owns its own Postgres schema and is independently deployable. Services talk
> synchronously over WebClient where a caller needs an answer to proceed — a limit check, a
> fraud decision — and asynchronously over Kafka via the transactional outbox pattern where
> they don't. I went through the whole codebase, found and fixed the gap between what was
> documented and what was actually wired up, and finished the integration work."

### Questions you're likely to get, and grounded answers

**"Walk me through what happens when a payment is made."**
Use §3.1 above. Lead with two specific design details: the DB-backed state-machine allow-list
in `payment-service` (transitions are data, not code — you can add a new state without a
redeploy of the transition logic), and the two-phase reserve/commit/release pattern in
`limit-service` (a limit check isn't a single boolean — it's a hold that gets finalized or
released later, exactly like an inventory reservation).

**"How do these services communicate, and why that choice?"**
WebClient for the request path (synchronous, needs an answer now), Kafka+outbox for
everything else (asynchronous, at-least-once, no dual-write problem). Be ready to explain
the outbox pattern itself: why writing the event to a table in the same transaction as the
state change, then relaying it separately, beats publishing to Kafka directly inside the
request — a mid-request crash after the DB commit but before the Kafka publish would
otherwise lose the event silently.

**"What did you find wrong with the platform, and what did you fix?"**
Be specific and concrete — this is the strongest part of the story:

- Every service defaulted to a handful of colliding ports (`payment`/`clearing`/`ledger` all
  on 8080, `limit`/`settlement` both on 8085) — gave every service a unique port.
- `settings.gradle` referenced three directories that didn't exist and silently excluded
  `reporting-service`, which had real, substantial code but had never actually been built —
  fixed the module list.
- `fraud-service` and `audit-service` were on MongoDB with no fixed port or connection
  string configured at all for fraud-service — migrated both to Postgres + Flyway, and gave
  fraud-service a real port and datasource.
- `dispute-service` ran on in-memory H2 — its data didn't survive a restart — migrated to
  real Postgres + Flyway.
- The "integrations" between services were either simulators on the wrong ports, or logging
  stubs that never called anything — wired the core payment path for real with WebClient:
  `payment-service` ↔ `limit-service`/`fraud-service`/`tokenization-service`,
  `settlement-service` ↔ `ledger-service`/`merchant-service`,
  `dispute-service` ↔ `payment-service`/`ledger-service`/`settlement-service`.
- `gateway-service` only had routes for 4 of the 17 services — added the other 13, each with
  matching rate-limit/circuit-breaker/retry config.

**"What's still not finished, and what would you do next?"**
Also a strength if you can name it precisely rather than saying "it's all done":

- `dispute-service` → `notification-service` is still a logging stub — there's no resolvable
  recipient address on a dispute record yet; next step would be a merchant-contact lookup.
- `settlement-service`'s ledger postings address accounts by a deterministic naming
  convention (`merchant:<id>:settlement_liability`) rather than a real account-discovery
  call, because `ledger-service` has no merchant-aware account lookup endpoint yet — those
  accounts need to be pre-provisioned during merchant onboarding.
- Fee/reserve-rate configuration in `settlement-service`'s merchant profile still falls back
  to platform defaults because `merchant-service` doesn't expose a fee-schedule endpoint yet.

**"How is PCI scope handled?"**
`tokenization-service` isolates PAN/bank-detail storage with AES-256-GCM and a DEK registry
for envelope encryption, 7-year retention. `payment-service`'s vault client only ever fetches
masked instrument metadata — never raw PAN. Worth naming one honest gap: the instrument
access-log controller exposes a DELETE endpoint, which undermines its value as an audit
trail — contrast with `audit-service`'s actual hash-chained/Merkle-sealed design, which is
the correct pattern for that job and a much stronger answer if asked "how would you fix it."

**"What's the accounting model?"**
`ledger-service` is real double-entry bookkeeping — every journal's entries must sum to zero
per currency or the post is rejected server-side (`UnbalancedJournalException`); there's a
genuine trial-balance report and accounting-period close/lock semantics. Reversals above a
configured threshold require an approver. `BalanceShard`/`BalanceSnapshot` point at sharded
balance materialization for scale, rather than summing entries on every balance read.

**"How would this scale / what would break first?"**
Good places to point: `limit-service`'s Redis-backed velocity counters are the natural
horizontal-scale seam; `ledger-service`'s balance snapshots exist specifically because
summing entries per read doesn't scale; `clearing-service`'s batch scheduler runs on a fixed
cron, which is fine at moderate volume but would need to move to a queue-driven trigger under
much higher transaction rates.

---

## 6. Running the platform locally

```bash
./gradlew build              # compiles and tests all 17 modules
./gradlew :payment-service:bootRun   # run a single service (repeat per service, own port/DB)
```

Each service reads its Postgres connection from `DB_HOST`/`DB_PORT`/`DB_NAME`/
`DB_USERNAME`/`DB_PASSWORD` (see each service's `README.md` for its specific defaults), and
its own port from `SERVER_PORT` (defaults per the table in §1). Kafka bootstrap servers
default to `localhost:9092` across every service that produces or consumes events.
`settlement-service` additionally has a `dev` profile that runs entirely on in-memory H2
with simulated downstream clients, useful for a self-contained demo without standing up
Postgres or the rest of the platform.

## 7. Service Runners

dispute-service → payment, ledger, settlement, notification
payment-service → fraud, tokenization, limit
settlement-service → ledger, merchant
user-service → authentication-service (for real JWTs; it'll boot without it, just rejects requests)
gateway-service → authentication-service (JWT validation) + whatever backends you're routing to
