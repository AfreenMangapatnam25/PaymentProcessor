# Settlement Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** `settlement-service` calls out to two other services:

- **ledger-service** (`LEDGER_SERVICE_URL`, default `:8092`)
- **merchant-service** (`MERCHANT_SERVICE_URL`, default `:8083`)

It starts fine without them, but batch reconciliation and merchant/settlement-
account lookups will fail at request time until both are up. Start them first for
a full end-to-end test.

**Infrastructure:** in the default `dev` profile, an in-memory H2 database (no
external Postgres needed); the `prod` profile uses Postgres (`settlementservicedb`)
with Flyway. Config Server is optional.

Base URL: `http://localhost:8091` (`server.port` in `application.yml`, overridable via `SERVER_PORT`)

No authentication is required by any endpoint below.

**Profiles**: this service defaults to the `dev` profile (`spring.profiles.active` defaults to
`dev`), which uses an **H2 in-memory** database (`ddl-auto: update`, Flyway disabled). The
alternate `prod` profile uses **PostgreSQL with Flyway migrations**. This differs from the other
services in this repo, which use `local`/no-profile setups.

Running under `dev` (the default), Spring Boot auto-executes `src/main/resources/data.sql` on
startup (`spring.sql.init.mode: always`, set only in the `dev` profile block), seeding:

- `settlement_batches` id `batch-seed-0001` (merchant `MERCH-1001`, USD, status `COMPLETED`, net 441900 minor units)
- `payouts` id `payout-seed-0001` (same batch, rail `ACH`, status `COMPLETED`)
- `reserves` id `reserve-seed-0001` (kind `ROLLING`, status `HELD`, amount 25000 minor units)

The GET examples below use these seeded ids directly. All monetary values are **minor units**
(e.g. cents) unless stated otherwise.

---

## SettlementBatchController (`/api/settlement-batches`)

### GET /api/settlement-batches

Query params (optional): `merchantId`, `status` (`PENDING`, `APPROVED`, `INITIATED`,
`PROCESSING`, `COMPLETED`, `FAILED`, `RECONCILED`, `REVERSED`, `ROLLED_OVER`).

curl:

```bash
curl "http://localhost:8091/api/settlement-batches?merchantId=MERCH-1001"
```

### GET /api/settlement-batches/{id}

Response (200):

```json
{
  "id": "batch-seed-0001",
  "merchantId": "MERCH-1001",
  "currency": "USD",
  "scheduleType": "DAILY",
  "status": "COMPLETED",
  "periodStart": "2026-07-24T00:00:00Z",
  "periodEnd": "2026-07-24T23:59:59Z",
  "grossMinor": 500000,
  "refundsMinor": 10000,
  "chargebacksMinor": 0,
  "feesMinor": 15000,
  "interchangeMinor": 8000,
  "reserveMinor": 25000,
  "settlementFeeMinor": 100,
  "adjustmentsMinor": 0,
  "netMinor": 441900,
  "ledgerJournalId": "LJ-SEED-0001",
  "approvedBy": "ops-analyst-jane",
  "approvedAt": "2026-07-25T01:00:00Z",
  "closedAt": "2026-07-25T02:00:00Z",
  "failureReason": null,
  "createdAt": "2026-07-25T00:30:00Z",
  "updatedAt": "2026-07-25T02:00:00Z"
}
```

curl:

```bash
curl http://localhost:8091/api/settlement-batches/batch-seed-0001
```

### GET /api/settlement-batches/{id}/payouts

Response (200):

```json
[
  {
    "id": "payout-seed-0001",
    "batchId": "batch-seed-0001",
    "merchantId": "MERCH-1001",
    "payoutAccountId": "PAYACC-1001",
    "amountMinor": 441900,
    "currency": "USD",
    "rail": "ACH",
    "status": "COMPLETED",
    "providerRef": "PR-SEED-0001",
    "ledgerJournalId": "LJ-SEED-0002",
    "attemptCount": 1,
    "nextRetryAt": null,
    "failureCode": null,
    "failureReason": null,
    "failureCategory": null,
    "scheduledAt": "2026-07-25T01:05:00Z",
    "submittedAt": "2026-07-25T01:10:00Z",
    "paidAt": "2026-07-25T02:00:00Z",
    "createdAt": "2026-07-25T01:00:00Z",
    "updatedAt": "2026-07-25T02:00:00Z"
  }
]
```

curl:

```bash
curl http://localhost:8091/api/settlement-batches/batch-seed-0001/payouts
```

### POST /api/settlement-batches/{id}/initiate

Transitions a batch from `APPROVED` to `INITIATED` and sends the transfer instruction to the rail.
No request body.

curl:

```bash
curl -X POST http://localhost:8091/api/settlement-batches/batch-seed-0001/initiate
```

### POST /api/settlement-batches/{id}/reconcile

