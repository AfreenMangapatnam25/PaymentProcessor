# Limit Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `limit-service` makes no outbound calls — it's called
*by* `payment-service` (limit check/reserve/commit/release), not the other way
around. Fully testable standalone.

**Infrastructure:** Postgres (`limitservicedb`), Redis (`spring.data.redis`, used
for velocity counters). Eureka client is configured but optional — registration
failure won't block startup. Config Server is optional.

`limit-service` evaluates and reserves transaction-limit capacity. It runs on
**`server.port: 8085`**, so all examples below target
`http://localhost:8085`. There is **no authentication** on any endpoint in
this service.

Examples that reference specific ids use the deterministic sample data
seeded by `db/seed/V3__seed_sample_data.sql` when running with the `local`
Spring profile (see that file and the `local` profile document added to
`application.yml`, which points `spring.flyway.locations` at
`classpath:db/migration,classpath:db/seed`):

- `limit_configuration` id `a0000000-0000-0000-0000-000000000001` —
  CUSTOMER `CUST-1001` daily AMOUNT cap, threshold `5000.00 USD`, HARD.
- `limit_configuration` id `a0000000-0000-0000-0000-000000000002` —
  MERCHANT `MERCH-2001` daily COUNT cap, threshold `100`, SOFT.
- `usage_counter` id `b0000000-0000-0000-0000-000000000001` — `150.00`
  already reserved today against the customer config above.

---

## Limit Checks — `LimitCheckController` (`/api/v1/limits`)

### POST /api/v1/limits/check

A dry-run evaluation — does **not** reserve any capacity.

Request body:

```json
{
  "customerId": "CUST-1001",
  "merchantId": "MERCH-2001",
  "currency": "USD",
  "amount": 4900.00,
  "country": "US",
  "transactionId": "TXN-2026-0100"
}
```

Response (200) — amount would push the seeded customer daily usage
(`150.00` already reserved) over the `5000.00` HARD threshold:

```json
{
  "decision": "DECLINED",
  "hardViolations": [
    {
      "limitConfigId": "a0000000-0000-0000-0000-000000000001",
      "limitName": "Sample customer daily amount override",
      "dimension": "AMOUNT",
      "timeWindow": "DAILY",
      "enforcement": "HARD",
      "threshold": 5000.00,
      "currentUsage": 150.00,
      "attempted": 5050.00,
      "message": "Daily amount limit exceeded for customer CUST-1001"
    }
  ],
  "softViolations": [],
  "evaluated": 3
}
```

curl:

```bash
curl -X POST http://localhost:8085/api/v1/limits/check \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": "CUST-1001",
        "merchantId": "MERCH-2001",
        "currency": "USD",
        "amount": 4900.00,
        "country": "US",
        "transactionId": "TXN-2026-0100"
      }'
```

### GET /api/v1/limits/usage?customerId=...&currency=...

Response (200):

```json
[
  {
    "limitConfigId": "a0000000-0000-0000-0000-000000000001",
    "limitName": "Sample customer daily amount override",
    "dimension": "AMOUNT",
    "timeWindow": "DAILY",
    "windowKey": "CUST-1001:2026-07-25",
    "threshold": 5000.00,
    "used": 150.00,
    "remaining": 4850.00,
    "currency": "USD",
    "enforcement": "HARD"
  }
]
```

curl:

```bash
curl "http://localhost:8085/api/v1/limits/usage?customerId=CUST-1001&currency=USD"
```

---

## Limit Configuration — `LimitConfigController` (`/api/v1/limit-configs`, full CRUD)

### POST /api/v1/limit-configs

Request body:

```json
{
  "name": "Merchant weekly amount cap - Acme Co",
  "scope": "MERCHANT",
  "scopeId": "MERCH-2001",
  "dimension": "AMOUNT",
  "timeWindow": "WEEKLY",
  "threshold": 250000.00,
  "currency": "USD",
  "enforcement": "HARD",
  "priority": 60,
  "active": true,
  "timeZone": "UTC"
}
```

Response (201 Created):

```json
{
  "id": "c1a2b3c4-d5e6-4f70-8a9b-0c1d2e3f4a5b",
  "name": "Merchant weekly amount cap - Acme Co",
  "scope": "MERCHANT",
  "scopeId": "MERCH-2001",
  "dimension": "AMOUNT",
  "timeWindow": "WEEKLY",
  "threshold": 250000.00,
  "currency": "USD",
  "enforcement": "HARD",
  "priority": 60,
  "active": true,
  "timeZone": "UTC",
  "createdAt": "2026-07-25T10:00:00Z",
  "updatedAt": "2026-07-25T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8085/api/v1/limit-configs \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Merchant weekly amount cap - Acme Co",
        "scope": "MERCHANT",
        "scopeId": "MERCH-2001",
        "dimension": "AMOUNT",
        "timeWindow": "WEEKLY",
        "threshold": 250000.00,
        "currency": "USD",
        "enforcement": "HARD",
        "priority": 60,
        "active": true,
        "timeZone": "UTC"
      }'
```

### PUT /api/v1/limit-configs/{id}

Request body (full replace, same shape as create):

