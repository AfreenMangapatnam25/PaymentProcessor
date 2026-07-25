# Reconciliation Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `reconciliation-service` makes no outbound calls to any
other service — bank statements and internal records are ingested directly via
its own REST API. Fully testable standalone.

**Infrastructure:** Postgres (`reconciliationservicedb`). Kafka for completion/
mismatch events (optional for REST testing). Config Server is optional.

Base URL: `http://localhost:8093` (`server.port` in `application.yml`, overridable via `SERVER_PORT`)

No authentication is required by any endpoint below.

Run with the `local` profile (`SPRING_PROFILES_ACTIVE=local`) to have Flyway also apply
`db/seed/V2__seed_sample_data.sql`, which seeds:

- `recon_run` id `1` / uuid `11111111-1111-1111-1111-111111111111` (COMPLETED, ACQUIRER/VISA_ACQUIRING, business date
  2026-07-24)
- `recon_record` ids `1` (INTERNAL) and `2` (EXTERNAL), both `MATCHED`
- `recon_match` id `1` linking records 1 and 2

The GET examples below use these seeded ids directly.

---

## Main workflow: ReconRunController (`/api/v1/recon-runs`)

The core reconciliation workflow is a numbered sequence:

1. **Create a run** — `POST /api/v1/recon-runs`
2. **Add records** (internal and/or external) — `POST /api/v1/recon-runs/{id}/records`
   (alternatively ingest external records from a bank statement via `POST /api/v1/statements/ingest?runId=`)
3. **Execute the run** (runs the matching engine) — `POST /api/v1/recon-runs/{id}/execute`
4. **View results** — `GET /api/v1/recon-runs/{id}/matches` and `GET /api/v1/recon-runs/{id}/exceptions`
5. **Work exceptions** — assign / resolve / escalate / defer / adjust via `ExceptionRecordController`

### 1. POST /api/v1/recon-runs

Create a new reconciliation run.

Request body:

```json
{
  "reconType": "ACQUIRER",
  "channel": "VISA_ACQUIRING",
  "accountRef": "ACC-100200",
  "businessDate": "2026-07-25",
  "currency": "USD",
  "triggeredBy": "ops-analyst-jane"
}
```

Response (201 Created, `Location: /api/v1/recon-runs/2`):

```json
{
  "id": 2,
  "uuid": "6f1c9b8e-2a3d-4e11-9c2a-1a2b3c4d5e6f",
  "reconType": "ACQUIRER",
  "channel": "VISA_ACQUIRING",
  "accountRef": "ACC-100200",
  "businessDate": "2026-07-25",
  "currency": "USD",
  "status": "PENDING",
  "startedAt": null,
  "completedAt": null,
  "totalInternal": 0,
  "totalExternal": 0,
  "matchedCount": 0,
  "mismatchedCount": 0,
  "missingInternalCount": 0,
  "missingExternalCount": 0,
  "duplicateCount": 0,
  "exceptionCount": 0,
  "matchedAmount": 0,
  "matchRate": null,
  "triggeredBy": "ops-analyst-jane",
  "failureReason": null,
  "createdAt": "2026-07-25T09:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/recon-runs \
  -H "Content-Type: application/json" \
  -d '{
        "reconType": "ACQUIRER",
        "channel": "VISA_ACQUIRING",
        "accountRef": "ACC-100200",
        "businessDate": "2026-07-25",
        "currency": "USD",
        "triggeredBy": "ops-analyst-jane"
      }'
```

### GET /api/v1/recon-runs/{id}

Response (200):

```json
{
  "id": 1,
  "uuid": "11111111-1111-1111-1111-111111111111",
  "reconType": "ACQUIRER",
  "channel": "VISA_ACQUIRING",
  "accountRef": "ACC-100200",
  "businessDate": "2026-07-24",
  "currency": "USD",
  "status": "COMPLETED",
  "startedAt": "2026-07-24T02:00:00Z",
  "completedAt": "2026-07-24T02:03:00Z",
  "totalInternal": 1,
  "totalExternal": 1,
  "matchedCount": 1,
  "mismatchedCount": 0,
  "missingInternalCount": 0,
  "missingExternalCount": 0,
  "duplicateCount": 0,
  "exceptionCount": 0,
  "matchedAmount": 250.0000,
  "matchRate": 1.0000,
  "triggeredBy": "seed-script",
  "failureReason": null,
  "createdAt": "2026-07-24T02:00:00Z"
}
```

