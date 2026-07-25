# tokenization-service API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `tokenization-service` makes no outbound calls to any
other service — it's called *by* `payment-service` as the card/UPI vault, not the
other way around. Fully testable standalone.

**Infrastructure:** Postgres (`tokenizationservicedb`; note `ddl-auto: update` is
used here as a workaround since this service ships with no Flyway migrations —
see `application.yml` comments). Eureka client is configured but optional.
Config Server is optional.

Base URL: `http://localhost:8084` (see `server.port` in `application.yml`)

No authentication/authorization is enforced on any endpoint in this service.

All 7 controllers are generic CRUD wrappers directly over JPA `@Entity` classes — there is no DTO layer, so the
JSON request/response shape is exactly the entity's fields. Byte array (`byte[]`) fields (ciphertext, nonces,
fingerprints, wrapped keys) are serialized/deserialized by Jackson as **Base64-encoded strings**. The values
shown below are fake/dummy Base64 blobs — in a real deployment these would be produced by the service's AES-GCM
encryption layer, not supplied by the caller.

Every controller follows the same pattern:

- `GET /api/<resource>` — list all
- `POST /api/<resource>` — create
- `GET /api/<resource>/{id}` — get by id
- `PUT /api/<resource>/{id}` — update
- `DELETE /api/<resource>/{id}` — delete

---

## BinRangeController — `/api/bin-ranges`

Entity: `BinRange` (id: `Long`, auto-generated values are **not** assigned by the DB — `id` must be supplied by
the caller since there is no `@GeneratedValue`).

### GET /api/bin-ranges

Auth: none

Response (200):

```json
[
  {
    "id": 1,
    "binLow": 411111000000,
    "binHigh": 411111999999,
    "brand": "VISA",
    "funding": "CREDIT",
    "issuer": "CHASE",
    "country": "US",
    "productCode": "CLASSIC",
    "isPrepaid": false,
    "isCommercial": false
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/bin-ranges -H "Content-Type: application/json"
```

### POST /api/bin-ranges

Auth: none

Request body:

```json
{
  "id": 5,
  "binLow": 550000000000,
  "binHigh": 550000999999,
  "brand": "MASTERCARD",
  "funding": "DEBIT",
  "issuer": "WELLS FARGO",
  "country": "US",
  "productCode": "STANDARD",
  "isPrepaid": false,
  "isCommercial": false
}
```

Response (200):

```json
{
  "id": 5,
  "binLow": 550000000000,
  "binHigh": 550000999999,
  "brand": "MASTERCARD",
  "funding": "DEBIT",
  "issuer": "WELLS FARGO",
  "country": "US",
  "productCode": "STANDARD",
  "isPrepaid": false,
  "isCommercial": false
}
```

curl:

```bash
curl -X POST http://localhost:8084/api/bin-ranges -H "Content-Type: application/json" -d '{
  "id": 5,
  "binLow": 550000000000,
  "binHigh": 550000999999,
  "brand": "MASTERCARD",
  "funding": "DEBIT",
  "issuer": "WELLS FARGO",
  "country": "US",
  "productCode": "STANDARD",
  "isPrepaid": false,
  "isCommercial": false
}'
```

### GET /api/bin-ranges/{id}

Auth: none

Response (200): same shape as above. 404 (empty body) if not found.

curl:

```bash
curl -X GET http://localhost:8084/api/bin-ranges/1
```

### PUT /api/bin-ranges/{id}

Auth: none

Request body: full `BinRange` object (see POST). Note: the controller ignores the path `{id}` and simply calls
`save(entity)` using the `id` in the request body — make sure the body's `id` matches the path id.

curl:

```bash
curl -X PUT http://localhost:8084/api/bin-ranges/1 -H "Content-Type: application/json" -d '{
  "id": 1,
  "binLow": 411111000000,
  "binHigh": 411111999999,
  "brand": "VISA",
  "funding": "CREDIT",
  "issuer": "CHASE",
  "country": "US",
  "productCode": "PLATINUM",
  "isPrepaid": false,
  "isCommercial": false
}'
```

### DELETE /api/bin-ranges/{id}

Auth: none

Response: 204 No Content

curl:

```bash
curl -X DELETE http://localhost:8084/api/bin-ranges/1
```

---

## CardDetailController — `/api/card-details`

