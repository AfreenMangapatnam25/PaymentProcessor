## Dependencies — what to run before this service

**Other services:** `dispute-service` calls out to four other services over HTTP
(configurable base URLs, all default to `localhost`):

- **payment-service** (`PAYMENT_SERVICE_URL`, default `:8087`)
- **ledger-service** (`LEDGER_SERVICE_URL`, default `:8092`)
- **settlement-service** (`SETTLEMENT_SERVICE_URL`, default `:8091`)
- **notification-service** (`NOTIFICATION_SERVICE_URL`, default `:8094`)

The service itself still starts fine without them, but flows that touch payment
lookups, ledger entries, settlement holds, or outbound notifications will fail at
request time until those four are up. Start them first if you want a full
end-to-end test.

**Infrastructure:** Postgres (`disputeservicedb`). Config Server is optional.

Base URL: http://localhost:8090
Auth: none

Run with the `local` Spring profile (`SPRING_PROFILES_ACTIVE=local`) to load the seed data in
`src/main/resources/db/seed/V2__seed_sample_data.sql`. All path-param examples below use the
deterministic ids from that seed file so they can be exercised as-is against a freshly seeded
local database.

Seed reference ids:

- Disputes: `11111111-1111-1111-1111-111111111111` (OPEN) ... `111111111116` (LOST/CLOSED)
- Evidence: `22222222-2222-2222-2222-222222222221` .. `222222222224`
- Representments: `44444444-4444-4444-4444-444444444441` .. `444444444443`

---

## Dispute

### POST /api/v1/disputes

Auth: none

Opens a new dispute from an inbound chargeback / retrieval notification.

Request body:

```json
{
  "chargebackId": "CB-2026-000123",
  "transactionId": "TXN-2026-000123",
  "paymentId": "PAY-2026-000123",
  "merchantId": "MERCH-SEED-01",
  "customerId": "CUST-SEED-99",
  "network": "VISA",
  "type": "CHARGEBACK",
  "source": "CARD_NETWORK",
  "reasonCode": "10.4",
  "reasonDescription": "Other Fraud - Card Absent Environment",
  "amountMinor": 15999,
  "currency": "USD",
  "partial": false,
  "chargebackRatePercent": 0.8
}
```

Response `201 Created`:

```json
{
  "id": "a3f1c9d2-...",
  "chargebackId": "CB-2026-000123",
  "transactionId": "TXN-2026-000123",
  "paymentId": "PAY-2026-000123",
  "merchantId": "MERCH-SEED-01",
  "customerId": "CUST-SEED-99",
  "network": "VISA",
  "type": "CHARGEBACK",
  "source": "CARD_NETWORK",
  "stage": "CHARGEBACK",
  "status": "OPEN",
  "reasonCode": "10.4",
  "reasonDescription": "Other Fraud - Card Absent Environment",
  "amountMinor": 15999,
  "currency": "USD",
  "chargebackFeeMinor": 1500,
  "partial": false,
  "liabilityParty": "PENDING",
  "receivedAt": "2026-07-25T09:00:00Z",
  "openedAt": "2026-07-25T09:00:05Z",
  "deadlineAt": "2026-08-14T09:00:05Z",
  "resolvedAt": null,
  "closedAt": null,
  "merchantNotified": false,
  "daysUntilDeadline": 20
}
```

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "chargebackId": "CB-2026-000123",
    "transactionId": "TXN-2026-000123",
    "paymentId": "PAY-2026-000123",
    "merchantId": "MERCH-SEED-01",
    "customerId": "CUST-SEED-99",
    "network": "VISA",
    "type": "CHARGEBACK",
    "source": "CARD_NETWORK",
    "reasonCode": "10.4",
    "reasonDescription": "Other Fraud - Card Absent Environment",
    "amountMinor": 15999,
    "currency": "USD",
    "partial": false,
    "chargebackRatePercent": 0.8
  }'
