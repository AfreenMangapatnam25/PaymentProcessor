# Authorization Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None strictly required to start or call this service directly
— `JwtClaimsFilter` decodes whatever bearer token you send without calling out to
another service. However, tokens are normally *issued* by `authentication-service`,
so run **authentication-service** first if you want to test with real, validly
signed JWTs rather than hand-crafted ones. This service is also called *by*
`payment-service`-adjacent flows and the gateway, not the other way around.

**Infrastructure:** Postgres (`authorizationservicedb`). Kafka for domain events
(optional for REST testing). Eureka client is configured (`localhost:8761`) but
registration failure won't block startup. Config Server is optional.

Base URL: http://localhost:8086

Auth model: RS256 JWT bearer tokens issued by `authentication-service`. Send
`Authorization: Bearer <accessToken>`. This service is now an OAuth2 resource server, so an
unauthenticated request is rejected with `401` at the filter chain — see
[Authentication](#authentication) below. `JwtClaimsFilter` still runs alongside it, decoding claims
`roles`, `scope` and `merchant_id` (per `security.jwt.*` config) and binding a domain identity to
the request context; per-endpoint checks in the service layer still raise
`UnauthenticatedException` / `AccessDeniedException` → `401`/`403`.
`PaymentAuthorizationController` additionally supports an optional `Idempotency-Key` header on
`POST /api/v1/authorizations` to dedupe retried authorize calls.

Seeded sample data (see `src/main/resources/db/seed/V3__seed_sample_data.sql`, local profile only —
`spring.profiles.default: local` in `application.yml` means this is the effective default profile)
provides 3 authorization records with fixed UUIDs `11111111-1111-1111-1111-111111111111`,
`...112`, `...113`, and 3 role assignments referencing the `MERCHANT_ADMIN` / `MERCHANT_VIEWER` /
`END_USER` roles that ship in `V2__seed_rbac.sql`. Sample ids are used directly below.

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
permit-all chain. `local` is this service's default profile (`spring.profiles.default: local`),
so the plain `curl` commands in this guide work as-is.
Never set it to `false` outside a developer machine or an ephemeral CI container.

**Always public** (no token, in either mode): `/actuator/health/**`, `/actuator/info`,
`/actuator/prometheus`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`.

**Alongside the pre-existing claims filter.** `JwtClaimsFilter` / `IdentityContext` are unchanged and
still run under both chains: the filter decodes the bearer token (claims `roles`, `scope`,
`merchant_id`, per `security.jwt.*`) and binds a domain identity to the request, and the service layer
still raises `UnauthenticatedException` / `AccessDeniedException` per endpoint. It never
short-circuits, so it cannot conflict with the resource-server chain — the resource server is now the
gate that actually rejects an unauthenticated request with `401`.

**The same call, both ways:**

```bash
# with the `local` profile (security.jwt.enabled=false) — works as written
curl http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111111

# with the toggle on (the default) — token required
curl http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111111 \
  -H "Authorization: Bearer $TOKEN"
```

---

## AccessControlController — `/api/v1/access`

### POST /api/v1/access/check

Auth: Bearer JWT (or pass `identityId` explicitly to check on behalf of another identity).

Request body:

```json
{
  "identityId": "11111111-1111-1111-1111-111111111112",
  "resource": "authorization",
  "action": "capture",
  "scope": "MERCHANT",
  "scopeId": "merch_7788",
  "resourceOwnerId": "merch_7788",
  "requiredScopes": [
    "authorization:capture"
  ],
  "attributes": {
    "subject.kyc_status": "VERIFIED",
    "resource.amount": 250.00,
    "environment.device_trust_level": 0.9
  }
}
```

Response (`200 OK`):

```json
{
  "decision": "ALLOW",
  "reasonCode": "ROLE_GRANT",
  "reason": "Identity has role MERCHANT_ADMIN granting authorization:capture at scope MERCHANT/merch_7788",
  "identityId": "11111111-1111-1111-1111-111111111112",
  "resource": "authorization",
  "action": "capture",
  "evaluatedPolicies": [
    "high-value-transaction-guard"
  ],
  "evaluatedAt": "2026-07-25T14:45:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8086/api/v1/access/check \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "identityId": "11111111-1111-1111-1111-111111111112",
    "resource": "authorization",
    "action": "capture",
    "scope": "MERCHANT",
    "scopeId": "merch_7788",
    "requiredScopes": ["authorization:capture"]
  }'
```

### POST /api/v1/access/scopes/validate

Auth: Bearer JWT.

Request body:

```json
{
  "grantedScopes": [
    "authorization:read",
    "authorization:capture",
    "transaction:read"
  ],
  "requiredScopes": [
    "authorization:capture"
  ]
}
```

Response (`200 OK`):

```json
{
  "valid": true
}
```

curl:

```bash
curl -X POST http://localhost:8086/api/v1/access/scopes/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "grantedScopes": ["authorization:read", "authorization:capture"], "requiredScopes": ["authorization:capture"] }'
```

### GET /api/v1/access/identities/{identityId}/permissions

Auth: Bearer JWT.

Response (`200 OK`):

```json
{
  "identityId": "11111111-1111-1111-1111-111111111112",
  "permissions": [
    "transaction:read",
    "transaction:create",
    "transaction:refund",
    "authorization:read",
    "authorization:create",
    "authorization:capture",
    "authorization:reverse",
    "merchant:admin"
  ]
}
```

curl:

```bash
curl http://localhost:8086/api/v1/access/identities/11111111-1111-1111-1111-111111111112/permissions \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## PaymentAuthorizationController — `/api/v1/authorizations`

### POST /api/v1/authorizations

Auth: Bearer JWT.
Optional header: `Idempotency-Key: <client-generated key>` — replays with the same key + payload
hash return the original result instead of creating a duplicate.

Request body:

```json
{
  "paymentReference": "pay_20260725_0001",
  "merchantId": "merch_7788",
  "customerId": "cust_4455",
  "amount": 250.00,
  "currency": "USD",
  "paymentMethodToken": "tok_4242424242424242",
  "cardBin": "424242",
  "cardLast4": "4242",
  "cardExpMonth": 12,
  "cardExpYear": 2029,
  "captureImmediately": false,
  "statementDescriptor": "PAYPROC*DEMO",
  "riskScore": 0.12,
  "metadata": {
    "orderId": "ord_9981"
  }
}
```

Response (`201 Created`):

```json
{
  "authorizationId": "11111111-1111-1111-1111-111111111114",
  "status": "APPROVED",
  "type": "INITIAL",
  "paymentReference": "pay_20260725_0001",
  "merchantId": "merch_7788",
  "customerId": "cust_4455",
  "requestedAmount": 250.00,
  "approvedAmount": 250.00,
  "capturedAmount": null,
  "currency": "USD",
  "authorizationCode": "OK7788",
  "networkReferenceId": "24012345678901234567890",
  "responseCode": "00",
  "gatewayResponseMessage": "Approved",
  "cardNetwork": "VISA",
  "cardLast4": "4242",
  "avsResult": "FULL_MATCH",
  "cvvResult": "MATCH",
  "requiresAuthentication": false,
  "authenticationUrl": null,
  "originalAuthorizationId": null,
  "reauthorizationCount": 0,
  "expiresAt": "2026-08-01T14:50:00Z",
  "authorizedAt": "2026-07-25T14:50:00Z",
  "capturedAt": null,
  "reversedAt": null,
  "createdAt": "2026-07-25T14:50:00Z",
  "updatedAt": "2026-07-25T14:50:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8086/api/v1/authorizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Idempotency-Key: idem_7a8b9c0d1e2f3041" \
  -d '{
    "paymentReference": "pay_20260725_0001",
    "merchantId": "merch_7788",
    "customerId": "cust_4455",
    "amount": 250.00,
    "currency": "USD",
    "paymentMethodToken": "tok_4242424242424242",
    "cardBin": "424242",
    "cardLast4": "4242",
    "cardExpMonth": 12,
    "cardExpYear": 2029,
    "captureImmediately": false
  }'
```

### GET /api/v1/authorizations/{id}

Auth: Bearer JWT.

Response (`200 OK`): `AuthorizationResponse`, same shape as above. Seeded id
`11111111-1111-1111-1111-111111111111` is `APPROVED`, `...112` is `CAPTURED`, `...113` is
`DECLINED`.

curl:

```bash
curl http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111111 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### GET /api/v1/authorizations

Auth: Bearer JWT.
Query param (required): `paymentReference`.

Response (`200 OK`): array of `AuthorizationResponse`.

curl:

```bash
curl "http://localhost:8086/api/v1/authorizations?paymentReference=pay_seed_0001" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### POST /api/v1/authorizations/{id}/capture

Auth: Bearer JWT. Body is optional — omit `amount` (or send `{}` / no body) to capture the full
approved amount.

Request body:

```json
{
  "amount": 100.00
}
```

Response (`200 OK`): `AuthorizationResponse` with `status: "PARTIALLY_CAPTURED"` or `"CAPTURED"`
and `capturedAmount`/`capturedAt` populated.

curl:

```bash
curl -X POST http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111111/capture \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "amount": 100.00 }'
```

### POST /api/v1/authorizations/{id}/reversal

Auth: Bearer JWT. Body optional.

Request body:

```json
{
  "reason": "customer_cancelled"
}
```

Response (`200 OK`): `AuthorizationResponse` with `status: "REVERSED"` and `reversedAt` populated.

curl:

```bash
curl -X POST http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111111/reversal \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "reason": "customer_cancelled" }'
```

### POST /api/v1/authorizations/{id}/reauthorize

Auth: Bearer JWT.

Request body:

```json
{
  "amount": 250.00,
  "paymentMethodToken": "tok_4242424242424242",
  "reason": "original_hold_expired"
}
```

Response (`201 Created`): new `AuthorizationResponse` with `type: "REAUTHORIZATION"` and
`originalAuthorizationId` set to the path `{id}`.

curl:

```bash
curl -X POST http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111113/reauthorize \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "amount": 250.00, "paymentMethodToken": "tok_4242424242424242", "reason": "original_hold_expired" }'
```

### POST /api/v1/authorizations/{id}/synchronize

Auth: Bearer JWT. No body — re-fetches state from the gateway (useful after a 3-D Secure redirect).

Response (`200 OK`): `AuthorizationResponse`.

curl:

```bash
curl -X POST http://localhost:8086/api/v1/authorizations/11111111-1111-1111-1111-111111111111/synchronize \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## RoleAdminController — `/api/v1/admin`

Auth: Bearer JWT (admin-only in practice; not separately enforced by a path matcher — see note at
top of this document).

### Permissions

#### POST /api/v1/admin/permissions

```json
{
  "name": "dispute:read",
  "resource": "dispute",
  "action": "read",
  "description": "View disputes"
}
```

Response (`201 Created`):

```json
{
  "id": "44444444-4444-4444-4444-444444444401",
  "name": "dispute:read",
  "resource": "dispute",
  "action": "read",
  "description": "View disputes"
}
```

```bash
curl -X POST http://localhost:8086/api/v1/admin/permissions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{ "name": "dispute:read", "resource": "dispute", "action": "read", "description": "View disputes" }'
```

#### GET /api/v1/admin/permissions

Response (`200 OK`): array of permission objects (see `V2__seed_rbac.sql` for the 11 baseline
permissions, e.g. `transaction:read`, `authorization:capture`, `merchant:admin`).

```bash
curl http://localhost:8086/api/v1/admin/permissions -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Roles

#### POST /api/v1/admin/roles

```json
{
  "name": "SUPPORT_AGENT",
  "description": "Read-only support staff",
  "category": "PLATFORM",
  "permissions": [
    "transaction:read",
    "authorization:read"
  ],
  "parentRole": null,
  "systemRole": false
}
```

Response (`201 Created`):

```json
{
  "id": "55555555-5555-5555-5555-555555555501",
  "name": "SUPPORT_AGENT",
  "description": "Read-only support staff",
  "category": "PLATFORM",
  "permissions": [
    "transaction:read",
    "authorization:read"
  ],
  "parentRole": null,
  "systemRole": false
}
```

```bash
curl -X POST http://localhost:8086/api/v1/admin/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{ "name": "SUPPORT_AGENT", "description": "Read-only support staff", "category": "PLATFORM", "permissions": ["transaction:read", "authorization:read"], "systemRole": false }'
```

#### GET /api/v1/admin/roles

Response (`200 OK`): array of role objects, e.g. the seeded `MERCHANT_ADMIN`, `MERCHANT_VIEWER`,
`END_USER`, `SUPER_ADMIN` roles from `V2__seed_rbac.sql`.

```bash
curl http://localhost:8086/api/v1/admin/roles -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### GET /api/v1/admin/roles/{name}

```bash
curl http://localhost:8086/api/v1/admin/roles/MERCHANT_ADMIN -H "Authorization: Bearer $ADMIN_TOKEN"
```

Response (`200 OK`):

```json
{
  "id": "...",
  "name": "MERCHANT_ADMIN",
  "description": "Merchant administrator",
  "category": "MERCHANT",
  "permissions": [
    "transaction:read",
    "transaction:create",
    "transaction:refund",
    "authorization:read",
    "authorization:create",
    "authorization:capture",
    "authorization:reverse",
    "merchant:admin"
  ],
  "parentRole": null,
  "systemRole": true
}
```

#### POST /api/v1/admin/roles/{name}/permissions/{permission}

Grants a permission to a role. Response (`200 OK`): updated `RoleResponse`.

```bash
curl -X POST http://localhost:8086/api/v1/admin/roles/SUPPORT_AGENT/permissions/transaction:refund \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### DELETE /api/v1/admin/roles/{name}/permissions/{permission}

Revokes a permission from a role. Response (`200 OK`): updated `RoleResponse`.

```bash
curl -X DELETE http://localhost:8086/api/v1/admin/roles/SUPPORT_AGENT/permissions/transaction:refund \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### DELETE /api/v1/admin/roles/{name}

Deletes a non-system role. Response: `204 No Content`.

```bash
curl -X DELETE http://localhost:8086/api/v1/admin/roles/SUPPORT_AGENT \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Assignments

#### POST /api/v1/admin/assignments

```json
{
  "identityId": "11111111-1111-1111-1111-111111111112",
  "roleName": "MERCHANT_ADMIN",
  "scope": "MERCHANT",
  "scopeId": "merch_7788",
  "validFrom": "2026-07-25T00:00:00Z",
  "validUntil": null
}
```

Response (`201 Created`):

```json
{
  "id": "66666666-6666-6666-6666-666666666601",
  "identityId": "11111111-1111-1111-1111-111111111112",
  "roleName": "MERCHANT_ADMIN",
  "scope": "MERCHANT",
  "scopeId": "merch_7788",
  "validFrom": "2026-07-25T00:00:00Z",
  "validUntil": null
}
```

```bash
curl -X POST http://localhost:8086/api/v1/admin/assignments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{ "identityId": "11111111-1111-1111-1111-111111111112", "roleName": "MERCHANT_ADMIN", "scope": "MERCHANT", "scopeId": "merch_7788" }'
```

#### GET /api/v1/admin/identities/{identityId}/assignments

Response (`200 OK`): array of assignment objects, same shape as above. Seed data gives identity
`11111111-1111-1111-1111-111111111112` a `MERCHANT_ADMIN` assignment scoped to `merch_7788`.

```bash
curl http://localhost:8086/api/v1/admin/identities/11111111-1111-1111-1111-111111111112/assignments \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### DELETE /api/v1/admin/identities/{identityId}/roles/{roleName}

Response: `204 No Content`.

```bash
curl -X DELETE http://localhost:8086/api/v1/admin/identities/11111111-1111-1111-1111-111111111112/roles/MERCHANT_ADMIN \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Policies

#### POST /api/v1/admin/policies

```json
{
  "name": "block-high-risk-refund",
  "type": "ABAC",
  "effect": "DENY",
  "resource": "transaction",
  "action": "refund",
  "conditionJson": "[{\"attribute\":\"resource.amount\",\"operator\":\"GTE\",\"value\":10000}]",
  "priority": 10,
  "enabled": true
}
```

Response (`201 Created`):

```json
{
  "id": "77777777-7777-7777-7777-777777777701",
  "name": "block-high-risk-refund",
  "type": "ABAC",
  "effect": "DENY",
  "resource": "transaction",
  "action": "refund",
  "conditionJson": "[{\"attribute\":\"resource.amount\",\"operator\":\"GTE\",\"value\":10000}]",
  "priority": 10,
  "enabled": true
}
```

```bash
curl -X POST http://localhost:8086/api/v1/admin/policies \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{ "name": "block-high-risk-refund", "type": "ABAC", "effect": "DENY", "resource": "transaction", "action": "refund", "conditionJson": "[{\"attribute\":\"resource.amount\",\"operator\":\"GTE\",\"value\":10000}]", "priority": 10, "enabled": true }'
```

#### PUT /api/v1/admin/policies/{id}

Same body shape as create. Response (`200 OK`): updated `PolicyResponse`.

```bash
curl -X PUT http://localhost:8086/api/v1/admin/policies/77777777-7777-7777-7777-777777777701 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{ "name": "block-high-risk-refund", "type": "ABAC", "effect": "DENY", "resource": "transaction", "action": "refund", "conditionJson": "[{\"attribute\":\"resource.amount\",\"operator\":\"GTE\",\"value\":5000}]", "priority": 5, "enabled": true }'
```

#### GET /api/v1/admin/policies

Response (`200 OK`): array of policy objects, including the seeded `high-value-transaction-guard`
policy from `V2__seed_rbac.sql`.

```bash
curl http://localhost:8086/api/v1/admin/policies -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### DELETE /api/v1/admin/policies/{id}

Response: `204 No Content`.

```bash
curl -X DELETE http://localhost:8086/api/v1/admin/policies/77777777-7777-7777-7777-777777777701 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Error format

```json
{
  "timestamp": "2026-07-25T14:55:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Authorization 99999999-9999-9999-9999-999999999999 not found",
  "path": "/api/v1/authorizations/99999999-9999-9999-9999-999999999999",
  "violations": null
}
```