Entity: `CardDetail` (id: `instrumentId` String, caller-supplied). Sensitive fields (`panCiphertext`, `nonce`,
`aad`, `cardholderNameCiphertext`) are Base64 strings representing already-encrypted data — **never send a raw
PAN**; only `last4`/`bin`/`brand` are stored in the clear.

### GET /api/card-details

Auth: none

Response (200):

```json
[
  {
    "instrumentId": "instr-card-0001",
    "panCiphertext": "b64:QUJDREVGR0hJSktMTU5PUA==",
    "dekId": "dek-0001",
    "nonce": "MTIzNDU2Nzg5MDEy",
    "aad": "aW5zdHItY2FyZC0wMDAx",
    "expMonth": 12,
    "expYear": 2028,
    "last4": "1111",
    "bin": "411111",
    "brand": "VISA",
    "funding": "CREDIT",
    "issuerCountry": "US",
    "productCode": "CLASSIC",
    "cardholderNameCiphertext": "ZW5jcnlwdGVkLW5hbWUtYnl0ZXM="
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/card-details
```

### POST /api/card-details

Auth: none

Request body (test card `4111111111111111`, Visa test PAN — only ciphertext/last4/bin are actually stored):

```json
{
  "instrumentId": "instr-card-0099",
  "panCiphertext": "b64:ZmFrZS1jaXBoZXJ0ZXh0LWZvci00MTExMTExMTExMTExMTEx",
  "dekId": "dek-0001",
  "nonce": "bm9uY2UtMTIzNDU2Nzg=",
  "aad": "aW5zdHItY2FyZC0wMDk5",
  "expMonth": 9,
  "expYear": 2027,
  "last4": "1111",
  "bin": "411111",
  "brand": "VISA",
  "funding": "CREDIT",
  "issuerCountry": "US",
  "productCode": "CLASSIC",
  "cardholderNameCiphertext": "ZW5jcnlwdGVkLWpvaG4tZG9l"
}
```

Response (200): same object echoed back.

curl:

```bash
curl -X POST http://localhost:8084/api/card-details -H "Content-Type: application/json" -d '{
  "instrumentId": "instr-card-0099",
  "panCiphertext": "b64:ZmFrZS1jaXBoZXJ0ZXh0LWZvci00MTExMTExMTExMTExMTEx",
  "dekId": "dek-0001",
  "nonce": "bm9uY2UtMTIzNDU2Nzg=",
  "aad": "aW5zdHItY2FyZC0wMDk5",
  "expMonth": 9,
  "expYear": 2027,
  "last4": "1111",
  "bin": "411111",
  "brand": "VISA",
  "funding": "CREDIT",
  "issuerCountry": "US",
  "productCode": "CLASSIC",
  "cardholderNameCiphertext": "ZW5jcnlwdGVkLWpvaG4tZG9l"
}'
```

### GET /api/card-details/{id}

curl:

```bash
curl -X GET http://localhost:8084/api/card-details/instr-card-0001
```

### PUT /api/card-details/{id}

Request body: full `CardDetail` object as above, with `instrumentId` matching the path.

curl:

```bash
curl -X PUT http://localhost:8084/api/card-details/instr-card-0001 -H "Content-Type: application/json" -d '{
  "instrumentId": "instr-card-0001",
  "panCiphertext": "b64:QUJDREVGR0hJSktMTU5PUA==",
  "dekId": "dek-0001",
  "nonce": "MTIzNDU2Nzg5MDEy",
  "aad": "aW5zdHItY2FyZC0wMDAx",
  "expMonth": 12,
  "expYear": 2029,
  "last4": "1111",
  "bin": "411111",
  "brand": "VISA",
  "funding": "CREDIT",
  "issuerCountry": "US",
  "productCode": "PLATINUM",
  "cardholderNameCiphertext": "ZW5jcnlwdGVkLW5hbWUtYnl0ZXM="
}'
```

### DELETE /api/card-details/{id}

curl:

```bash
curl -X DELETE http://localhost:8084/api/card-details/instr-card-0001
```

---

## BankDetailController — `/api/bank-details`

Entity: `BankDetail` (id: `instrumentId` String, caller-supplied). `accountCiphertext`/`routingCiphertext` are
Base64-encoded encrypted bytes; raw account/routing numbers should never be sent in the clear.

### GET /api/bank-details

Response (200):

