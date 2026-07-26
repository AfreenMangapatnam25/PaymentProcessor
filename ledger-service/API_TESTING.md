# Ledger Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `ledger-service` makes no outbound calls to any other
service — it's called *by* `dispute-service` and `settlement-service`, not the
other way around. Fully testable standalone.

**Infrastructure:** Postgres (`ledgerservicedb`). Config Server is optional.

`ledger-service` is an immutable, append-only double-entry ledger. It runs on
**`server.port: 8092`** (override via `SERVER_PORT`), so all examples below
target `http://localhost:8092`. Every endpoint requires a JWT bearer token by
default; the `local` profile turns that off — see [Authentication](#authentication).

Examples that reference specific ids (`1101`, `ACC-CUST-0001`,
`JRNL-SEED-0001`, `PERIOD-2026`, ...) use the deterministic sample data
seeded by `db/seed/V3__seed_sample_data.sql` when running with the `local`
Spring profile (see that file and the `local` profile document added to
`application.yml`, which points `spring.flyway.locations` at
`classpath:db/migration,classpath:db/seed`). `1101` (Cash - Settlement
Account) and `PERIOD-2026` come from the pre-existing `V2__seed_reference_data.sql`.

## Authentication

This service is now an OAuth2 **resource server**: every endpoint below requires
`Authorization: Bearer <accessToken>` by default. Tokens are RS256 JWTs issued by
`authentication-service` (port 8081) and validated locally against its JWKS at
`http://localhost:8081/.well-known/jwks.json` — signature, issuer, expiry, plus the `purpose`
claim, which must be `access` (refresh / step-up tokens are rejected). Claims map to authorities
as `scope` (space-delimited) -> `SCOPE_*`, and `principal_type` (`USER`, `MERCHANT`, `ADMIN`,
`SERVICE`) -> one `ROLE_*`. See `config/SecurityConfig.java`.

**Getting a token.** Log in against `authentication-service` on port 8081 — password login
(`POST http://localhost:8081/api/v1/auth/login`) or social login (Google / GitHub / Microsoft).
The token comes back as `tokens.accessToken`. See `authentication-service/API_TESTING.md` for the
full password/MFA and OAuth2 social-login flows.

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"<password>"}' \
  | jq -r '.tokens.accessToken')
