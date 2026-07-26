# user-service API Testing Guide

## Dependencies — what to run before this service

**Other services:** **authentication-service** — required in practice, not at boot.
`user-service` validates JWTs as an OAuth2 resource server against
`OAUTH2_ISSUER_URI`/`OAUTH2_JWK_SET_URI` (default
`https://auth.paymentprocessor.local/`, not resolvable locally). It will still
*start* without authentication-service running, but every endpoint except
`/actuator/**` and the OpenAPI/swagger paths will reject requests until you either
point those env vars at a running authentication-service instance
(`http://localhost:8081/.well-known/jwks.json`) or otherwise supply a valid JWT.

**Infrastructure:** Postgres (`userservicedb`). Kafka for the outbox relay
(optional for direct REST testing). Config Server is optional.

Base URL: `http://localhost:8082` (see `server.port` / `SERVER_PORT` in `application.yml`)

## Authentication

`infrastructure/security/SecurityConfig.java` wires this service as a stateless JWT resource server
(`spring.security.oauth2.resourceserver.jwt`). Every endpoint requires `Authorization: Bearer <jwt>`
**except**:

- `/actuator/health`, `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`
- `/v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`

Merchant context (`merchant_id`) and identity (`sub`) are resolved **server-side from JWT claims**
(`JwtUserContext`, configured via `app.security.jwt.*`) — they are never trusted from the request body,
query params, or headers on the merchant-facing endpoints (`CustomerController`, `AddressController` for
`CUSTOMER`-owned resources, `ConsentController` for `CUSTOMER`-subject consents). Scope claims (`scope`)
are mapped to Spring Security authorities with the `SCOPE_` prefix, enabling `@PreAuthorize` checks (used
by `InternalCustomerController`).

### How to get a real token locally

Option (a) — real issuer, real token (recommended):

1. Point this service at authentication-service's JWKS endpoint:
   ```bash
   export OAUTH2_ISSUER_URI=http://localhost:8081
   export OAUTH2_JWK_SET_URI=http://localhost:8081/.well-known/jwks.json
   ```
   (authentication-service listens on port 8081 — see its `server.port`.)
2. Log in against authentication-service to obtain a bearer token:
   ```bash
   curl -X POST http://localhost:8081/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "test.user@example.com", "password": "Passw0rd!23"}'
   ```
   This returns a `TokenResponse` (`accessToken`, `tokenType`, `expiresIn`, `refreshToken`, `sessionId`).
   Use `accessToken` as the bearer token below. Note the token's claims must include the `merchant_id`
   and `scope` claims this service expects (`app.security.jwt.merchant-id-claim` / `authorities-claim`)
   for merchant-scoped/internal endpoints to authorize correctly — that mapping is owned by
   authentication-service/authorization-service, not this service.

Option (b) — dev-only shortcut (do NOT ship this): temporarily relax `SecurityConfig` to permit all
requests (e.g. widen `PUBLIC_PATHS` to `/**` or drop the resource-server filter) so you can hit endpoints
without a token while iterating locally. This is purely a local convenience and must never be committed
or run outside a developer's own machine.

---

## Local seed data

`db/seed/V8__seed_sample_data.sql` (applied only under the `local` Spring profile, since
`spring.flyway.locations` there is `classpath:db/migration,classpath:db/seed`) inserts one fully-linked
sample record set with these fixed ids, reused in the GET examples below:

| Entity   | id                                                                      |
|----------|-------------------------------------------------------------------------|
| User     | `usr_seed_0000000001`                                                   |
| Customer | `cus_seed_0000000001` (merchant: `mer_seed_0000000001`)                 |
| Address  | `adr_seed_0000000001` (owner: the user above)                           |
| Consent  | `con_seed_0000000001` (subject: the user above, kind `DATA_PROCESSING`) |

Note: the seed inserts placeholder (non-decryptable) bytes into the `*_encrypted` columns — see the
comment at the top of `V8__seed_sample_data.sql`. `GET` calls against the seeded ids will 200 with
correct ids/status/metadata, but decrypted PII fields (email, name, etc.) will not come back as
meaningful plaintext from that specific seed path. To see full round-trip plaintext, create data through
the API (`POST /api/v1/users`, etc.) so the service's own `EncryptionService` produces the ciphertext.

All responses are wrapped in the generic envelope:

```json
{
  "data": {
    ...
  },
  "meta": {
    "timestamp": "...",
    "requestId": "...",
    "correlationId": "...",
    "status": 200,
    "message": null
  }
}
```