```json
[
  {
    "instrumentId": "instr-bank-0001",
    "accountCiphertext": "ZW5jLWFjY291bnQtMDAwMQ==",
    "routingCiphertext": "ZW5jLXJvdXRpbmctMDAwMQ==",
    "dekId": "dek-0002",
    "nonce": "bm9uY2UtYmFuay0wMDAx",
    "last4": "6789",
    "bankName": "First National Bank",
    "country": "US"
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/bank-details
```

### POST /api/bank-details

Request body:

```json
{
  "instrumentId": "instr-bank-0099",
  "accountCiphertext": "ZW5jLWFjY291bnQtMDA5OQ==",
  "routingCiphertext": "ZW5jLXJvdXRpbmctMDA5OQ==",
  "dekId": "dek-0002",
  "nonce": "bm9uY2UtYmFuay0wMDk5",
  "last4": "4321",
  "bankName": "Second Federal Credit Union",
  "country": "US"
}
```

Response (200): same object echoed.

curl:

```bash
curl -X POST http://localhost:8084/api/bank-details -H "Content-Type: application/json" -d '{
  "instrumentId": "instr-bank-0099",
  "accountCiphertext": "ZW5jLWFjY291bnQtMDA5OQ==",
  "routingCiphertext": "ZW5jLXJvdXRpbmctMDA5OQ==",
  "dekId": "dek-0002",
  "nonce": "bm9uY2UtYmFuay0wMDk5",
  "last4": "4321",
  "bankName": "Second Federal Credit Union",
  "country": "US"
}'
```

### GET /api/bank-details/{id}

curl:

```bash
curl -X GET http://localhost:8084/api/bank-details/instr-bank-0001
```

### PUT /api/bank-details/{id}

curl:

```bash
curl -X PUT http://localhost:8084/api/bank-details/instr-bank-0001 -H "Content-Type: application/json" -d '{
  "instrumentId": "instr-bank-0001",
  "accountCiphertext": "ZW5jLWFjY291bnQtMDAwMQ==",
  "routingCiphertext": "ZW5jLXJvdXRpbmctMDAwMQ==",
  "dekId": "dek-0002",
  "nonce": "bm9uY2UtYmFuay0wMDAx",
  "last4": "6789",
  "bankName": "First National Bank Updated",
  "country": "US"
}'
```

### DELETE /api/bank-details/{id}

curl:

```bash
curl -X DELETE http://localhost:8084/api/bank-details/instr-bank-0001
```

---

## InstrumentController — `/api/instruments`

Entity: `Instrument` (id: `String`, caller-supplied). Represents the top-level "payment instrument" record that
`CardDetail`/`BankDetail`/`NetworkToken` rows reference via `instrumentId`.

### GET /api/instruments

Response (200):

```json
[
  {
    "id": "instr-card-0001",
    "token": "tok_9f1c2e3b4a5d6f70",
    "kind": "CARD",
    "scopeMerchantId": "merchant-0001",
    "fingerprint": "ZmluZ2VycHJpbnQtMDAwMQ==",
    "createdAt": "2026-01-10T09:00:00Z",
    "deletedAt": null
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/instruments
```

### POST /api/instruments

Request body:

```json
{
  "id": "instr-card-0099",
  "token": "tok_deadbeefcafef00d",
  "kind": "CARD",
  "scopeMerchantId": "merchant-0001",
  "fingerprint": "ZmluZ2VycHJpbnQtMDA5OQ==",
  "createdAt": "2026-07-25T12:00:00Z",
  "deletedAt": null
}
```

Response (200): same object echoed.

curl:

```bash
curl -X POST http://localhost:8084/api/instruments -H "Content-Type: application/json" -d '{
  "id": "instr-card-0099",
  "token": "tok_deadbeefcafef00d",
  "kind": "CARD",
  "scopeMerchantId": "merchant-0001",
  "fingerprint": "ZmluZ2VycHJpbnQtMDA5OQ==",
  "createdAt": "2026-07-25T12:00:00Z",
  "deletedAt": null
}'
```

### GET /api/instruments/{id}

curl:

```bash
curl -X GET http://localhost:8084/api/instruments/instr-card-0001
```

### PUT /api/instruments/{id}

curl:

```bash
curl -X PUT http://localhost:8084/api/instruments/instr-card-0001 -H "Content-Type: application/json" -d '{
  "id": "instr-card-0001",
  "token": "tok_9f1c2e3b4a5d6f70",
  "kind": "CARD",
  "scopeMerchantId": "merchant-0001",
  "fingerprint": "ZmluZ2VycHJpbnQtMDAwMQ==",
  "createdAt": "2026-01-10T09:00:00Z",
  "deletedAt": null
}'
```

### DELETE /api/instruments/{id}

curl:

```bash
curl -X DELETE http://localhost:8084/api/instruments/instr-card-0001
```

---

## NetworkTokenController — `/api/network-tokens`

Entity: `NetworkToken` (id: `String`, caller-supplied). `tokenCiphertext` is the Base64-encoded encrypted network
token (e.g. a Visa/Mastercard network token PAN substitute).

### GET /api/network-tokens

Response (200):

```json
[
  {
    "id": "ntok-0001",
    "instrumentId": "instr-card-0001",
    "network": "VISA",
    "tokenCiphertext": "bmV0d29yay10b2tlbi1jaXBoZXJ0ZXh0LTAwMDE=",
    "tar": "TAR1234567890",
    "expMonth": 12,
    "expYear": 2028,
    "status": "ACTIVE",
    "provisionedAt": "2026-01-10T09:05:00Z",
    "lastUpdatedAt": "2026-01-10T09:05:00Z"
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/network-tokens
```

### POST /api/network-tokens

Request body:

```json
{
  "id": "ntok-0099",
  "instrumentId": "instr-card-0099",
  "network": "VISA",
  "tokenCiphertext": "bmV0d29yay10b2tlbi1jaXBoZXJ0ZXh0LTAwOTk=",
  "tar": "TAR0000000099",
  "expMonth": 9,
  "expYear": 2027,
  "status": "ACTIVE",
  "provisionedAt": "2026-07-25T12:00:00Z",
  "lastUpdatedAt": "2026-07-25T12:00:00Z"
}
```

Response (200): same object echoed.

curl:

```bash
curl -X POST http://localhost:8084/api/network-tokens -H "Content-Type: application/json" -d '{
  "id": "ntok-0099",
  "instrumentId": "instr-card-0099",
  "network": "VISA",
  "tokenCiphertext": "bmV0d29yay10b2tlbi1jaXBoZXJ0ZXh0LTAwOTk=",
  "tar": "TAR0000000099",
  "expMonth": 9,
  "expYear": 2027,
  "status": "ACTIVE",
  "provisionedAt": "2026-07-25T12:00:00Z",
  "lastUpdatedAt": "2026-07-25T12:00:00Z"
}'
```

### GET /api/network-tokens/{id}

curl:

```bash
curl -X GET http://localhost:8084/api/network-tokens/ntok-0001
```

### PUT /api/network-tokens/{id}

curl:

```bash
curl -X PUT http://localhost:8084/api/network-tokens/ntok-0001 -H "Content-Type: application/json" -d '{
  "id": "ntok-0001",
  "instrumentId": "instr-card-0001",
  "network": "VISA",
  "tokenCiphertext": "bmV0d29yay10b2tlbi1jaXBoZXJ0ZXh0LTAwMDE=",
  "tar": "TAR1234567890",
  "expMonth": 12,
  "expYear": 2029,
  "status": "SUSPENDED",
  "provisionedAt": "2026-01-10T09:05:00Z",
  "lastUpdatedAt": "2026-07-25T12:10:00Z"
}'
```

### DELETE /api/network-tokens/{id}

curl:

```bash
curl -X DELETE http://localhost:8084/api/network-tokens/ntok-0001
```

---

## DekRegistryController — `/api/dek-registry`

Entity: `DekRegistry` (id: `String`, caller-supplied). Tracks wrapped Data Encryption Keys (DEKs) used to encrypt
the ciphertext columns above; `wrappedDek` is Base64-encoded key material wrapped by a KEK — never a plaintext key.

### GET /api/dek-registry

Response (200):

```json
[
  {
    "id": "dek-0001",
    "kekId": "kek-master-0001",
    "wrappedDek": "d3JhcHBlZC1kZWstYnl0ZXMtMDAwMQ==",
    "createdAt": "2026-01-01T00:00:00Z",
    "rotatedAt": null
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/dek-registry
```

### POST /api/dek-registry

Request body:

```json
{
  "id": "dek-0099",
  "kekId": "kek-master-0001",
  "wrappedDek": "d3JhcHBlZC1kZWstYnl0ZXMtMDA5OQ==",
  "createdAt": "2026-07-25T12:00:00Z",
  "rotatedAt": null
}
```

Response (200): same object echoed.

curl:

```bash
curl -X POST http://localhost:8084/api/dek-registry -H "Content-Type: application/json" -d '{
  "id": "dek-0099",
  "kekId": "kek-master-0001",
  "wrappedDek": "d3JhcHBlZC1kZWstYnl0ZXMtMDA5OQ==",
  "createdAt": "2026-07-25T12:00:00Z",
  "rotatedAt": null
}'
```

### GET /api/dek-registry/{id}

curl:

```bash
curl -X GET http://localhost:8084/api/dek-registry/dek-0001
```

### PUT /api/dek-registry/{id}

curl:

```bash
curl -X PUT http://localhost:8084/api/dek-registry/dek-0001 -H "Content-Type: application/json" -d '{
  "id": "dek-0001",
  "kekId": "kek-master-0001",
  "wrappedDek": "d3JhcHBlZC1kZWstYnl0ZXMtMDAwMQ==",
  "createdAt": "2026-01-01T00:00:00Z",
  "rotatedAt": "2026-07-25T12:15:00Z"
}'
```

### DELETE /api/dek-registry/{id}

curl:

```bash
curl -X DELETE http://localhost:8084/api/dek-registry/dek-0001
```

---

## InstrumentAccessLogController — `/api/instrument-access-log`

Entity: `InstrumentAccessLog` (id: `Long`, caller-supplied — no `@GeneratedValue`). Audit trail of who accessed
a given instrument and why.

### GET /api/instrument-access-log

Response (200):

```json
[
  {
    "id": 1,
    "instrumentId": "instr-card-0001",
    "actor": "svc-payment-orchestrator",
    "purpose": "AUTHORIZATION",
    "correlationId": "corr-0001",
    "createdAt": "2026-01-10T09:10:00Z"
  }
]
```

curl:

```bash
curl -X GET http://localhost:8084/api/instrument-access-log
```

### POST /api/instrument-access-log

Request body:

```json
{
  "id": 99,
  "instrumentId": "instr-card-0099",
  "actor": "svc-payment-orchestrator",
  "purpose": "AUTHORIZATION",
  "correlationId": "corr-0099",
  "createdAt": "2026-07-25T12:00:00Z"
}
```

Response (200): same object echoed.

curl:

```bash
curl -X POST http://localhost:8084/api/instrument-access-log -H "Content-Type: application/json" -d '{
  "id": 99,
  "instrumentId": "instr-card-0099",
  "actor": "svc-payment-orchestrator",
  "purpose": "AUTHORIZATION",
  "correlationId": "corr-0099",
  "createdAt": "2026-07-25T12:00:00Z"
}'
```

### GET /api/instrument-access-log/{id}

curl:

```bash
curl -X GET http://localhost:8084/api/instrument-access-log/1
```

### PUT /api/instrument-access-log/{id}

curl:

```bash
curl -X PUT http://localhost:8084/api/instrument-access-log/1 -H "Content-Type: application/json" -d '{
  "id": 1,
  "instrumentId": "instr-card-0001",
  "actor": "svc-fraud-review",
  "purpose": "MANUAL_REVIEW",
  "correlationId": "corr-0001",
  "createdAt": "2026-01-10T09:10:00Z"
}'
```

### DELETE /api/instrument-access-log/{id}

curl:

```bash
curl -X DELETE http://localhost:8084/api/instrument-access-log/1
```

---

## Notes on local testing

- All endpoints are open (no `Authorization` header needed) — see "Fix pending" note below for why this is a gap
  worth calling out.
- IDs used in the examples above (`instr-card-0001`, `instr-bank-0001`, `dek-0001`, `ntok-0001`, `bin-ranges` id
  `1`, `instrument-access-log` id `1`) match the fixed rows inserted by `src/main/resources/data.sql`, so the
  `GET .../{id}` examples work out of the box against a freshly started local instance.
- This service currently has **no Flyway migrations** — see the note in the project README / commit history:
  `ddl-auto` was changed from `validate` to `update` purely so the service can boot locally against an empty
  database. This is a workaround, not a substitute for real schema migrations.