curl:

```bash
curl http://localhost:8093/api/v1/recon-runs/1
```

### GET /api/v1/recon-runs/uuid/{uuid}

curl:

```bash
curl http://localhost:8093/api/v1/recon-runs/uuid/11111111-1111-1111-1111-111111111111
```

### GET /api/v1/recon-runs

Paginated list.

curl:

```bash
curl "http://localhost:8093/api/v1/recon-runs?page=0&size=20"
```

### 2. POST /api/v1/recon-runs/{id}/records

Attach a batch of internal and/or external records to a run.

Request body:

```json
{
  "records": [
    {
      "source": "INTERNAL",
      "sourceSystem": "payment-service",
      "externalReference": "TXN-2002",
      "internalPaymentId": "PAY-2002",
      "amount": 100.00,
      "feeAmount": 2.00,
      "currency": "USD",
      "transactionDate": "2026-07-25",
      "merchantId": "MERCH-1001",
      "counterparty": "Acme Corp",
      "cardBin": "411111",
      "cardLast4": "1111",
      "transactionStatus": "CAPTURED"
    },
    {
      "source": "EXTERNAL",
      "sourceSystem": "acquirer-visa",
      "externalReference": "TXN-2002",
      "arn": "ARN-99001200",
      "amount": 100.00,
      "feeAmount": 2.00,
      "currency": "USD",
      "transactionDate": "2026-07-25",
      "merchantId": "MERCH-1001",
      "counterparty": "Acme Corp",
      "transactionStatus": "SETTLED"
    }
  ]
}
```

Response (201 Created):

```json
[
  {
    "id": 3,
    "source": "INTERNAL",
    "sourceSystem": "payment-service",
    "externalReference": "TXN-2002",
    "arn": null,
    "internalPaymentId": "PAY-2002",
    "amount": 100.00,
    "feeAmount": 2.00,
    "currency": "USD",
    "transactionDate": "2026-07-25",
    "valueDate": null,
    "merchantId": "MERCH-1001",
    "counterparty": "Acme Corp",
    "cardBin": "411111",
    "cardLast4": "1111",
    "transactionStatus": "CAPTURED",
    "matchStatus": "UNMATCHED"
  },
  {
    "id": 4,
    "source": "EXTERNAL",
    "sourceSystem": "acquirer-visa",
    "externalReference": "TXN-2002",
    "arn": "ARN-99001200",
    "internalPaymentId": null,
    "amount": 100.00,
    "feeAmount": 2.00,
    "currency": "USD",
    "transactionDate": "2026-07-25",
    "valueDate": null,
    "merchantId": "MERCH-1001",
    "counterparty": "Acme Corp",
    "cardBin": null,
    "cardLast4": null,
    "transactionStatus": "SETTLED",
    "matchStatus": "UNMATCHED"
  }
]
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/recon-runs/2/records \
  -H "Content-Type: application/json" \
  -d '{"records":[{"source":"INTERNAL","sourceSystem":"payment-service","externalReference":"TXN-2002","internalPaymentId":"PAY-2002","amount":100.00,"feeAmount":2.00,"currency":"USD","transactionDate":"2026-07-25","merchantId":"MERCH-1001","counterparty":"Acme Corp","cardBin":"411111","cardLast4":"1111","transactionStatus":"CAPTURED"},{"source":"EXTERNAL","sourceSystem":"acquirer-visa","externalReference":"TXN-2002","arn":"ARN-99001200","amount":100.00,"feeAmount":2.00,"currency":"USD","transactionDate":"2026-07-25","merchantId":"MERCH-1001","counterparty":"Acme Corp","transactionStatus":"SETTLED"}]}'
```

### GET /api/v1/recon-runs/{id}/records

Paginated list of records on a run.

