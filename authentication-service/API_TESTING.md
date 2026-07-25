# Authentication Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `authentication-service` makes no outbound calls to any
other service — it's a foundational identity/token-issuing service. Start this one
first if you plan to test other services (gateway-service, user-service,
authorization-service) that validate JWTs it issues.

**Infrastructure:** Postgres (`authenticationservicedb`). Kafka only matters if you
want to observe the domain events it publishes (login, logout, MFA, lockouts) —
not required to exercise the REST API. Config Server is optional.

Base URL: http://localhost:8081

Auth model: RS256 JWT bearer tokens issued by this service (`POST /api/v1/auth/login` /
`/login/mfa`). Send `Authorization: Bearer <accessToken>` on protected routes. `/api/v1/admin/**`
additionally requires the `ADMIN` role (`hasRole("ADMIN")`) — i.e. the token's roles claim must
contain `ADMIN`. Public (no token required) routes, per `SecurityConfig`:

- `POST /api/v1/auth/login`, `/login/mfa`, `/refresh`, `/logout`, `/token/introspect`
- `POST /api/v1/passwords/forgot`, `/reset`
- `POST /api/v1/verification/email/confirm`, `/verification/phone/confirm`
- `POST /api/v1/api-keys/verify`
- `GET /.well-known/jwks.json`, `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`

Everything else requires a valid bearer token.

Seeded sample data (see `src/main/resources/db/seed/V3__seed_sample_data.sql`, local profile only)
provides 5 identities with fixed UUIDs `11111111-1111-1111-1111-111111111111` … `...115`, used as
sample ids below:

- `...111` — ADMIN, ACTIVE, `admin@paymentprocessor.local`
- `...112` — USER, ACTIVE, `owner@merchant-demo.com`, has 2 devices + a TOTP MFA factor
- `...113` — USER, ACTIVE, `alice@example.com`
- `...114` — USER, LOCKED, `locked.user@example.com`
- `...115` — USER, PENDING (unverified email), `pending.user@example.com`

Note: seeded `credentials.secret_hash` values are placeholder Argon2id-shaped strings, not a hash
of a real usable password — you cannot log in against seeded identities out of the box. Use
`POST /api/v1/admin/identities` to register a fresh identity with a known password for login/MFA
flow testing, or reset the seeded row's hash yourself.

---

## AdminIdentityController — `/api/v1/admin/identities`

Auth: Bearer JWT with `ADMIN` role.

### POST /api/v1/admin/identities

Request body:

```json
{
  "principalType": "USER",
  "email": "new.user@example.com",
  "phoneE164": "+14155551234",
  "password": "Str0ng!Passw0rd123",
  "mfaRequired": false
}
```

Response (`201 Created`):

```json
{
  "id": "3f9a2b7e-1d4c-4a2e-9c3b-8a6f0d2e5c11",
  "principalType": "USER",
  "email": "new.user@example.com",
  "phoneE164": "+14155551234",
  "status": "PENDING",
  "mfaRequired": false,
  "emailVerified": false,
  "createdAt": "2026-07-25T14:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8081/api/v1/admin/identities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "principalType": "USER",
    "email": "new.user@example.com",
    "phoneE164": "+14155551234",
    "password": "Str0ng!Passw0rd123",
    "mfaRequired": false
  }'
```

### GET /api/v1/admin/identities/{id}

Response (`200 OK`):

```json
{
  "id": "11111111-1111-1111-1111-111111111112",
  "principalType": "USER",
  "email": "owner@merchant-demo.com",
  "phoneE164": "+14155559012",
  "status": "ACTIVE",
  "mfaRequired": true,
  "emailVerified": true,
  "createdAt": "2026-07-20T08:00:00Z"
}
```

curl:

```bash
curl http://localhost:8081/api/v1/admin/identities/11111111-1111-1111-1111-111111111112 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### POST /api/v1/admin/identities/{id}/lock

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/admin/identities/11111111-1111-1111-1111-111111111113/lock \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### POST /api/v1/admin/identities/{id}/unlock

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/admin/identities/11111111-1111-1111-1111-111111111114/unlock \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### POST /api/v1/admin/identities/{id}/disable

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/admin/identities/11111111-1111-1111-1111-111111111115/disable \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## ApiKeyController — `/api/v1/api-keys`

### POST /api/v1/api-keys

Auth: Bearer JWT (admin-only in practice; issue keys for services/merchants).

Request body:

```json
{
  "ownerType": "SERVICE",
  "ownerId": "fraud-service",
  "scopes": [
    "fraud:read",
    "fraud:score"
  ],
  "environment": "sandbox",
  "expiresAt": "2027-07-25T00:00:00Z"
}
```

Response (`201 Created`) — `secret` is shown only this once:

```json
{
  "key": {
    "id": "8b2c1a4e-6f3d-4e21-9a10-1c2d3e4f5061",
    "ownerType": "SERVICE",
    "ownerId": "fraud-service",
    "prefix": "pk_live_8b2c1a",
    "scopes": [
      "fraud:read",
      "fraud:score"
    ],
    "environment": "sandbox",
    "expiresAt": "2027-07-25T00:00:00Z",
    "revokedAt": null,
    "createdAt": "2026-07-25T14:05:00Z"
  },
  "secret": "sk_8b2c1a4e6f3d4e219a101c2d3e4f5061a1b2c3d4e5f60718293a4b5c6d7e8f9"
}
```

curl:

```bash
curl -X POST http://localhost:8081/api/v1/api-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "ownerType": "SERVICE",
    "ownerId": "fraud-service",
    "scopes": ["fraud:read", "fraud:score"],
    "environment": "sandbox",
    "expiresAt": "2027-07-25T00:00:00Z"
  }'
```

### GET /api/v1/api-keys

Auth: Bearer JWT.
Query params (both required): `ownerType`, `ownerId`.

Response (`200 OK`): array of key objects (same shape as `key` above, no `secret`).

curl:

```bash
curl "http://localhost:8081/api/v1/api-keys?ownerType=SERVICE&ownerId=fraud-service" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### DELETE /api/v1/api-keys/{id}

Auth: Bearer JWT. Response: `204 No Content`.

```bash
curl -X DELETE http://localhost:8081/api/v1/api-keys/8b2c1a4e-6f3d-4e21-9a10-1c2d3e4f5061 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### POST /api/v1/api-keys/verify

Auth: none (public service-to-service endpoint).

Request body:

```json
{
  "apiKey": "sk_8b2c1a4e6f3d4e219a101c2d3e4f5061a1b2c3d4e5f60718293a4b5c6d7e8f9"
}
```

Response (`200 OK`):

```json
{
  "valid": true,
  "keyId": "8b2c1a4e-6f3d-4e21-9a10-1c2d3e4f5061",
  "ownerType": "SERVICE",
  "ownerId": "fraud-service",
  "scopes": [
    "fraud:read",
    "fraud:score"
  ],
  "environment": "sandbox"
}
```

curl:

```bash
curl -X POST http://localhost:8081/api/v1/api-keys/verify \
  -H "Content-Type: application/json" \
  -d '{ "apiKey": "sk_8b2c1a4e6f3d4e219a101c2d3e4f5061a1b2c3d4e5f60718293a4b5c6d7e8f9" }'
```

---

## AuthController — `/api/v1/auth`

### POST /api/v1/auth/login

Auth: none.

Request body:

```json
{
  "email": "alice@example.com",
  "password": "correct horse battery staple 1!",
  "deviceFingerprint": "fp_c8b1e2a3d4f5",
  "deviceLabel": "Alice's MacBook Pro"
}
```

Response (`200 OK`, no MFA required):

```json
{
  "status": "AUTHENTICATED",
  "tokens": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImF1dGgta2V5LTEifQ.eyJzdWIiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTMiLCJyb2xlcyI6WyJVU0VSIl0sImV4cCI6MTc1MzQ2NzMwMH0.signature",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "refreshToken": "rtk_1a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2c3d4e5f6a7",
    "sessionId": "2b3c4d5e-6f70-4899-a0b1-c2d3e4f50617"
  },
  "challenge": null
}
```

Response (`200 OK`, MFA required):

```json
{
  "status": "MFA_REQUIRED",
  "tokens": null,
  "challenge": {
    "mfaToken": "mfa_9f8e7d6c5b4a39281706f5e4d3c2b1a0",
    "methods": [
      "TOTP"
    ]
  }
}
```

curl:

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "correct horse battery staple 1!",
    "deviceFingerprint": "fp_c8b1e2a3d4f5",
    "deviceLabel": "Alice'\''s MacBook Pro"
  }'
```

### POST /api/v1/auth/login/mfa

Auth: none (requires the `mfaToken` from the login challenge above).

Request body:

```json
{
  "mfaToken": "mfa_9f8e7d6c5b4a39281706f5e4d3c2b1a0",
  "code": "123456",
  "deviceFingerprint": "fp_c8b1e2a3d4f5",
  "deviceLabel": "Alice's MacBook Pro"
}
```

Response (`200 OK`): `TokenResponse`, same shape as the `tokens` object above.

curl:

```bash
curl -X POST http://localhost:8081/api/v1/auth/login/mfa \
  -H "Content-Type: application/json" \
  -d '{ "mfaToken": "mfa_9f8e7d6c5b4a39281706f5e4d3c2b1a0", "code": "123456" }'
```

### POST /api/v1/auth/refresh

Auth: none.

Request body:

```json
{
  "refreshToken": "rtk_1a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2c3d4e5f6a7"
}
```

Response (`200 OK`): `TokenResponse`, same shape as above (rotated refresh token).

curl:

```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "rtk_1a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2c3d4e5f6a7" }'
```

### POST /api/v1/auth/logout

Auth: none (token itself is the credential).

Request body:

```json
{
  "refreshToken": "rtk_1a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2c3d4e5f6a7"
}
```

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "rtk_1a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2c3d4e5f6a7" }'
```

### POST /api/v1/auth/logout-all

Auth: Bearer JWT. Revokes every refresh token for the caller's identity.

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/auth/logout-all \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### POST /api/v1/auth/token/introspect

Auth: none.

Request body:

```json
{
  "token": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImF1dGgta2V5LTEifQ..."
}
```

Response (`200 OK`, active token):

```json
{
  "active": true,
  "identityId": "11111111-1111-1111-1111-111111111113",
  "principalType": "USER",
  "scopes": [
    "profile:read"
  ],
  "expiresAt": 1753467300
}
```

Response (`200 OK`, inactive/expired token):
`{ "active": false, "identityId": null, "principalType": null, "scopes": null, "expiresAt": null }`

curl:

```bash
curl -X POST http://localhost:8081/api/v1/auth/token/introspect \
  -H "Content-Type: application/json" \
  -d '{ "token": "eyJhbGciOiJSUzI1NiIs..." }'
```

---

## DeviceController — `/api/v1/devices`

Auth: Bearer JWT (acts on the caller's own devices).

### GET /api/v1/devices

Response (`200 OK`):

```json
[
  {
    "id": "22222222-2222-2222-2222-222222222221",
    "label": "Alice's MacBook Pro",
    "fingerprint": "fp_c8b1e2a3d4f5",
    "trustLevel": "TRUSTED",
    "lastIp": "203.0.113.11",
    "lastSeenAt": "2026-07-25T09:00:00Z",
    "createdAt": "2026-06-01T09:00:00Z"
  }
]
```

curl:

```bash
curl http://localhost:8081/api/v1/devices \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### POST /api/v1/devices/{deviceId}/trust

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/devices/22222222-2222-2222-2222-222222222221/trust \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### DELETE /api/v1/devices/{deviceId}

Response: `204 No Content`.

```bash
curl -X DELETE http://localhost:8081/api/v1/devices/22222222-2222-2222-2222-222222222221 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## JwksController — `/.well-known/jwks.json`

### GET /.well-known/jwks.json

Auth: none.

Response (`200 OK`):

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "auth-key-1",
      "use": "sig",
      "alg": "RS256",
      "n": "xGOr-H7A-PWZ4jw...",
      "e": "AQAB"
    }
  ]
}
```

curl:

```bash
curl http://localhost:8081/.well-known/jwks.json
```

---

## MfaController — `/api/v1/mfa`

Auth: Bearer JWT for all routes.

### POST /api/v1/mfa/enroll

Query params: `label` (optional).

Response (`200 OK`):

```json
{
  "factorId": "33333333-3333-3333-3333-333333333331",
  "secret": "JBSWY3DPEHPK3PXP",
  "otpauthUri": "otpauth://totp/PaymentProcessor:owner@merchant-demo.com?secret=JBSWY3DPEHPK3PXP&issuer=PaymentProcessor"
}
```

curl:

```bash
curl -X POST "http://localhost:8081/api/v1/mfa/enroll?label=iPhone%2015" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### POST /api/v1/mfa/verify

Request body:

```json
{
  "factorId": "33333333-3333-3333-3333-333333333331",
  "code": "654321"
}
```

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/mfa/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "factorId": "33333333-3333-3333-3333-333333333331", "code": "654321" }'
```