```

**Testing without a token.** `security.jwt.enabled` (env `SECURITY_JWT_ENABLED`) defaults to
`true`. The `local` profile document in `application.yml` sets it to `false`, which swaps in a
permit-all chain. `spring.profiles.active` already defaults to `local` here, so the plain
`curl` commands in this guide work as-is.
Never set it to `false` outside a developer machine or an ephemeral CI container.

**Always public** (no token, in either mode): `/actuator/health/**`, `/actuator/info`,
`/actuator/prometheus`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`.

**The same call, both ways:**

```bash
# with the `local` profile (security.jwt.enabled=false) — works as written
curl http://localhost:8092/api/v1/accounts/ACC-CUST-0001

# with the toggle on (the default) — token required
curl http://localhost:8092/api/v1/accounts/ACC-CUST-0001 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Accounts — `AccountController` (`/api/v1/accounts`)

### POST /api/v1/accounts

Request body:

```json
{
  "id": "ACC-CUST-0002",
  "accountCode": "ACC-CUST-0002",
  "name": "Customer Wallet - John Smith",
  "typeCode": "LIABILITY",
  "currency": "USD",
  "ownerType": "CUSTOMER",
  "ownerId": "CUST-1002",
  "parentAccountId": null
}
```

Response (201 Created):

```json
{
  "id": "ACC-CUST-0002",
  "accountCode": "ACC-CUST-0002",
  "name": "Customer Wallet - John Smith",
  "ownerType": "CUSTOMER",
  "ownerId": "CUST-1002",
  "typeCode": "LIABILITY",
  "classification": "LIABILITY",
  "normalBalance": "CREDIT",
  "currency": "USD",
  "parentAccountId": null,
  "status": "ACTIVE",
  "createdAt": "2026-07-25T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
        "id": "ACC-CUST-0002",
        "accountCode": "ACC-CUST-0002",
        "name": "Customer Wallet - John Smith",
        "typeCode": "LIABILITY",
        "currency": "USD",
        "ownerType": "CUSTOMER",
        "ownerId": "CUST-1002"
      }'
```

### GET /api/v1/accounts

Response (200):

```json
[
  {
    "id": "1101",
    "accountCode": "1101",
    "name": "Cash - Settlement Account",
    "ownerType": null,
    "ownerId": null,
    "typeCode": "ASSET",
    "classification": "ASSET",
    "normalBalance": "DEBIT",
    "currency": "USD",
    "parentAccountId": null,
    "status": "ACTIVE",
    "createdAt": "2026-01-15T10:00:00Z"
  },
  {
    "id": "ACC-CUST-0001",
    "accountCode": "ACC-CUST-0001",
    "name": "Customer Wallet - Jane Doe",
    "ownerType": "CUSTOMER",
    "ownerId": "CUST-1001",
    "typeCode": "LIABILITY",
    "classification": "LIABILITY",
    "normalBalance": "CREDIT",
    "currency": "USD",
    "parentAccountId": null,
    "status": "ACTIVE",
    "createdAt": "2026-01-15T10:00:00Z"
  }
]
```

curl:

```bash
curl http://localhost:8092/api/v1/accounts
```

### GET /api/v1/accounts/{id}

Response (200): same shape as a single element above.
curl (using the seeded sample customer wallet):

```bash
curl http://localhost:8092/api/v1/accounts/ACC-CUST-0001
```

### POST /api/v1/accounts/{id}/deactivate

Response (200):

```json
{
  "id": "ACC-MERCH-0001",
  "accountCode": "ACC-MERCH-0001",
  "name": "Merchant Payable - Acme Co",
  "ownerType": "MERCHANT",
  "ownerId": "MERCH-2001",
  "typeCode": "LIABILITY",
  "classification": "LIABILITY",
  "normalBalance": "CREDIT",
  "currency": "USD",
  "parentAccountId": null,
  "status": "INACTIVE",
  "createdAt": "2026-01-15T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/accounts/ACC-MERCH-0001/deactivate
```

---

## Balances — `AccountBalanceController`

### GET /api/v1/accounts/{accountId}/balance

Response (200):

```json
{
  "accountId": "ACC-CUST-0001",
  "currency": "USD",
  "normalBalance": "CREDIT",
  "postedMinor": 10000,
  "pendingMinor": 0,
  "heldMinor": 0,
  "availableMinor": 10000,
  "entryHighWater": 1,
  "version": 1,
  "updatedAt": "2026-01-15T10:00:00Z"
}
```

curl:

```bash
curl http://localhost:8092/api/v1/accounts/ACC-CUST-0001/balance
```

### GET /api/v1/balances/{accountId}

Alias of the above; same response shape.
curl:

```bash
curl http://localhost:8092/api/v1/balances/ACC-CUST-0001
```

---

## Account Types — `AccountTypeController` (`/api/v1/account-types`, raw entity)

### GET /api/v1/account-types

Response (200):

```json
[
  {
    "code": "ASSET",
    "classification": "ASSET",
    "normalBalance": "DEBIT",
    "description": "Resources owned by the platform"
  },
  {
    "code": "LIABILITY",
    "classification": "LIABILITY",
    "normalBalance": "CREDIT",
    "description": "Obligations owed by the platform"
  }
]
```

curl:

```bash
curl http://localhost:8092/api/v1/account-types
```

### GET /api/v1/account-types/{code}

curl:

```bash
curl http://localhost:8092/api/v1/account-types/LIABILITY
```

---

## Balance Shards — `BalanceShardController` (`/api/balance-shards`, raw entity)

### GET /api/balance-shards

Response (200):

```json
[
  {
    "accountId": "1101",
    "shard": 0,
    "postedMinor": 10000
  }
]
```

curl:

```bash
curl http://localhost:8092/api/balance-shards
```

### POST /api/balance-shards

Request body (raw `BalanceShard` entity):

```json
{
  "accountId": "1101",
  "shard": 1,
  "postedMinor": 0
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/balance-shards \
  -H "Content-Type: application/json" \
  -d '{"accountId": "1101", "shard": 1, "postedMinor": 0}'
```

---

## Balance Snapshots — `BalanceSnapshotController`

### POST /api/v1/snapshots

Request body:

```json
{
  "asOfDate": "2026-01-15",
  "snapshotType": "EOD",
  "accountIds": [
    "ACC-CUST-0001",
    "1101"
  ]
}
```

Response (201 Created):

```json
[
  {
    "accountId": "ACC-CUST-0001",
    "asOfDate": "2026-01-15",
    "snapshotType": "EOD",
    "openingMinor": 0,
    "debitMinor": 0,
    "creditMinor": 10000,
    "closingMinor": 10000,
    "entryCount": 1,
    "entryHighWater": 1,
    "lastEntryAt": "2026-01-15T10:00:00Z",
    "createdAt": "2026-07-25T10:00:00Z"
  }
]
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/snapshots \
  -H "Content-Type: application/json" \
  -d '{"asOfDate": "2026-01-15", "snapshotType": "EOD", "accountIds": ["ACC-CUST-0001", "1101"]}'
```

### GET /api/v1/snapshots?date=2026-01-15

curl:

```bash
curl "http://localhost:8092/api/v1/snapshots?date=2026-01-15"
```

### GET /api/v1/accounts/{accountId}/snapshots

curl:

```bash
curl http://localhost:8092/api/v1/accounts/ACC-CUST-0001/snapshots
```

---

## Currencies — `CurrencyController` (`/api/v1/currencies`, raw entity)

### GET /api/v1/currencies

Response (200):

```json
[
  {
    "code": "USD",
    "exponent": 2,
    "name": "US Dollar"
  },
  {
    "code": "EUR",
    "exponent": 2,
    "name": "Euro"
  },
  {
    "code": "GBP",
    "exponent": 2,
    "name": "Pound Sterling"
  }
]
```

curl:

```bash
curl http://localhost:8092/api/v1/currencies
```

### GET /api/v1/currencies/{code}

curl:

```bash
curl http://localhost:8092/api/v1/currencies/USD
```

---

## Entries — `EntryController` (`/api/v1/entries`, paginated)

### GET /api/v1/entries?accountId={accountId}

Response (200, `Page<EntryResponse>`):

```json
{
  "content": [
    {
      "id": 2,
      "lineNumber": 2,
      "accountId": "ACC-CUST-0001",
      "direction": "CREDIT",
      "amountMinor": 10000,
      "currency": "USD",
      "description": "Seed deposit into customer wallet",
      "effectiveAt": "2026-01-15T10:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

curl:

```bash
curl "http://localhost:8092/api/v1/entries?accountId=ACC-CUST-0001&page=0&size=50"
```

### GET /api/v1/entries/{id}

curl (id `2` is the seeded credit line into `ACC-CUST-0001`):

```bash
curl http://localhost:8092/api/v1/entries/2
```

---

## Holds — `HoldController`

### POST /api/v1/holds

Request body:

```json
{
  "accountId": "ACC-CUST-0001",
  "amountMinor": 2500,
  "currency": "USD",
  "reason": "PENDING_AUTHORIZATION",
  "externalRef": "AUTH-2026-0001",
  "expiresAt": "2026-07-25T22:00:00Z"
}
```

Response (201 Created):

```json
{
  "id": "HOLD-3f9a1c2b",
  "accountId": "ACC-CUST-0001",
  "amountMinor": 2500,
  "currency": "USD",
  "reason": "PENDING_AUTHORIZATION",
  "status": "ACTIVE",
  "externalRef": "AUTH-2026-0001",
  "expiresAt": "2026-07-25T22:00:00Z",
  "createdAt": "2026-07-25T10:00:00Z",
  "releasedAt": null
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/holds \
  -H "Content-Type: application/json" \
  -d '{
        "accountId": "ACC-CUST-0001",
        "amountMinor": 2500,
        "currency": "USD",
        "reason": "PENDING_AUTHORIZATION",
        "externalRef": "AUTH-2026-0001",
        "expiresAt": "2026-07-25T22:00:00Z"
      }'
```

### POST /api/v1/holds/{id}/release

curl:

```bash
curl -X POST http://localhost:8092/api/v1/holds/HOLD-3f9a1c2b/release
```

### GET /api/v1/holds/{id}

curl:

```bash
curl http://localhost:8092/api/v1/holds/HOLD-3f9a1c2b
```

### GET /api/v1/accounts/{accountId}/holds

curl:

```bash
curl http://localhost:8092/api/v1/accounts/ACC-CUST-0001/holds
```

---

## Journals — `JournalController` (`/api/v1/journals`)

Idempotency is enforced via the **request body field** `idempotencyKey`
(unique constraint `uq_journal_idempotency`), **not** a request header:
posting the same `idempotencyKey` twice returns the original journal instead
of creating a duplicate.

### POST /api/v1/journals

Request body:

```json
{
  "eventType": "PAYMENT_CAPTURED",
  "externalRef": "PAY-2026-0042",
  "idempotencyKey": "payment-capture-PAY-2026-0042",
  "description": "Capture of payment PAY-2026-0042",
  "effectiveAt": "2026-07-25T10:15:00Z",
  "createdBy": "payment-service",
  "metadata": {
    "paymentId": "PAY-2026-0042"
  },
  "lines": [
    {
      "accountId": "1101",
      "direction": "DEBIT",
      "amountMinor": 5000,
      "currency": "USD",
      "description": "Cash in"
    },
    {
      "accountId": "ACC-MERCH-0001",
      "direction": "CREDIT",
      "amountMinor": 5000,
      "currency": "USD",
      "description": "Merchant payable"
    }
  ]
}
```

Response (201 Created):

```json
{
  "id": "JRNL-8b3e5f10",
  "eventType": "PAYMENT_CAPTURED",
  "externalRef": "PAY-2026-0042",
  "idempotencyKey": "payment-capture-PAY-2026-0042",
  "description": "Capture of payment PAY-2026-0042",
  "status": "POSTED",
  "reversesJournalId": null,
  "reversedByJournalId": null,
  "reversalReason": null,
  "periodId": "PERIOD-2026",
  "effectiveAt": "2026-07-25T10:15:00Z",
  "postedAt": "2026-07-25T10:15:01Z",
  "createdBy": "payment-service",
  "lines": [
    {
      "id": 3,
      "lineNumber": 1,
      "accountId": "1101",
      "direction": "DEBIT",
      "amountMinor": 5000,
      "currency": "USD",
      "description": "Cash in",
      "effectiveAt": "2026-07-25T10:15:00Z"
    },
    {
      "id": 4,
      "lineNumber": 2,
      "accountId": "ACC-MERCH-0001",
      "direction": "CREDIT",
      "amountMinor": 5000,
      "currency": "USD",
      "description": "Merchant payable",
      "effectiveAt": "2026-07-25T10:15:00Z"
    }
  ],
  "totalDebitMinor": 5000,
  "totalCreditMinor": 5000,
  "balanced": true
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/journals \
  -H "Content-Type: application/json" \
  -d '{
        "eventType": "PAYMENT_CAPTURED",
        "externalRef": "PAY-2026-0042",
        "idempotencyKey": "payment-capture-PAY-2026-0042",
        "description": "Capture of payment PAY-2026-0042",
        "effectiveAt": "2026-07-25T10:15:00Z",
        "createdBy": "payment-service",
        "lines": [
          {"accountId": "1101", "direction": "DEBIT", "amountMinor": 5000, "currency": "USD"},
          {"accountId": "ACC-MERCH-0001", "direction": "CREDIT", "amountMinor": 5000, "currency": "USD"}
        ]
      }'
```

### GET /api/v1/journals?externalRef=...

`list()` returns an untyped body: either a plain array (when `externalRef` is
supplied) or a Spring `Page` object (when it is not) — best-guess shape below.

Response (200, no `externalRef` — paged):

```json
{
  "content": [
    {
      "id": "JRNL-SEED-0001",
      "eventType": "SEED_DEPOSIT",
      "externalRef": "SEED-REF-0001",
      "idempotencyKey": "seed-idempotency-0001",
      "status": "POSTED",
      "periodId": "PERIOD-2026",
      "effectiveAt": "2026-01-15T10:00:00Z",
      "postedAt": "2026-01-15T10:00:00Z",
      "totalDebitMinor": 10000,
      "totalCreditMinor": 10000,
      "balanced": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

curl:

```bash
curl "http://localhost:8092/api/v1/journals?page=0&size=50"
curl "http://localhost:8092/api/v1/journals?externalRef=SEED-REF-0001"
```

### GET /api/v1/journals/{id}

curl (seeded sample journal):

```bash
curl http://localhost:8092/api/v1/journals/JRNL-SEED-0001
```

### GET /api/v1/journals/{id}/entries

Returns an untyped `List<?>` — each element is best-guess-shaped like
`EntryResponse`:

```json
[
  {
    "id": 1,
    "lineNumber": 1,
    "accountId": "1101",
    "direction": "DEBIT",
    "amountMinor": 10000,
    "currency": "USD",
    "description": "Seed deposit source (platform cash)",
    "effectiveAt": "2026-01-15T10:00:00Z"
  },
  {
    "id": 2,
    "lineNumber": 2,
    "accountId": "ACC-CUST-0001",
    "direction": "CREDIT",
    "amountMinor": 10000,
    "currency": "USD",
    "description": "Seed deposit into customer wallet",
    "effectiveAt": "2026-01-15T10:00:00Z"
  }
]
```

curl:

```bash
curl http://localhost:8092/api/v1/journals/JRNL-SEED-0001/entries
```

### POST /api/v1/journals/{id}/reverse

Request body:

```json
{
  "reason": "ERRONEOUS_ENTRY",
  "description": "Reversing seed deposit — test correction",
  "createdBy": "ops-user",
  "approvedBy": null,
  "idempotencyKey": "reverse-JRNL-SEED-0001"
}
```

Response (201 Created): a new `JournalResponse` with `status: "REVERSED"`
lines (opposite directions) and `reversesJournalId` pointing at the original.
curl:

```bash
curl -X POST http://localhost:8092/api/v1/journals/JRNL-SEED-0001/reverse \
  -H "Content-Type: application/json" \
  -d '{
        "reason": "ERRONEOUS_ENTRY",
        "description": "Reversing seed deposit - test correction",
        "createdBy": "ops-user",
        "idempotencyKey": "reverse-JRNL-SEED-0001"
      }'
```

> Note: `approvedBy` is required whenever a reversed line exceeds
> `ledger.reversal.approval-threshold-minor` (default `100000`).

---

## Accounting Periods — `PeriodController` (`/api/v1/periods`)

### POST /api/v1/periods

Request body:

```json
{
  "code": "FY2027",
  "periodType": "YEARLY",
  "startDate": "2027-01-01",
  "endDate": "2027-12-31"
}
```

Response (201 Created):

```json
{
  "id": "PERIOD-FY2027",
  "code": "FY2027",
  "periodType": "YEARLY",
  "startDate": "2027-01-01",
  "endDate": "2027-12-31",
  "state": "OPEN",
  "createdAt": "2026-07-25T10:00:00Z",
  "closedAt": null
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/periods \
  -H "Content-Type: application/json" \
  -d '{"code": "FY2027", "periodType": "YEARLY", "startDate": "2027-01-01", "endDate": "2027-12-31"}'
```

### GET /api/v1/periods

curl:

```bash
curl http://localhost:8092/api/v1/periods
```

### GET /api/v1/periods/{id}

curl (seeded genesis period):

```bash
curl http://localhost:8092/api/v1/periods/PERIOD-2026
```

### POST /api/v1/periods/{id}/state

Request body:

```json
{
  "state": "CLOSING"
}
```

curl:

```bash
curl -X POST http://localhost:8092/api/v1/periods/PERIOD-2026/state \
  -H "Content-Type: application/json" \
  -d '{"state": "CLOSING"}'
```

---

## Statements — `StatementController`

### GET /api/v1/accounts/{accountId}/statement?from=...&to=...

Response (200):

```json
{
  "accountId": "ACC-CUST-0001",
  "currency": "USD",
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-02-01T00:00:00Z",
  "openingBalanceMinor": 0,
  "closingBalanceMinor": 10000,
  "lines": [
    {
      "entryId": 2,
      "journalId": "JRNL-SEED-0001",
      "lineNumber": 2,
      "direction": "CREDIT",
      "amountMinor": 10000,
      "currency": "USD",
      "description": "Seed deposit into customer wallet",
      "effectiveAt": "2026-01-15T10:00:00Z",
      "runningBalanceMinor": 10000
    }
  ]
}
```

curl:

```bash
curl "http://localhost:8092/api/v1/accounts/ACC-CUST-0001/statement?from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z"
```

---

## Trial Balance — `TrialBalanceController` (`/api/v1/trial-balance`)

### GET /api/v1/trial-balance?asOf=...

Response (200):

```json
{
  "asOf": "2026-07-25T10:00:00Z",
  "lines": [
    {
      "accountCode": "1101",
      "accountName": "Cash - Settlement Account",
      "classification": "ASSET",
      "debitMinor": 10000,
      "creditMinor": 0
    },
    {
      "accountCode": "ACC-CUST-0001",
      "accountName": "Customer Wallet - Jane Doe",
      "classification": "LIABILITY",
      "debitMinor": 0,
      "creditMinor": 10000
    }
  ],
  "totalDebitMinor": 10000,
  "totalCreditMinor": 10000,
  "balanced": true
}
```

curl:

```bash
curl "http://localhost:8092/api/v1/trial-balance?asOf=2026-07-25T10:00:00Z"
# or, defaulting asOf to now:
curl http://localhost:8092/api/v1/trial-balance
```