curl:

```bash
curl "http://localhost:8093/api/v1/recon-runs/1/records?page=0&size=50"
```

### 3. POST /api/v1/recon-runs/{id}/execute

Runs the matching engine over the run's internal/external records, producing matches and exceptions.

Response (200):

```json
{
  "id": 2,
  "uuid": "6f1c9b8e-2a3d-4e11-9c2a-1a2b3c4d5e6f",
  "reconType": "ACQUIRER",
  "channel": "VISA_ACQUIRING",
  "accountRef": "ACC-100200",
  "businessDate": "2026-07-25",
  "currency": "USD",
  "status": "COMPLETED",
  "startedAt": "2026-07-25T09:05:00Z",
  "completedAt": "2026-07-25T09:05:02Z",
  "totalInternal": 1,
  "totalExternal": 1,
  "matchedCount": 1,
  "mismatchedCount": 0,
  "missingInternalCount": 0,
  "missingExternalCount": 0,
  "duplicateCount": 0,
  "exceptionCount": 0,
  "matchedAmount": 100.00,
  "matchRate": 1.0000,
  "triggeredBy": "ops-analyst-jane",
  "failureReason": null,
  "createdAt": "2026-07-25T09:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/recon-runs/2/execute
```

### 4. GET /api/v1/recon-runs/{id}/matches

Response (200, page):

```json
{
  "content": [
    {
      "id": 1,
      "reconRunId": 1,
      "internalRecordId": 1,
      "externalRecordId": 2,
      "matchType": "EXACT",
      "matchRule": "AmountAndReferenceRule",
      "confidence": 1.0000,
      "amountVariance": 0.0000,
      "dateVarianceDays": 0,
      "note": "Seed sample match",
      "manual": false,
      "matchedAt": "2026-07-24T02:03:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 50
}
```

curl:

```bash
curl "http://localhost:8093/api/v1/recon-runs/1/matches?page=0&size=50"
```

### GET /api/v1/recon-runs/{id}/exceptions

Same shape as `GET /api/v1/exceptions`, filtered to this run.

curl:

```bash
curl "http://localhost:8093/api/v1/recon-runs/1/exceptions?page=0&size=50"
```

---

## MatchController (`/api/v1/matches`)

### GET /api/v1/matches/{id}

Response (200):

```json
{
  "id": 1,
  "reconRunId": 1,
  "internalRecordId": 1,
  "externalRecordId": 2,
  "matchType": "EXACT",
  "matchRule": "AmountAndReferenceRule",
  "confidence": 1.0000,
  "amountVariance": 0.0000,
  "dateVarianceDays": 0,
  "note": "Seed sample match",
  "manual": false,
  "matchedAt": "2026-07-24T02:03:00Z"
}
```

curl:

```bash
curl http://localhost:8093/api/v1/matches/1
```

---

## ReconRecordController (`/api/v1/records`)

### GET /api/v1/records/{id}

Response (200):

```json
{
  "id": 1,
  "source": "INTERNAL",
  "sourceSystem": "payment-service",
  "externalReference": "TXN-1001",
  "arn": null,
  "internalPaymentId": "PAY-1001",
  "amount": 250.0000,
  "feeAmount": 5.0000,
  "currency": "USD",
  "transactionDate": "2026-07-24",
  "valueDate": "2026-07-24",
  "merchantId": "MERCH-1001",
  "counterparty": "Acme Corp",
  "cardBin": "411111",
  "cardLast4": "1111",
  "transactionStatus": "CAPTURED",
  "matchStatus": "MATCHED"
}
```

curl:

```bash
curl http://localhost:8093/api/v1/records/1
```

---

## BankStatementController (`/api/v1/statements`)

### POST /api/v1/statements/ingest?runId=

Ingests a CSV bank statement and attaches its rows as `EXTERNAL` records to the given run.

Request body:

```json
{
  "statementReference": "STMT-2026-07-25-001",
  "bankName": "First National Bank",
  "accountType": "SETTLEMENT",
  "accountNumber": "ACC-100200",
  "format": "CSV",
  "currency": "USD",
  "statementDate": "2026-07-25",
  "openingBalance": 100000.00,
  "closingBalance": 100100.00,
  "csvContent": "reference,amount,date\nTXN-2002,100.00,2026-07-25\n"
}
```