List endpoints wrap their array in `PageResponse`:
`{ "content": [...], "totalElements": 1, "totalPages": 1, "page": 0, "size": 50, "first": true, "last": true }`.

---

## UserController — `/api/v1/users`

### POST /api/v1/users

Auth: `Authorization: Bearer <token>`

Request body:

```json
{
  "identityId": "idn_seed_0000000001",
  "email": "jane.doe@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-14",
  "phone": "+14155550100",
  "locale": "en_US",
  "timezone": "America/New_York"
}
```

Response (201):

```json
{
  "data": {
    "id": "usr_01J8ZK7QW3N5T6V8X9Y0Z1A2B3",
    "identityId": "idn_seed_0000000001",
    "status": "ACTIVE",
    "profile": {
      "email": "jane.doe@example.com",
      "firstName": "Jane",
      "lastName": "Doe",
      "dateOfBirth": "1990-05-14",
      "phone": "+14155550100",
      "locale": "en_US",
      "timezone": "America/New_York"
    },
    "version": 0,
    "createdAt": "2026-07-25T12:00:00Z",
    "updatedAt": "2026-07-25T12:00:00Z",
    "erasedAt": null
  },
  "meta": {
    "timestamp": "2026-07-25T12:00:00Z",
    "requestId": "req-1",
    "correlationId": "corr-1",
    "status": 201,
    "message": null
  }
}
```

curl:

```bash
curl -X POST http://localhost:8082/api/v1/users -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "identityId": "idn_seed_0000000001",
  "email": "jane.doe@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-14",
  "phone": "+14155550100",
  "locale": "en_US",
  "timezone": "America/New_York"
}'
```

### GET /api/v1/users/{userId}

Auth: `Authorization: Bearer <token>`

Response (200): same shape as the POST response `data`, for the seeded id.

curl:

```bash
curl -X GET http://localhost:8082/api/v1/users/usr_seed_0000000001 -H "Authorization: Bearer <token>"
```

### PATCH /api/v1/users/{userId}/status

Auth: `Authorization: Bearer <token>`

Request body (`expectedVersion` is the optimistic-lock version last read by the client):

```json
{
  "action": "SUSPEND",
  "expectedVersion": 0
}
```

`action` is one of `ACTIVATE`, `SUSPEND`, `LOCK`.

Response (200): `UserResponse` with `status: "SUSPENDED"` and `version` incremented.

curl:

```bash
curl -X PATCH http://localhost:8082/api/v1/users/usr_seed_0000000001/status -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "action": "SUSPEND",
  "expectedVersion": 0
}'
```

### DELETE /api/v1/users/{userId}

Auth: `Authorization: Bearer <token>`

GDPR crypto-shred erasure — destroys the user's DEK (`crypto_keys` row), rendering their PII
ciphertext unrecoverable; the row itself is retained with `status: "ERASED"` and `erasedAt` set.

Query params: `expectedVersion` (required, `>= 0`).

Response (200): `UserResponse` with `status: "ERASED"`, `erasedAt` set, `profile: null`.

curl:

```bash
curl -X DELETE "http://localhost:8082/api/v1/users/usr_seed_0000000001?expectedVersion=0" -H "Authorization: Bearer <token>"
```

---

## UserProfileController — `/api/v1/users/{userId}/profile`

### GET /api/v1/users/{userId}/profile

Auth: `Authorization: Bearer <token>`

Response (200):

```json
{
  "data": {
    "email": "jane.doe@example.com",
    "firstName": "Jane",
    "lastName": "Doe",
    "dateOfBirth": "1990-05-14",
    "phone": "+14155550100",
    "locale": "en_US",
    "timezone": "America/New_York"
  },
  "meta": {
    "timestamp": "2026-07-25T12:00:00Z",
    "requestId": "req-2",
    "correlationId": "corr-2",
    "status": 200,
    "message": null
  }
}
```

`data` is `null` if the user has been erased.

curl:

```bash
curl -X GET http://localhost:8082/api/v1/users/usr_seed_0000000001/profile -H "Authorization: Bearer <token>"
```

### PUT /api/v1/users/{userId}/profile

Auth: `Authorization: Bearer <token>`

Full-replace semantics; requires `expectedVersion` from the current `UserResponse.version`.

Request body:

```json
{
  "email": "jane.doe+updated@example.com",
  "firstName": "Jane",
  "lastName": "Doe-Smith",
  "dateOfBirth": "1990-05-14",
  "phone": "+14155550199",
  "locale": "en_US",
  "timezone": "America/Los_Angeles",
  "expectedVersion": 0
}
```

Response (200): updated `UserProfileResponse`.

curl:

```bash
curl -X PUT http://localhost:8082/api/v1/users/usr_seed_0000000001/profile -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "email": "jane.doe+updated@example.com",
  "firstName": "Jane",
  "lastName": "Doe-Smith",
  "dateOfBirth": "1990-05-14",
  "phone": "+14155550199",
  "locale": "en_US",
  "timezone": "America/Los_Angeles",
  "expectedVersion": 0
}'
```

---

## CustomerController — `/api/v1/customers`

All operations are implicitly scoped to the caller's `merchant_id` JWT claim — you can never read or
write another merchant's customers through this controller. The seeded customer belongs to merchant
`mer_seed_0000000001`, so your test token must carry that `merchant_id` claim to see it.

### POST /api/v1/customers

Auth: `Authorization: Bearer <token>` (merchant_id claim required)

Request body:

```json
{
  "email": "buyer@example.com",
  "fullName": "Alex Buyer",
  "phone": "+14155550111",
  "externalRef": "shop-order-cust-42",
  "userId": null,
  "metadata": {
    "tier": "gold"
  }
}
```

Response (201):

```json
{
  "data": {
    "id": "cus_01J8ZKAB12CD34EF56GH78JK90",
    "merchantId": "mer_seed_0000000001",
    "userId": null,
    "externalRef": "shop-order-cust-42",
    "email": "buyer@example.com",
    "fullName": "Alex Buyer",
    "phone": "+14155550111",
    "defaultInstrumentToken": null,
    "status": "ACTIVE",
    "metadata": {
      "tier": "gold"
    },
    "version": 0,
    "createdAt": "2026-07-25T12:00:00Z",
    "updatedAt": "2026-07-25T12:00:00Z",
    "deletedAt": null,
    "erasedAt": null
  },
  "meta": {
    "timestamp": "2026-07-25T12:00:00Z",
    "requestId": "req-3",
    "correlationId": "corr-3",
    "status": 201,
    "message": null
  }
}
```

curl:

```bash
curl -X POST http://localhost:8082/api/v1/customers -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "email": "buyer@example.com",
  "fullName": "Alex Buyer",
  "phone": "+14155550111",
  "externalRef": "shop-order-cust-42",
  "userId": null,
  "metadata": { "tier": "gold" }
}'
```

### GET /api/v1/customers/{customerId}