```

Notes: `network` is one of `VISA, MASTERCARD, AMEX, DISCOVER`; `type` is one of
`CHARGEBACK, RETRIEVAL_REQUEST, PRE_ARBITRATION`; `source` is one of
`CARD_NETWORK, ACQUIRER, PAYMENT_GATEWAY, INTERNAL`.

---

### GET /api/v1/disputes

Auth: none

Lists disputes, optionally filtered by merchant.

Query params:

- `merchantId` (optional) — filter to a single merchant, e.g. `MERCH-SEED-01`

Response `200 OK`:

```json
[
  {
    "id": "11111111-1111-1111-1111-111111111111",
    "chargebackId": "CB-SEED-0001",
    "transactionId": "TXN-SEED-0001",
    "paymentId": "PAY-SEED-0001",
    "merchantId": "MERCH-SEED-01",
    "customerId": "CUST-SEED-01",
    "network": "VISA",
    "type": "CHARGEBACK",
    "source": "CARD_NETWORK",
    "stage": "CHARGEBACK",
    "status": "OPEN",
    "reasonCode": "10.4",
    "reasonDescription": "Other Fraud - Card Absent Environment",
    "amountMinor": 15999,
    "currency": "USD",
    "chargebackFeeMinor": 1500,
    "partial": false,
    "liabilityParty": "PENDING",
    "receivedAt": "2026-07-01T09:00:00Z",
    "openedAt": "2026-07-01T09:05:00Z",
    "deadlineAt": "2026-07-21T09:05:00Z",
    "resolvedAt": null,
    "closedAt": null,
    "merchantNotified": false,
    "daysUntilDeadline": 0
  }
]
```

curl:

```bash
curl "http://localhost:8090/api/v1/disputes?merchantId=MERCH-SEED-01"
```

---

### GET /api/v1/disputes/{id}

Auth: none

Full dispute detail: evidence, representments, liability and timeline.

Response `200 OK`:

```json
{
  "dispute": {
    "id": "11111111-1111-1111-1111-111111111113",
    "status": "EVIDENCE_REVIEW",
    "...": "..."
  },
  "liability": {
    "id": "33333333-3333-3333-3333-333333333331",
    "party": "MERCHANT",
    "...": "..."
  },
  "evidence": [
    {
      "id": "22222222-2222-2222-2222-222222222222",
      "status": "ACCEPTED",
      "...": "..."
    }
  ],
  "representments": [],
  "timeline": [
    {
      "type": "DISPUTE_CREATED",
      "toStatus": "OPEN",
      "...": "..."
    }
  ]
}
```

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111113
```

---

### GET /api/v1/disputes/{id}/status

Auth: none

Current status only (returns the same shape as `DisputeResponse`).

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111111/status
```

---

### POST /api/v1/disputes/{id}/request-evidence

Auth: none

Requests evidence from the merchant (`OPEN` -> `PENDING_EVIDENCE`). No request body.

Response `200 OK`: `DisputeResponse` (see above), with `status: "PENDING_EVIDENCE"`.

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111111/request-evidence
```

---

### POST /api/v1/disputes/{id}/review

Auth: none

Moves the dispute into review once evidence is supplied. No request body.

Response `200 OK`: `DisputeResponse`, with `status: "EVIDENCE_REVIEW"`.

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111112/review
```

---

### POST /api/v1/disputes/{id}/accept

Auth: none

Merchant / platform accepts liability without fighting.

Query params:

- `actor` (optional) — who performed the action, e.g. `merchant-ops-01`

Response `200 OK`: `DisputeResponse`, with `status: "ACCEPTED"`.

curl:

```bash
curl -X POST "http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111112/accept?actor=merchant-ops-01"
```

---

### POST /api/v1/disputes/{id}/close

Auth: none

Closes a resolved dispute. No request body.

Response `200 OK`: `DisputeResponse`, with `closedAt` set.

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111115/close
```

---

## DisputeEvent

### GET /api/v1/disputes/{disputeId}/timeline

Auth: none

Returns the full audit timeline for a dispute, oldest first.

Response `200 OK`:

```json
[
  {
    "id": 1,
    "disputeId": "11111111-1111-1111-1111-111111111116",
    "type": "ARBITRATION_FILED",
    "actor": "merchant-ops-05",
    "description": "Case escalated to network arbitration",
    "fromStatus": "PRE_ARBITRATION",
    "toStatus": "ARBITRATION",
    "createdAt": "2026-06-15T10:00:00Z"
  },
  {
    "id": 2,
    "disputeId": "11111111-1111-1111-1111-111111111116",
    "type": "ARBITRATION_DECISION",
    "actor": "system",
    "description": "Arbitration decided against merchant",
    "fromStatus": "ARBITRATION",
    "toStatus": "LOST",
    "createdAt": "2026-07-15T16:00:00Z"
  }
]
```

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111116/timeline
```

---

## Evidence

### POST /api/v1/disputes/{disputeId}/evidence

Auth: none

Registers an uploaded evidence document against a dispute. Binary content is streamed to the
document store separately; this endpoint only registers the metadata.

Request body:

```json
{
  "fileName": "proof_of_delivery.pdf",
  "storageKey": "evidence/2026/07/proof_of_delivery.pdf",
  "category": "DELIVERY_CONFIRMATION",
  "sizeBytes": 184320,
  "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85",
  "description": "Signed carrier delivery confirmation",
  "uploadedBy": "merchant-ops-01"
}
```

Response `201 Created`:

```json
{
  "id": "9c2f4a1e-...",
  "disputeId": "11111111-1111-1111-1111-111111111112",
  "fileName": "proof_of_delivery.pdf",
  "type": "PDF",
  "category": "DELIVERY_CONFIRMATION",
  "sizeBytes": 184320,
  "status": "UPLOADED",
  "description": "Signed carrier delivery confirmation",
  "malwareScanned": false,
  "uploadedBy": "merchant-ops-01",
  "uploadedAt": "2026-07-25T09:10:00Z",
  "reviewedAt": null,
  "submittedAt": null
}
```

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111112/evidence \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "proof_of_delivery.pdf",
    "storageKey": "evidence/2026/07/proof_of_delivery.pdf",
    "category": "DELIVERY_CONFIRMATION",
    "sizeBytes": 184320,
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85",
    "description": "Signed carrier delivery confirmation",
    "uploadedBy": "merchant-ops-01"
  }'
```

Notes: `category` is one of `RECEIPT, INVOICE, TRACKING, DELIVERY_CONFIRMATION, SIGNATURE_PROOF,
COMMUNICATION, REFUND_POLICY, REFUND_PROOF, AUTHORIZATION_RECORD, AVS_CVV_RESULT, THREE_DS_PROOF,
DEVICE_FINGERPRINT, TERMS_OF_SERVICE, PRODUCT_DESCRIPTION, CANCELLATION_PROOF, OTHER`.

---

### GET /api/v1/disputes/{disputeId}/evidence

Auth: none

Lists all evidence for a dispute.

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111113/evidence
```

---

### GET /api/v1/evidence/{evidenceId}

Auth: none

Retrieves a single evidence document.

Response `200 OK`: same shape as the evidence object shown above.

curl:

```bash
curl http://localhost:8090/api/v1/evidence/22222222-2222-2222-2222-222222222222
```

---

### POST /api/v1/evidence/{evidenceId}/review

Auth: none

Records a review decision (accept / reject) on a document.

Request body:

```json
{
  "accepted": true,
  "reviewer": "review-team-01",
  "notes": "Delivery address matches billing address on file"
}
```

Response `200 OK`: evidence object with `status: "ACCEPTED"` (or `"REJECTED"`) and `reviewedAt` set.

curl:

```bash
curl -X POST http://localhost:8090/api/v1/evidence/22222222-2222-2222-2222-222222222223/review \
  -H "Content-Type: application/json" \
  -d '{
    "accepted": false,
    "reviewer": "review-team-01",
    "notes": "Email thread does not confirm item condition"
  }'
```

---

## Liability

### GET /api/v1/disputes/{disputeId}/liability

Auth: none

The current (latest) financial impact record for a dispute.

Response `200 OK`:

```json
{
  "id": "33333333-3333-3333-3333-333333333331",
  "disputeId": "11111111-1111-1111-1111-111111111113",
  "party": "MERCHANT",
  "disputedAmountMinor": 24999,
  "feeMinor": 1500,
  "totalMinor": 26499,
  "currency": "USD",
  "reserveTier": "STANDARD",
  "reservePercentage": 10,
  "ledgerJournalId": "JRNL-SEED-0003",
  "reversed": false,
  "recordedAt": "2026-07-05T12:00:00Z",
  "reversedAt": null
}
```

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111113/liability
```

---

### GET /api/v1/disputes/{disputeId}/liability/history

Auth: none