Response (201 Created):

```json
{
  "id": 2,
  "statementReference": "STMT-2026-07-25-001",
  "bankName": "First National Bank",
  "accountType": "SETTLEMENT",
  "accountNumber": "ACC-100200",
  "format": "CSV",
  "currency": "USD",
  "statementDate": "2026-07-25",
  "openingBalance": 100000.00,
  "closingBalance": 100100.00,
  "recordCount": 1,
  "status": "INGESTED",
  "ingestedAt": "2026-07-25T09:10:00Z"
}
```

curl:

```bash
curl -X POST "http://localhost:8093/api/v1/statements/ingest?runId=2" \
  -H "Content-Type: application/json" \
  -d '{
        "statementReference": "STMT-2026-07-25-001",
        "bankName": "First National Bank",
        "accountType": "SETTLEMENT",
        "accountNumber": "ACC-100200",
        "format": "CSV",
        "currency": "USD",
        "statementDate": "2026-07-25",
        "openingBalance": 100000.00,
        "closingBalance": 100100.00,
        "csvContent": "reference,amount,date\nTXN-2002,100.00,2026-07-25\n"
      }'
```

### GET /api/v1/statements/{id}

curl:

```bash
curl http://localhost:8093/api/v1/statements/1
```

### GET /api/v1/statements

Paginated list.

curl:

```bash
curl "http://localhost:8093/api/v1/statements?page=0&size=20"
```

---

## ExceptionRecordController (`/api/v1/exceptions`)

### GET /api/v1/exceptions

Query params (all optional): `status` (`OPEN`, `IN_REVIEW`, `RESOLVED`, `ESCALATED`, `DEFERRED`),
`severity` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), `queue` (`SENIOR_ANALYST`, `ESCALATED`,
`FRAUD_INVESTIGATION`, `COMPLIANCE`, `OPERATIONS_ANALYST`), `runId`, plus `page`/`size`.

curl:

```bash
curl "http://localhost:8093/api/v1/exceptions?status=OPEN&severity=HIGH&page=0&size=50"
```

### GET /api/v1/exceptions/{id}

Response (200):

```json
{
  "id": 10,
  "uuid": "22222222-2222-2222-2222-222222222222",
  "reconRunId": 2,
  "reconRecordId": 3,
  "category": "AMOUNT_MISMATCH",
  "severityScore": 68.50,
  "severityLevel": "HIGH",
  "status": "OPEN",
  "reviewQueue": "SENIOR_ANALYST",
  "amount": 100.00,
  "currency": "USD",
  "expectedAmount": 100.00,
  "actualAmount": 95.00,
  "externalReference": "TXN-2002",
  "description": "Internal amount 100.00 does not match external amount 95.00",
  "ageDays": 0,
  "detectedAt": "2026-07-25T09:05:02Z",
  "slaDueAt": "2026-07-25T13:05:02Z",
  "assignedTo": null,
  "resolutionType": null,
  "resolutionNote": null,
  "resolvedBy": null,
  "resolvedAt": null,
  "autoResolved": false
}
```

curl:

```bash
curl http://localhost:8093/api/v1/exceptions/10
```

### GET /api/v1/exceptions/uuid/{uuid}

curl:

```bash
curl http://localhost:8093/api/v1/exceptions/uuid/22222222-2222-2222-2222-222222222222
```

### POST /api/v1/exceptions/{id}/assign

Request body:

```json
{
  "assignee": "analyst.jane"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/exceptions/10/assign \
  -H "Content-Type: application/json" \
  -d '{"assignee": "analyst.jane"}'
```

### POST /api/v1/exceptions/{id}/resolve

Request body (`resolutionType` one of `MATCHED`, `ENTRY_POSTED`, `REVERSED`, `ADJUSTED`, `ESCALATED`, `DEFERRED`,
`AUTO_CORRECTED`, `WRITTEN_OFF`):