Compares the batch's internal totals against the external bank statement, moving it to
`RECONCILED` (from `COMPLETED`). No request body.

curl:

```bash
curl -X POST http://localhost:8091/api/settlement-batches/batch-seed-0001/reconcile
```

### POST /api/settlement-batches/{id}/reverse

Reverses a settled/reconciled batch to recover funds. `approverLevel` is one of `AUTO`,
`SUPERVISOR`, `FINANCE_DIRECTOR`.

Request body:

```json
{
  "reason": "Duplicate settlement detected during audit",
  "approvedBy": "finance.director.sam",
  "approverLevel": "FINANCE_DIRECTOR"
}
```

Response (200): `BatchResponse` with `status: "REVERSED"`.

curl:

```bash
curl -X POST http://localhost:8091/api/settlement-batches/batch-seed-0001/reverse \
  -H "Content-Type: application/json" \
  -d '{"reason":"Duplicate settlement detected during audit","approvedBy":"finance.director.sam","approverLevel":"FINANCE_DIRECTOR"}'
```

---

## SettlementRunController (`/api/settlement-runs`)

### POST /api/settlement-runs

Manually triggers a settlement run. If `merchantId` and `currency` are both supplied, only that
merchant/currency pair is settled (returns the created/updated `BatchResponse`, or `204 No Content`
if there was nothing to settle). Otherwise the full cycle runs across all merchants with pending
items (returns a `RunSummary`). `scheduleType` is one of `DAILY`, `WEEKLY`, `BI_WEEKLY`,
`MONTHLY`, `ON_DEMAND`, `INSTANT` (defaults to `DAILY`).

Request body (single merchant):

```json
{
  "scheduleType": "ON_DEMAND",
  "merchantId": "MERCH-1001",
  "currency": "USD"
}
```

Response (200, batch created):

```json
{
  "id": "batch-2026-07-25-0007",
  "merchantId": "MERCH-1001",
  "currency": "USD",
  "scheduleType": "ON_DEMAND",
  "status": "PENDING",
  "periodStart": "2026-07-25T00:00:00Z",
  "periodEnd": "2026-07-25T09:00:00Z",
  "grossMinor": 120000,
  "refundsMinor": 0,
  "chargebacksMinor": 0,
  "feesMinor": 3600,
  "interchangeMinor": 1800,
  "reserveMinor": 6000,
  "settlementFeeMinor": 100,
  "adjustmentsMinor": 0,
  "netMinor": 108500,
  "ledgerJournalId": null,
  "approvedBy": null,
  "approvedAt": null,
  "closedAt": null,
  "failureReason": null,
  "createdAt": "2026-07-25T09:00:00Z",
  "updatedAt": "2026-07-25T09:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8091/api/settlement-runs \
  -H "Content-Type: application/json" \
  -d '{"scheduleType":"ON_DEMAND","merchantId":"MERCH-1001","currency":"USD"}'
```

Request body (full cycle — body may also be omitted entirely):

```json
{
  "scheduleType": "DAILY"
}
```

Response (200):

```json
{
  "batchesCreated": 42,
  "merchantsProcessed": 42,
  "totalNetMinor": 18345600
}
```

curl:

```bash
curl -X POST http://localhost:8091/api/settlement-runs \
  -H "Content-Type: application/json" \
  -d '{"scheduleType":"DAILY"}'
```

---

## SettlementItemController (`/api/settlement-items`)

Ingestion is idempotent on `idempotencyKey`.

### POST /api/settlement-items

`type` is one of `CAPTURE`, `REFUND`, `CHARGEBACK`, `CHARGEBACK_REVERSAL`, `PLATFORM_FEE`,
`INTERCHANGE_FEE`, `SETTLEMENT_FEE`, `RESERVE_HOLD`, `RESERVE_RELEASE`, `ADJUSTMENT_CREDIT`,
`ADJUSTMENT_DEBIT`, `PRIOR_PERIOD_CORRECTION`.

Request body:

```json
{
  "merchantId": "MERCH-1001",
  "type": "CAPTURE",
  "sourceType": "PAYMENT",
  "sourceId": "PAY-2002",
  "amountMinor": 10000,
  "currency": "USD",
  "effectiveAt": "2026-07-25T09:00:00Z",
  "idempotencyKey": "item-capture-pay-2002"
}
```

Response (201 Created):

```json
{
  "id": 501,
  "batchId": null,
  "merchantId": "MERCH-1001",
  "type": "CAPTURE",
  "sourceType": "PAYMENT",
  "sourceId": "PAY-2002",
  "amountMinor": 10000,
  "signedAmountMinor": 10000,
  "currency": "USD",
  "effectiveAt": "2026-07-25T09:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8091/api/settlement-items \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"MERCH-1001","type":"CAPTURE","sourceType":"PAYMENT","sourceId":"PAY-2002","amountMinor":10000,"currency":"USD","effectiveAt":"2026-07-25T09:00:00Z","idempotencyKey":"item-capture-pay-2002"}'
```