### POST /api/v1/mfa/disable

Request body:

```json
{
  "factorId": "33333333-3333-3333-3333-333333333331",
  "currentPassword": "correct horse battery staple 1!"
}
```

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/mfa/disable \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "factorId": "33333333-3333-3333-3333-333333333331", "currentPassword": "correct horse battery staple 1!" }'
```

### POST /api/v1/mfa/recovery-codes

Response (`200 OK`):

```json
{
  "codes": [
    "a1b2-c3d4",
    "e5f6-a7b8",
    "c9d0-e1f2",
    "..."
  ]
}
```

```bash
curl -X POST http://localhost:8081/api/v1/mfa/recovery-codes \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### GET /api/v1/mfa

Response (`200 OK`):

```json
{
  "activeMethods": [
    "TOTP"
  ]
}
```

```bash
curl http://localhost:8081/api/v1/mfa \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## PasswordController — `/api/v1/passwords`

### POST /api/v1/passwords/change

Auth: Bearer JWT.

Request body:

```json
{
  "currentPassword": "correct horse battery staple 1!",
  "newPassword": "Even Str0nger Passw0rd 2!"
}
```

Response: `204 No Content`.

```bash
curl -X POST http://localhost:8081/api/v1/passwords/change \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "currentPassword": "correct horse battery staple 1!", "newPassword": "Even Str0nger Passw0rd 2!" }'
```

### POST /api/v1/passwords/forgot

Auth: none. Always returns the same message regardless of whether the email exists (anti-enumeration).

Request body:

```json
{
  "email": "alice@example.com"
}
```

Response (`200 OK`):

```json
{
  "message": "If the email exists, a reset link has been sent"
}
```

```bash
curl -X POST http://localhost:8081/api/v1/passwords/forgot \
  -H "Content-Type: application/json" \
  -d '{ "email": "alice@example.com" }'
```

### POST /api/v1/passwords/reset

Auth: none.

Request body:

```json
{
  "token": "prt_5f6e7d8c9b0a1928374655463728190a1b2c3d4e5f60718",
  "newPassword": "Br4nd New Passw0rd!"
}
```

Response (`200 OK`):

```json
{
  "message": "Password has been reset"
}
```

```bash
curl -X POST http://localhost:8081/api/v1/passwords/reset \
  -H "Content-Type: application/json" \
  -d '{ "token": "prt_5f6e7d8c9b0a1928374655463728190a1b2c3d4e5f60718", "newPassword": "Br4nd New Passw0rd!" }'
```

---

## VerificationController — `/api/v1/verification`

### POST /api/v1/verification/email/start

Auth: Bearer JWT.
Response (`200 OK`): `{ "message": "Verification email dispatched" }`

```bash
curl -X POST http://localhost:8081/api/v1/verification/email/start \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### POST /api/v1/verification/phone/start

Auth: Bearer JWT.
Response (`200 OK`): `{ "message": "Verification code dispatched" }`

```bash
curl -X POST http://localhost:8081/api/v1/verification/phone/start \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### POST /api/v1/verification/email/confirm

Auth: none.

Request body:

```json
{
  "token": "vft_1a2b3c4d5e6f7089a0b1c2d3e4f50617",
  "code": "482913"
}
```

Response (`200 OK`): `{ "message": "Email verified" }`

```bash
curl -X POST http://localhost:8081/api/v1/verification/email/confirm \
  -H "Content-Type: application/json" \
  -d '{ "token": "vft_1a2b3c4d5e6f7089a0b1c2d3e4f50617", "code": "482913" }'
```

### POST /api/v1/verification/phone/confirm

Auth: none.

Request body:

```json
{
  "token": "vft_2b3c4d5e6f70891a0b1c2d3e4f506172",
  "code": "917453"
}
```

Response (`200 OK`): `{ "message": "Phone verified" }`

```bash
curl -X POST http://localhost:8081/api/v1/verification/phone/confirm \
  -H "Content-Type: application/json" \
  -d '{ "token": "vft_2b3c4d5e6f70891a0b1c2d3e4f506172", "code": "917453" }'
```

---

## Error format

```json
{
  "error": "NOT_FOUND",
  "message": "Identity not found",
  "timestamp": "2026-07-25T14:40:00Z",
  "path": "/api/v1/admin/identities/does-not-exist",
  "fieldErrors": null
}
```