```json
{
  "resolutionType": "ADJUSTED",
  "note": "Posted a correcting adjustment for the $5.00 variance",
  "resolvedBy": "analyst.jane"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/exceptions/10/resolve \
  -H "Content-Type: application/json" \
  -d '{"resolutionType":"ADJUSTED","note":"Posted a correcting adjustment for the $5.00 variance","resolvedBy":"analyst.jane"}'
```

### POST /api/v1/exceptions/{id}/escalate

Request body:

```json
{
  "queue": "COMPLIANCE",
  "note": "Possible fraud pattern, escalating for review",
  "actor": "analyst.jane"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/exceptions/10/escalate \
  -H "Content-Type: application/json" \
  -d '{"queue":"COMPLIANCE","note":"Possible fraud pattern, escalating for review","actor":"analyst.jane"}'
```

### POST /api/v1/exceptions/{id}/defer

Request body:

```json
{
  "note": "Waiting on acquirer response, revisit next cycle",
  "actor": "analyst.jane"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/exceptions/10/defer \
  -H "Content-Type: application/json" \
  -d '{"note":"Waiting on acquirer response, revisit next cycle","actor":"analyst.jane"}'
```

### POST /api/v1/exceptions/{id}/adjustments

Create a corrective adjustment for an exception (`adjustmentType` one of `POST_ENTRY`, `REVERSE`, `FX_VARIANCE`,
`FEE_ADJUSTMENT`, `WRITE_OFF`).

Request body:

```json
{
  "adjustmentType": "FEE_ADJUSTMENT",
  "amount": 5.00,
  "currency": "USD",
  "reason": "Correct fee variance identified during reconciliation",
  "createdBy": "analyst.jane"
}
```

Response (201 Created):

```json
{
  "id": 1,
  "exceptionId": 10,
  "adjustmentType": "FEE_ADJUSTMENT",
  "amount": 5.00,
  "currency": "USD",
  "reason": "Correct fee variance identified during reconciliation",
  "ledgerReference": null,
  "status": "PENDING",
  "requiresDualApproval": false,
  "createdBy": "analyst.jane",
  "approvedBy": null,
  "approvedAt": null,
  "postedAt": null,
  "createdAt": "2026-07-25T09:15:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8093/api/v1/exceptions/10/adjustments \
  -H "Content-Type: application/json" \
  -d '{"adjustmentType":"FEE_ADJUSTMENT","amount":5.00,"currency":"USD","reason":"Correct fee variance identified during reconciliation","createdBy":"analyst.jane"}'
```

### GET /api/v1/exceptions/{id}/adjustments

curl:

```bash
curl http://localhost:8093/api/v1/exceptions/10/adjustments
```

---

## AdjustmentController (`/api/v1/adjustments`)

### GET /api/v1/adjustments/{id}

curl:

```bash
curl http://localhost:8093/api/v1/adjustments/1
```

### POST /api/v1/adjustments/{id}/approve

Request body:

```json
{
  "actor": "supervisor.morgan"
}
```

Response (200): same shape as `AdjustmentResponse`, with `status: "APPROVED"` and `approvedBy`/`approvedAt` populated.

curl:

```bash
curl -X POST http://localhost:8093/api/v1/adjustments/1/approve \
  -H "Content-Type: application/json" \
  -d '{"actor": "supervisor.morgan"}'
```

### POST /api/v1/adjustments/{id}/post

Request body:

```json
{
  "actor": "system.ledger-sync"
}
```

Response (200): `status: "POSTED"`, `postedAt` populated.

curl:

```bash
curl -X POST http://localhost:8093/api/v1/adjustments/1/post \
  -H "Content-Type: application/json" \
  -d '{"actor": "system.ledger-sync"}'
```

### POST /api/v1/adjustments/{id}/reject

Request body:

```json
{
  "actor": "supervisor.morgan"
}
```

Response (200): `status: "REJECTED"`.

curl:

```bash
curl -X POST http://localhost:8093/api/v1/adjustments/1/reject \
  -H "Content-Type: application/json" \
  -d '{"actor": "supervisor.morgan"}'
```