### GET /api/settlement-items/{id}

curl:

```bash
curl http://localhost:8091/api/settlement-items/501
```

### GET /api/settlement-items

Optional `batchId` filter.

curl:

```bash
curl "http://localhost:8091/api/settlement-items?batchId=batch-seed-0001"
```

---

## PayoutController (`/api/payouts`)

### GET /api/payouts

Optional `merchantId` or `batchId` filters (mutually exclusive; `merchantId` takes precedence).

curl:

```bash
curl "http://localhost:8091/api/payouts?merchantId=MERCH-1001"
```

### GET /api/payouts/{id}

Response (200): see `payout-seed-0001` example under `SettlementBatchController` above.

curl:

```bash
curl http://localhost:8091/api/payouts/payout-seed-0001
```

### POST /api/payouts/{id}/confirm

Simulated bank confirmation that a `PROCESSING`/`INITIATED` payout settled successfully.
No request body.

Response (200): `PayoutResponse` with `status: "COMPLETED"`, `paidAt` populated.

curl:

```bash
curl -X POST http://localhost:8091/api/payouts/payout-seed-0001/confirm
```

### POST /api/payouts/{id}/returns

Records a bank return against a payout (e.g. invalid/closed account).

Request body:

```json
{
  "reasonCode": "R03",
  "reasonDescription": "No account / unable to locate account"
}
```

Response (201 Created):

```json
{
  "id": "return-seed-0001",
  "payoutId": "payout-seed-0001",
  "reasonCode": "R03",
  "reasonDescription": "No account / unable to locate account",
  "amountMinor": 441900,
  "currency": "USD",
  "returnedAt": "2026-07-26T10:00:00Z",
  "ledgerJournalId": "LJ-SEED-0003",
  "createdAt": "2026-07-26T10:00:00Z",
  "updatedAt": "2026-07-26T10:00:00Z",
  "version": 0
}
```

curl:

```bash
curl -X POST http://localhost:8091/api/payouts/payout-seed-0001/returns \
  -H "Content-Type: application/json" \
  -d '{"reasonCode":"R03","reasonDescription":"No account / unable to locate account"}'
```

---

## PayoutReturnController (`/api/payout-returns`)

### GET /api/payout-returns

Optional `payoutId` filter.

curl:

```bash
curl "http://localhost:8091/api/payout-returns?payoutId=payout-seed-0001"
```

---

## ReserveController (`/api/reserves`)

### GET /api/reserves

Optional `merchantId` filter.

curl:

```bash
curl "http://localhost:8091/api/reserves?merchantId=MERCH-1001"
```

### GET /api/reserves/{id}

Response (200):

```json
{
  "id": "reserve-seed-0001",
  "merchantId": "MERCH-1001",
  "kind": "ROLLING",
  "rateBps": 500,
  "amountMinor": 25000,
  "releasedMinor": 0,
  "remainingMinor": 25000,
  "currency": "USD",
  "sourceBatchId": "batch-seed-0001",
  "holdUntil": "2026-10-23",
  "status": "HELD",
  "releasedAt": null,
  "createdAt": "2026-07-25T01:00:00Z"
}
```

curl:

```bash
curl http://localhost:8091/api/reserves/reserve-seed-0001
```

### POST /api/reserves/release-due

Releases every reserve whose `holdUntil` date has passed as of today. No request body.

Response (200):

```json
{
  "released": 3
}
```

curl:

```bash
curl -X POST http://localhost:8091/api/reserves/release-due
```

---

## AdjustmentController (`/api/adjustments`)

Supports an optional `Idempotency-Key` header on create — replaying the same key returns the
original response instead of creating a duplicate adjustment.

### POST /api/adjustments

`type` is one of `CREDIT`, `DEBIT`, `FEE_CORRECTION`, `RESERVE_RELEASE`, `RESERVE_HOLD`,
`CURRENCY_CORRECTION`.

Request body:

```json
{
  "merchantId": "MERCH-1001",
  "type": "CREDIT",
  "amountMinor": 5000,
  "currency": "USD",
  "reasonCode": "GOODWILL_CREDIT",
  "description": "Compensation for delayed payout",
  "requestedBy": "support.agent.alex"
}
```

Response (201 Created):