Auth: `Authorization: Bearer <token>` (merchant_id claim required, must match the customer's merchant)

curl:

```bash
curl -X GET http://localhost:8082/api/v1/customers/cus_seed_0000000001 -H "Authorization: Bearer <token>"
```

### GET /api/v1/customers

Auth: `Authorization: Bearer <token>` (merchant_id claim required)

Query params: `page` (default 0), `size` (default 50).

Response (200):

```json
{
  "data": {
    "content": [
      {
        "id": "cus_seed_0000000001",
        "merchantId": "mer_seed_0000000001",
        "...": "..."
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 50,
    "first": true,
    "last": true
  },
  "meta": {
    "timestamp": "2026-07-25T12:00:00Z",
    "requestId": "req-4",
    "correlationId": "corr-4",
    "status": 200,
    "message": null
  }
}
```

curl:

```bash
curl -X GET "http://localhost:8082/api/v1/customers?page=0&size=50" -H "Authorization: Bearer <token>"
```

### PUT /api/v1/customers/{customerId}

Auth: `Authorization: Bearer <token>` (merchant_id claim required)

Request body:

```json
{
  "email": "buyer.updated@example.com",
  "fullName": "Alex Buyer Jr.",
  "phone": "+14155550122",
  "metadata": {
    "tier": "platinum"
  },
  "expectedVersion": 0
}
```

curl:

```bash
curl -X PUT http://localhost:8082/api/v1/customers/cus_seed_0000000001 -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "email": "buyer.updated@example.com",
  "fullName": "Alex Buyer Jr.",
  "phone": "+14155550122",
  "metadata": { "tier": "platinum" },
  "expectedVersion": 0
}'
```

### DELETE /api/v1/customers/{customerId}

Auth: `Authorization: Bearer <token>` (merchant_id claim required)

Soft delete (`deleted_at` set; row retained). Query param: `expectedVersion` (required).

curl:

```bash
curl -X DELETE "http://localhost:8082/api/v1/customers/cus_seed_0000000001?expectedVersion=0" -H "Authorization: Bearer <token>"
```

### POST /api/v1/customers/{customerId}/erasure

Auth: `Authorization: Bearer <token>` (merchant_id claim required)

GDPR crypto-shred for a customer (destroys their DEK). Query param: `expectedVersion` (required).

curl:

```bash
curl -X POST "http://localhost:8082/api/v1/customers/cus_seed_0000000001/erasure?expectedVersion=0" -H "Authorization: Bearer <token>"
```

---

## InternalCustomerController — `/api/v1/internal/customers/{customerId}`

Service-to-service lookup (e.g. payment-service refreshing its read model). Requires both:

- a valid JWT with the `internal.customers.read` scope (
  `@PreAuthorize("hasAuthority('SCOPE_internal.customers.read')")`)
- the `X-Merchant-ID` header (merchant is passed explicitly by the calling service, not derived from
  end-user claims, since this is a machine-to-machine call)

### GET /api/v1/internal/customers/{customerId}

Auth: `Authorization: Bearer <token>` with `scope` claim containing `internal.customers.read`, plus header
`X-Merchant-ID: <merchantId>`

Response (200): `CustomerResponse` (same shape as `CustomerController` GET).

curl:

```bash
curl -X GET http://localhost:8082/api/v1/internal/customers/cus_seed_0000000001 \
  -H "Authorization: Bearer <service-token-with-internal.customers.read-scope>" \
  -H "X-Merchant-ID: mer_seed_0000000001"
```

---

## AddressController — `/api/v1/addresses`

Addresses belong to either a `USER` or a `CUSTOMER` (`ownerType` + `ownerId`). `CUSTOMER`-owned
addresses are authorized against the caller's merchant scope (the customer must resolve within the
caller's `merchant_id`); `USER`-owned addresses just require authentication.

### POST /api/v1/addresses

Auth: `Authorization: Bearer <token>`

Request body:

```json
{
  "ownerType": "USER",
  "ownerId": "usr_seed_0000000001",
  "addressType": "SHIPPING",
  "line1": "123 Market St",
  "line2": "Apt 4B",
  "city": "San Francisco",
  "region": "CA",
  "postalCode": "94103",
  "countryCode": "US",
  "defaultAddress": true
}
```

Response (201):

```json
{
  "data": {
    "id": "adr_01J8ZKC3D4E5F6G7H8J9K0M1N2",
    "ownerType": "USER",
    "ownerId": "usr_seed_0000000001",
    "addressType": "SHIPPING",
    "line1": "123 Market St",
    "line2": "Apt 4B",
    "city": "San Francisco",
    "region": "CA",
    "postalCode": "94103",
    "countryCode": "US",
    "defaultAddress": true,
    "version": 0,
    "createdAt": "2026-07-25T12:00:00Z",
    "updatedAt": "2026-07-25T12:00:00Z",
    "deletedAt": null
  },
  "meta": {
    "timestamp": "2026-07-25T12:00:00Z",
    "requestId": "req-5",
    "correlationId": "corr-5",
    "status": 201,
    "message": null
  }
}
```

curl:

```bash
curl -X POST http://localhost:8082/api/v1/addresses -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "ownerType": "USER",
  "ownerId": "usr_seed_0000000001",
  "addressType": "SHIPPING",
  "line1": "123 Market St",
  "line2": "Apt 4B",
  "city": "San Francisco",
  "region": "CA",
  "postalCode": "94103",
  "countryCode": "US",
  "defaultAddress": true
}'
```

### GET /api/v1/addresses

Auth: `Authorization: Bearer <token>`

Query params: `ownerType` (`USER`|`CUSTOMER`), `ownerId`.

curl:

```bash
curl -X GET "http://localhost:8082/api/v1/addresses?ownerType=USER&ownerId=usr_seed_0000000001" -H "Authorization: Bearer <token>"
```

### GET /api/v1/addresses/{addressId}

Auth: `Authorization: Bearer <token>`

Query params: `ownerType`, `ownerId` (must match the address's actual owner).

curl:

```bash
curl -X GET "http://localhost:8082/api/v1/addresses/adr_seed_0000000001?ownerType=USER&ownerId=usr_seed_0000000001" -H "Authorization: Bearer <token>"
```

### PUT /api/v1/addresses/{addressId}

Auth: `Authorization: Bearer <token>`

Query params: `ownerType`, `ownerId`. Request body:

```json
{
  "addressType": "SHIPPING",
  "line1": "456 Mission St",
  "line2": null,
  "city": "San Francisco",
  "region": "CA",
  "postalCode": "94105",
  "countryCode": "US",
  "defaultAddress": true,
  "expectedVersion": 0
}
```

curl:

```bash
curl -X PUT "http://localhost:8082/api/v1/addresses/adr_seed_0000000001?ownerType=USER&ownerId=usr_seed_0000000001" -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "addressType": "SHIPPING",
  "line1": "456 Mission St",
  "line2": null,
  "city": "San Francisco",
  "region": "CA",
  "postalCode": "94105",
  "countryCode": "US",
  "defaultAddress": true,
  "expectedVersion": 0
}'
```

### DELETE /api/v1/addresses/{addressId}

Auth: `Authorization: Bearer <token>`

Query params: `ownerType`, `ownerId`, `expectedVersion` (required, `>= 0`). Soft delete.

curl:

```bash
curl -X DELETE "http://localhost:8082/api/v1/addresses/adr_seed_0000000001?ownerType=USER&ownerId=usr_seed_0000000001&expectedVersion=0" -H "Authorization: Bearer <token>"
```

---

## ConsentController — `/api/v1/consents`

Consents apply to a `USER` or `CUSTOMER` subject (`subjectType` + `subjectId`); `CUSTOMER`-subject
consents are merchant-scope-checked the same way as customer-owned addresses.

### POST /api/v1/consents

Auth: `Authorization: Bearer <token>`

Grants (or re-grants) consent for a `(subjectType, subjectId, kind)`. Valid `kind` values:
`MARKETING`, `DATA_PROCESSING`, `THIRD_PARTY_SHARING`, `PROFILING`.

Request body:

```json
{
  "subjectType": "USER",
  "subjectId": "usr_seed_0000000001",
  "kind": "MARKETING",
  "source": "web",
  "policyVersion": "v1.2"
}
```

Response (201):

```json
{
  "data": {
    "id": "con_01J8ZKD5E6F7G8H9J0K1M2N3P4",
    "subjectType": "USER",
    "subjectId": "usr_seed_0000000001",
    "kind": "MARKETING",
    "granted": true,
    "source": "web",
    "policyVersion": "v1.2",
    "grantedAt": "2026-07-25T12:00:00Z",
    "revokedAt": null,
    "version": 0,
    "createdAt": "2026-07-25T12:00:00Z",
    "updatedAt": "2026-07-25T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-07-25T12:00:00Z",
    "requestId": "req-6",
    "correlationId": "corr-6",
    "status": 201,
    "message": null
  }
}
```

curl:

```bash
curl -X POST http://localhost:8082/api/v1/consents -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{
  "subjectType": "USER",
  "subjectId": "usr_seed_0000000001",
  "kind": "MARKETING",
  "source": "web",
  "policyVersion": "v1.2"
}'
```

### GET /api/v1/consents/{kind}

Auth: `Authorization: Bearer <token>`

Path variable `kind` is one of `MARKETING`/`DATA_PROCESSING`/`THIRD_PARTY_SHARING`/`PROFILING`.
Query params: `subjectType`, `subjectId`.

curl (using the seeded `DATA_PROCESSING` consent):

```bash
curl -X GET "http://localhost:8082/api/v1/consents/DATA_PROCESSING?subjectType=USER&subjectId=usr_seed_0000000001" -H "Authorization: Bearer <token>"
```

### GET /api/v1/consents

Auth: `Authorization: Bearer <token>`

Query params: `subjectType`, `subjectId`. Returns all consent-kind rows for the subject.

curl:

```bash
curl -X GET "http://localhost:8082/api/v1/consents?subjectType=USER&subjectId=usr_seed_0000000001" -H "Authorization: Bearer <token>"
```

### DELETE /api/v1/consents

Auth: `Authorization: Bearer <token>`

Revokes consent (sets `granted: false`, `revokedAt`). Query params: `subjectType`, `subjectId`, `kind`.

curl:

```bash
curl -X DELETE "http://localhost:8082/api/v1/consents?subjectType=USER&subjectId=usr_seed_0000000001&kind=DATA_PROCESSING" -H "Authorization: Bearer <token>"
```

---

## Skipped

`HealthController` is an empty stub with no mapped endpoints (dead code) — nothing to document. Actual
health checks are served by Spring Boot Actuator at `/actuator/health`.