```json
{
  "name": "Sample customer daily amount override",
  "scope": "CUSTOMER",
  "scopeId": "CUST-1001",
  "dimension": "AMOUNT",
  "timeWindow": "DAILY",
  "threshold": 6000.00,
  "currency": "USD",
  "enforcement": "HARD",
  "priority": 50,
  "active": true,
  "timeZone": "UTC"
}
```

curl (updates the seeded customer config's threshold to `6000.00`):

```bash
curl -X PUT http://localhost:8085/api/v1/limit-configs/a0000000-0000-0000-0000-000000000001 \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Sample customer daily amount override",
        "scope": "CUSTOMER",
        "scopeId": "CUST-1001",
        "dimension": "AMOUNT",
        "timeWindow": "DAILY",
        "threshold": 6000.00,
        "currency": "USD",
        "enforcement": "HARD",
        "priority": 50,
        "active": true,
        "timeZone": "UTC"
      }'
```

### DELETE /api/v1/limit-configs/{id}

Soft-deletes (disables) the configuration. Response: `204 No Content`.
curl:

```bash
curl -X DELETE http://localhost:8085/api/v1/limit-configs/a0000000-0000-0000-0000-000000000002
```

### GET /api/v1/limit-configs/{id}

curl (seeded merchant config):

```bash
curl http://localhost:8085/api/v1/limit-configs/a0000000-0000-0000-0000-000000000002
```

### GET /api/v1/limit-configs

curl:

```bash
curl http://localhost:8085/api/v1/limit-configs
```

---

## Reservations — `ReservationController` (`/api/v1/reservations`)

Called by `payment-service` as part of the reserve → commit/release
lifecycle. `POST /reserve` returns `422 Unprocessable Entity` (via the
error envelope below) instead of `201` when a HARD limit is exceeded.

### POST /api/v1/reservations

Request body:

```json
{
  "transactionId": "TXN-2026-0200",
  "customerId": "CUST-1001",
  "merchantId": "MERCH-2001",
  "currency": "USD",
  "amount": 1200.00,
  "country": "US",
  "idempotencyKey": "reserve-TXN-2026-0200"
}
```

Response (201 Created):

```json
{
  "reservationId": "d2e3f4a5-b6c7-4d80-9e0f-1a2b3c4d5e6f",
  "transactionId": "TXN-2026-0200",
  "status": "RESERVED",
  "currency": "USD",
  "reservedAmount": 1200.00,
  "committedAmount": 0.00,
  "expiresAt": "2026-07-25T10:15:00Z",
  "softViolations": []
}
```

Response (422, hard limit exceeded — e.g. amount pushes the seeded customer
past the `5000.00` daily cap):

```json
{
  "timestamp": "2026-07-25T10:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Reservation declined: one or more hard limits exceeded",
  "path": "/api/v1/reservations",
  "violations": [
    {
      "limitConfigId": "a0000000-0000-0000-0000-000000000001",
      "limitName": "Sample customer daily amount override",
      "dimension": "AMOUNT",
      "timeWindow": "DAILY",
      "enforcement": "HARD",
      "threshold": 5000.00,
      "currentUsage": 150.00,
      "attempted": 5150.00,
      "message": "Daily amount limit exceeded for customer CUST-1001"
    }
  ]
}
```

curl:

```bash
curl -X POST http://localhost:8085/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
        "transactionId": "TXN-2026-0200",
        "customerId": "CUST-1001",
        "merchantId": "MERCH-2001",
        "currency": "USD",
        "amount": 1200.00,
        "country": "US",
        "idempotencyKey": "reserve-TXN-2026-0200"
      }'
```

### POST /api/v1/reservations/{reservationId}/commit

Request body:

```json
{
  "capturedAmount": 1200.00
}
```

Response (200):

```json
{
  "reservationId": "d2e3f4a5-b6c7-4d80-9e0f-1a2b3c4d5e6f",
  "transactionId": "TXN-2026-0200",
  "status": "COMMITTED",
  "currency": "USD",
  "reservedAmount": 1200.00,
  "committedAmount": 1200.00,
  "expiresAt": "2026-07-25T10:15:00Z",
  "softViolations": []
}
```

curl:

```bash
curl -X POST http://localhost:8085/api/v1/reservations/d2e3f4a5-b6c7-4d80-9e0f-1a2b3c4d5e6f/commit \
  -H "Content-Type: application/json" \
  -d '{"capturedAmount": 1200.00}'
```

### POST /api/v1/reservations/{reservationId}/release

Request body (optional):

```json
{
  "reason": "Payment declined by issuer"
}
```

Response (200): a `ReservationResponse` with `status: "RELEASED"`.
curl:

```bash
curl -X POST http://localhost:8085/api/v1/reservations/d2e3f4a5-b6c7-4d80-9e0f-1a2b3c4d5e6f/release \
  -H "Content-Type: application/json" \
  -d '{"reason": "Payment declined by issuer"}'
```

### GET /api/v1/reservations/{reservationId}

curl:

```bash
curl http://localhost:8085/api/v1/reservations/d2e3f4a5-b6c7-4d80-9e0f-1a2b3c4d5e6f
```

### GET /api/v1/reservations?transactionId=...

Response (200): array of `ReservationResponse`.
curl:

```bash
curl "http://localhost:8085/api/v1/reservations?transactionId=TXN-2026-0200"
```