```json
{
  "id": "adj-seed-0001",
  "merchantId": "MERCH-1001",
  "type": "CREDIT",
  "amountMinor": 5000,
  "currency": "USD",
  "reasonCode": "GOODWILL_CREDIT",
  "description": "Compensation for delayed payout",
  "status": "PENDING_APPROVAL",
  "requiredApprovalLevel": "AUTO",
  "requestedBy": "support.agent.alex",
  "approvedBy": null,
  "approvedAt": null,
  "appliedBatchId": null,
  "appliedAt": null,
  "createdAt": "2026-07-25T09:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8091/api/adjustments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: adj-req-2026-07-25-001" \
  -d '{"merchantId":"MERCH-1001","type":"CREDIT","amountMinor":5000,"currency":"USD","reasonCode":"GOODWILL_CREDIT","description":"Compensation for delayed payout","requestedBy":"support.agent.alex"}'
```

### POST /api/adjustments/{id}/approve

Request body:

```json
{
  "approvedBy": "supervisor.morgan",
  "approverLevel": "SUPERVISOR"
}
```

Response (200): `AdjustmentResponse` with `status: "APPROVED"`.

curl:

```bash
curl -X POST http://localhost:8091/api/adjustments/adj-seed-0001/approve \
  -H "Content-Type: application/json" \
  -d '{"approvedBy":"supervisor.morgan","approverLevel":"SUPERVISOR"}'
```

### POST /api/adjustments/{id}/reject

`rejectedBy` is a query parameter.

Response (200): `AdjustmentResponse` with `status: "REJECTED"`.

curl:

```bash
curl -X POST "http://localhost:8091/api/adjustments/adj-seed-0001/reject?rejectedBy=supervisor.morgan"
```

### GET /api/adjustments/{id}

curl:

```bash
curl http://localhost:8091/api/adjustments/adj-seed-0001
```

### GET /api/adjustments

Optional `merchantId` filter.

curl:

```bash
curl "http://localhost:8091/api/adjustments?merchantId=MERCH-1001"
```

---

## ReportController (`/api/reports`) — read-only reports

### GET /api/reports/merchant-statement/{merchantId}

Merchant-facing statement: batches, payouts, and reserves for the merchant.

Response (200):

```json
{
  "merchantId": "MERCH-1001",
  "generatedAt": "2026-07-25T09:00:00Z",
  "totalNetSettledMinor": 441900,
  "batches": [
    {
      "id": "batch-seed-0001",
      "merchantId": "MERCH-1001",
      "status": "COMPLETED",
      "netMinor": 441900
    }
  ],
  "payouts": [
    {
      "id": "payout-seed-0001",
      "batchId": "batch-seed-0001",
      "status": "COMPLETED",
      "amountMinor": 441900
    }
  ],
  "reserves": [
    {
      "id": "reserve-seed-0001",
      "status": "HELD",
      "amountMinor": 25000,
      "remainingMinor": 25000
    }
  ]
}
```

curl:

```bash
curl http://localhost:8091/api/reports/merchant-statement/MERCH-1001
```

### GET /api/reports/daily-summary

Finance summary: batch counts and net totals by currency and status.

Response (200):

```json
{
  "generatedAt": "2026-07-25T09:00:00Z",
  "lines": [
    {
      "currency": "USD",
      "status": "COMPLETED",
      "batchCount": 41,
      "totalNetMinor": 18190400
    },
    {
      "currency": "USD",
      "status": "PENDING",
      "batchCount": 6,
      "totalNetMinor": 812300
    }
  ]
}
```

curl:

```bash
curl http://localhost:8091/api/reports/daily-summary
```

### GET /api/reports/pending-settlements

Operations report of settlements awaiting execution or in retry.

Response (200):

```json
{
  "generatedAt": "2026-07-25T09:00:00Z",
  "pendingBatches": [],
  "pendingPayouts": []
}
```

curl:

```bash
curl http://localhost:8091/api/reports/pending-settlements
```

### GET /api/reports/reserve-release-schedule/{merchantId}

Upcoming reserve releases for a merchant.

Response (200):

```json
{
  "generatedAt": "2026-07-25T09:00:00Z",
  "totalHeldMinor": 25000,
  "upcomingReleases": [
    {
      "id": "reserve-seed-0001",
      "merchantId": "MERCH-1001",
      "kind": "ROLLING",
      "rateBps": 500,
      "amountMinor": 25000,
      "releasedMinor": 0,
      "remainingMinor": 25000,
      "currency": "USD",
      "sourceBatchId": "batch-seed-0001",
      "holdUntil": "2026-10-23",
      "status": "HELD",
      "releasedAt": null,
      "createdAt": "2026-07-25T01:00:00Z"
    }
  ]
}
```

curl:

```bash
curl http://localhost:8091/api/reports/reserve-release-schedule/MERCH-1001
```

### GET /api/reports/exceptions

Operations report of failed, returned, and reversed settlements.

Response (200):

```json
{
  "generatedAt": "2026-07-25T09:00:00Z",
  "problemBatches": [],
  "problemPayouts": []
}
```

curl:

```bash
curl http://localhost:8091/api/reports/exceptions
```