The full history of financial impact records for a dispute.

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111115/liability/history
```

---

## ReasonCodeCatalog

### GET /api/v1/reason-codes

Auth: none

All catalogued reason codes.

Response `200 OK`:

```json
[
  {
    "network": "VISA",
    "code": "10.4",
    "category": "Fraud",
    "description": "Other Fraud - Card Absent Environment",
    "winRate": "Low",
    "requiredEvidence": "AVS/CVV result, 3DS proof, device fingerprint",
    "optionalEvidence": "Prior undisputed transaction history",
    "responseDays": 20
  }
]
```

curl:

```bash
curl http://localhost:8090/api/v1/reason-codes
```

---

### GET /api/v1/reason-codes/{network}

Auth: none

Reason codes for a specific network. `network` is one of `VISA, MASTERCARD, AMEX, DISCOVER`.

curl:

```bash
curl http://localhost:8090/api/v1/reason-codes/VISA
```

---

### GET /api/v1/reason-codes/{network}/{code}

Auth: none

A single reason code for a network.

curl:

```bash
curl http://localhost:8090/api/v1/reason-codes/VISA/10.4
```

---

## Representment

### POST /api/v1/disputes/{disputeId}/representments

Auth: none

Assembles accepted evidence and submits a representment to the network.

Request body:

```json
{
  "narrative": "Accepted delivery confirmation and signature proof attached; goods were received by the customer at the billing address.",
  "submittedBy": "merchant-ops-03"
}
```

Response `201 Created`:

```json
{
  "id": "5b7e9a2c-...",
  "disputeId": "11111111-1111-1111-1111-111111111113",
  "stage": "REPRESENTMENT",
  "status": "SUBMITTED",
  "networkReference": "NETREF-2026-000456",
  "evidenceCount": 1,
  "feeMinor": 500,
  "issuerResponse": null,
  "submittedBy": "merchant-ops-03",
  "submittedAt": "2026-07-25T09:20:00Z",
  "decidedAt": null
}
```

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111113/representments \
  -H "Content-Type: application/json" \
  -d '{
    "narrative": "Accepted delivery confirmation and signature proof attached; goods were received by the customer at the billing address.",
    "submittedBy": "merchant-ops-03"
  }'
```

---

### GET /api/v1/disputes/{disputeId}/representments

Auth: none

Lists all representments / filings for a dispute.

curl:

```bash
curl http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111114/representments
```

---

### POST /api/v1/disputes/{disputeId}/issuer-response

Auth: none

Records the issuer's decision, driving the dispute to won or pre-arbitration.

Request body:

```json
{
  "response": "REJECTED",
  "actor": "network-webhook",
  "notes": "Issuer maintained the chargeback; case eligible for pre-arbitration"
}
```

Response `200 OK`: `DisputeResponse`, with `status` updated (e.g. `"PRE_ARBITRATION"`).

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111114/issuer-response \
  -H "Content-Type: application/json" \
  -d '{
    "response": "REJECTED",
    "actor": "network-webhook",
    "notes": "Issuer maintained the chargeback; case eligible for pre-arbitration"
  }'
```

Notes: `response` is one of `ACCEPTED, REJECTED, ESCALATED`.

---

### POST /api/v1/disputes/{disputeId}/arbitration

Auth: none

Escalates a rejected dispute to network arbitration.

Request body:

```json
{
  "narrative": "Requesting arbitration; merchant evidence conclusively shows authorized, delivered transaction.",
  "filedBy": "merchant-ops-05"
}
```

Response `201 Created`: representment object, with `stage: "ARBITRATION"`.

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111116/arbitration \
  -H "Content-Type: application/json" \
  -d '{
    "narrative": "Requesting arbitration; merchant evidence conclusively shows authorized, delivered transaction.",
    "filedBy": "merchant-ops-05"
  }'
```

---

### POST /api/v1/disputes/{disputeId}/arbitration-decision

Auth: none

Records the network's binding arbitration decision.

Request body:

```json
{
  "outcome": "LOST",
  "actor": "network-webhook",
  "notes": "Arbitration committee ruled in favor of the cardholder"
}
```

Response `200 OK`: `DisputeResponse`, with `status: "LOST"` (or `"WON"`) and `resolvedAt` set.

curl:

```bash
curl -X POST http://localhost:8090/api/v1/disputes/11111111-1111-1111-1111-111111111116/arbitration-decision \
  -H "Content-Type: application/json" \
  -d '{
    "outcome": "LOST",
    "actor": "network-webhook",
    "notes": "Arbitration committee ruled in favor of the cardholder"
  }'
```

Notes: `outcome` is one of `WON, LOST`.
