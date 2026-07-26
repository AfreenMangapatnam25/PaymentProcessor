# Fraud Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `fraud-service` makes no outbound calls — it's called
*by* `payment-service` (pre-authorization risk check via `POST /api/fraud/evaluate`),
not the other way around. You can start and test it entirely standalone.

**Infrastructure:** Postgres (`fraudservicedb`). Config Server is optional.

Base URL: http://localhost:8088
Auth: Bearer JWT — see [Authentication](#authentication)

All entity-backed controllers (everything except `FraudEvaluationController`) bind
directly to their JPA `@Entity` classes for request/response bodies — there is no
separate DTO layer for them. `id` is a server-generated `UUID` (`GenerationType.UUID`);
omit it from POST bodies. Timestamp fields are ISO-8601 (`Instant`, e.g.
`2026-07-25T10:15:00Z`).

To exercise the GET-by-id endpoints against real data, run the service with the
`local` Spring profile (`SPRING_PROFILES_ACTIVE=local`), which loads
`src/main/resources/db/seed/V2__seed_sample_data.sql` via Flyway in addition to the
base schema migration. The fixed UUIDs used in the examples below match that seed
data.

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
permit-all chain, so with `SPRING_PROFILES_ACTIVE=local` — the profile the seeded-data examples
below already assume — the plain `curl` commands in this guide work as-is.
Never set it to `false` outside a developer machine or an ephemeral CI container.

**Always public** (no token, in either mode): `/actuator/health/**`, `/actuator/info`,
`/actuator/prometheus`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`.

**The same call, both ways:**

```bash
# with the `local` profile (security.jwt.enabled=false) — works as written
curl http://localhost:8088/api/devices

# with the toggle on (the default) — token required
curl http://localhost:8088/api/devices \
  -H "Authorization: Bearer $TOKEN"
```

---

## DeviceController — `/api/devices`

Entity: `Device` (table `devices`)

Fields: `id` (UUID, generated), `fingerprint` (String, NOT NULL, max 200),
`merchantId` (String, max 100), `firstSeen` (Instant), `lastSeen` (Instant),
`metadata` (String / free-text, e.g. JSON blob).

### GET /api/devices

List all devices.

```bash
curl http://localhost:8088/api/devices
```

Response:

```json
[
  {
    "id": "11111111-1111-1111-1111-111111111111",
    "fingerprint": "fp_a1b2c3d4e5f6",
    "merchantId": "mrc_9",
    "firstSeen": "2026-06-01T10:15:00Z",
    "lastSeen": "2026-07-20T09:00:00Z",
    "metadata": "{\"os\":\"iOS 17\",\"browser\":\"Safari\"}"
  }
]
```

### POST /api/devices

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "fingerprint": "fp_new_device_123",
  "merchantId": "mrc_9",
  "firstSeen": "2026-07-25T10:00:00Z",
  "lastSeen": "2026-07-25T10:00:00Z",
  "metadata": "{\"os\":\"macOS 15\",\"browser\":\"Chrome\"}"
}
```

Response (`200 OK`):

```json
{
  "id": "9c3f1e2a-4b5d-4a6e-8f7c-1a2b3c4d5e6f",
  "fingerprint": "fp_new_device_123",
  "merchantId": "mrc_9",
  "firstSeen": "2026-07-25T10:00:00Z",
  "lastSeen": "2026-07-25T10:00:00Z",
  "metadata": "{\"os\":\"macOS 15\",\"browser\":\"Chrome\"}"
}
```

```bash
curl -X POST http://localhost:8088/api/devices \
  -H "Content-Type: application/json" \
  -d '{"fingerprint":"fp_new_device_123","merchantId":"mrc_9","firstSeen":"2026-07-25T10:00:00Z","lastSeen":"2026-07-25T10:00:00Z","metadata":"{\"os\":\"macOS 15\"}"}'
```

### GET /api/devices/{id}

Auth: Bearer JWT — see [Authentication](#authentication)

```bash
curl http://localhost:8088/api/devices/11111111-1111-1111-1111-111111111111
```

Returns `200` with the device body shown above, or `404` if not found.

### PUT /api/devices/{id}

Auth: Bearer JWT — see [Authentication](#authentication). Note: the current implementation ignores the path `{id}` and simply
saves the request body as-is (it calls `service.save(entity)`), so include `id`
in the body to update a specific existing row; otherwise a new row is inserted.

Request body:

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "fingerprint": "fp_a1b2c3d4e5f6",
  "merchantId": "mrc_9",
  "firstSeen": "2026-06-01T10:15:00Z",
  "lastSeen": "2026-07-25T12:00:00Z",
  "metadata": "{\"os\":\"iOS 17\",\"browser\":\"Safari\"}"
}
```

```bash
curl -X PUT http://localhost:8088/api/devices/11111111-1111-1111-1111-111111111111 \
  -H "Content-Type: application/json" \
  -d '{"id":"11111111-1111-1111-1111-111111111111","fingerprint":"fp_a1b2c3d4e5f6","merchantId":"mrc_9","lastSeen":"2026-07-25T12:00:00Z"}'
```

### DELETE /api/devices/{id}

Auth: Bearer JWT — see [Authentication](#authentication). Returns `204 No Content`.

```bash
curl -X DELETE http://localhost:8088/api/devices/11111111-1111-1111-1111-111111111112
```

---

## FraudCaseController — `/api/cases`

Entity: `FraudCase` (table `cases`)

Fields: `id` (UUID, generated), `merchantId` (String, max 100), `intentId` (String,
max 100), `status` (String, max 30, e.g. `OPEN`, `IN_REVIEW`, `ESCALATED`,
`CLOSED`), `assignee` (String, max 100), `createdAt` (Instant).

### GET /api/cases

```bash
curl http://localhost:8088/api/cases
```

Response:

```json
[
  {
    "id": "22222222-2222-2222-2222-222222222221",
    "merchantId": "mrc_9",
    "intentId": "pi_1001",
    "status": "OPEN",
    "assignee": "analyst.jane",
    "createdAt": "2026-07-20T09:05:00Z"
  }
]
```

### POST /api/cases

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "merchantId": "mrc_9",
  "intentId": "pi_2001",
  "status": "OPEN",
  "assignee": "analyst.jane",
  "createdAt": "2026-07-25T10:00:00Z"
}
```

Response (`200 OK`): same shape with generated `id`.

```bash
curl -X POST http://localhost:8088/api/cases \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"mrc_9","intentId":"pi_2001","status":"OPEN","assignee":"analyst.jane","createdAt":"2026-07-25T10:00:00Z"}'
```

### GET /api/cases/{id}

```bash
curl http://localhost:8088/api/cases/22222222-2222-2222-2222-222222222221
```

Returns `200` with the case body, or `404` if not found.

### PUT /api/cases/{id}

Request body (include `id` — see Device note above regarding save semantics):

```json
{
  "id": "22222222-2222-2222-2222-222222222221",
  "merchantId": "mrc_9",
  "intentId": "pi_1001",
  "status": "CLOSED",
  "assignee": "analyst.jane",
  "createdAt": "2026-07-20T09:05:00Z"
}
```

```bash
curl -X PUT http://localhost:8088/api/cases/22222222-2222-2222-2222-222222222221 \
  -H "Content-Type: application/json" \
  -d '{"id":"22222222-2222-2222-2222-222222222221","status":"CLOSED"}'
```

### DELETE /api/cases/{id}

```bash
curl -X DELETE http://localhost:8088/api/cases/22222222-2222-2222-2222-222222222224
```

---

## FraudEvaluationController — `/api/fraud/evaluate`

The real-time scoring endpoint. Request/response use dedicated DTOs
(`FraudEvaluationRequest` / `FraudDecisionResponse`), not JPA entities.

`FraudEvaluationRequest` — all fields optional (the engine degrades gracefully
when a signal is missing):

- Transaction identity: `intentId`, `merchantId`, `userId`, `amount` (BigDecimal),
  `currency`, `timestamp` (Instant)
- Card/BIN: `cardBin`, `cardLast4`, `cardFingerprint`, `cardType`
  (`CREDIT`/`DEBIT`/`PREPAID`/`VIRTUAL`), `cardCurrency`, `binCountry`
- Network/IP: `ipAddress`, `ipCountry`, `ipType`
  (`RESIDENTIAL`/`VPN`/`PROXY`/`TOR`/`HOSTING`), `ipKnownFraud` (Boolean)
- Geography: `billingCountry`, `sanctionedCountry` (Boolean)
- Device: `deviceFingerprint`, `deviceNew`, `emulator`, `rooted`, `vpn` (all Boolean)
- Identity/email: `email`, `emailDomain`, `disposableEmail` (Boolean)
- User risk: `userAccountAgeDays` (Integer), `userKycStatus`
  (`VERIFIED`/`PENDING`/`FAILED`/`UNVERIFIED`), `userDisputeCount` (Integer),
  `userNewCustomer` (Boolean)
- Merchant risk: `merchantAccountAgeDays` (Integer), `merchantChargebackRate`
  (Double, ratio e.g. `0.012`), `merchantMcc`, `merchantHighRisk` (Boolean)
- AML/compliance: `sanctionsHit`, `pepHit` (Boolean)

`FraudDecisionResponse`: `intentId`, `decision`
(`APPROVE`/`CHALLENGE`/`REVIEW`/`DECLINE`/`ESCALATE`), `riskScore` (int 0-100),
`confidence` (double 0-1), `rulesFired` (List<String>), `mlContribution`
(double 0-1), `reasonCode`, `recommendedAction`
(`PROCEED`/`INITIATE_3DS`/`HOLD_FOR_REVIEW`/`BLOCK`/`ALERT_COMPLIANCE`),
`reviewQueueId` (String, non-null only when routed to REVIEW/ESCALATE),
`model`, `latencyMs` (long), `breakdown` (List of `RuleHit`: `name`, `category`,
`points`, `action`, `reason`).

### POST /api/fraud/evaluate

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "intentId": "pi_3001",
  "merchantId": "mrc_9",
  "userId": "usr_500",
  "amount": 5400.00,
  "currency": "USD",
  "timestamp": "2026-07-25T10:00:00Z",
  "cardBin": "411111",
  "cardLast4": "1234",
  "cardType": "CREDIT",
  "binCountry": "US",
  "ipAddress": "203.0.113.5",
  "ipCountry": "NG",
  "ipType": "VPN",
  "billingCountry": "US",
  "sanctionedCountry": false,
  "deviceFingerprint": "fp_new_device_123",
  "deviceNew": true,
  "emulator": false,
  "rooted": false,
  "vpn": true,
  "email": "shopper@example.com",
  "emailDomain": "example.com",
  "disposableEmail": false,
  "userAccountAgeDays": 12,
  "userKycStatus": "VERIFIED",
  "userDisputeCount": 0,
  "userNewCustomer": true,
  "merchantAccountAgeDays": 900,
  "merchantChargebackRate": 0.012,
  "merchantMcc": "5732",
  "merchantHighRisk": false,
  "sanctionsHit": false,
  "pepHit": false
}
```

Response (`200 OK`):

```json
{
  "intentId": "pi_3001",
  "decision": "REVIEW",
  "riskScore": 82,
  "confidence": 0.87,
  "rulesFired": [
    "high_amount_review",
    "device_new_challenge"
  ],
  "mlContribution": 0.64,
  "reasonCode": "RISK_ASSESSED",
  "recommendedAction": "HOLD_FOR_REVIEW",
  "reviewQueueId": "6f2c1a3b-9e4d-4c5a-8b6f-2d3e4f5a6b7c",
  "model": "fraud-xgb-core",
  "latencyMs": 42,
  "breakdown": [
    {
      "name": "high_amount_review",
      "category": "RULE",
      "points": 20,
      "action": "REVIEW",
      "reason": "amount >= 5000"
    },
    {
      "name": "device_new_challenge",
      "category": "DEVICE",
      "points": 10,
      "action": "CHALLENGE",
      "reason": "deviceNew == true"
    }
  ]
}
```

```bash
curl -X POST http://localhost:8088/api/fraud/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "intentId": "pi_3001",
    "merchantId": "mrc_9",
    "amount": 5400.00,
    "currency": "USD",
    "cardBin": "411111",
    "ipCountry": "NG",
    "billingCountry": "US",
    "deviceNew": true
  }'
```

---

## ListEntryController — `/api/lists`

Entity: `ListEntry` (table `lists`)

Fields: `id` (UUID, generated), `merchantId` (String, max 100), `list` (String,
max 20 — JSON/Java field is `list`, DB column `list_kind`; kind of list, e.g.
`BLACKLIST`/`WHITELIST`), `attribute` (String, max 40, e.g.
`CARD`/`IP`/`USER`/`DEVICE`/`EMAIL_DOMAIN`/`MERCHANT`), `value` (String, max 200),
`reason` (String, free text), `expiresAt` (Instant, nullable).

### GET /api/lists

```bash
curl http://localhost:8088/api/lists
```

Response:

```json
[
  {
    "id": "33333333-3333-3333-3333-333333333331",
    "merchantId": "mrc_9",
    "list": "BLACKLIST",
    "attribute": "CARD",
    "value": "411111",
    "reason": "Repeated chargebacks",
    "expiresAt": "2027-01-01T00:00:00Z"
  }
]
```

### POST /api/lists

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "merchantId": "mrc_9",
  "list": "BLACKLIST",
  "attribute": "IP",
  "value": "198.51.100.23",
  "reason": "Repeated failed logins",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

```bash
curl -X POST http://localhost:8088/api/lists \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"mrc_9","list":"BLACKLIST","attribute":"IP","value":"198.51.100.23","reason":"Repeated failed logins"}'
```

### GET /api/lists/{id}

```bash
curl http://localhost:8088/api/lists/33333333-3333-3333-3333-333333333331
```

### PUT /api/lists/{id}

```bash
curl -X PUT http://localhost:8088/api/lists/33333333-3333-3333-3333-333333333331 \
  -H "Content-Type: application/json" \
  -d '{"id":"33333333-3333-3333-3333-333333333331","merchantId":"mrc_9","list":"BLACKLIST","attribute":"CARD","value":"411111","reason":"Confirmed fraud ring"}'
```

### DELETE /api/lists/{id}

```bash
curl -X DELETE http://localhost:8088/api/lists/33333333-3333-3333-3333-333333333334
```

---

## ModelRegistryController — `/api/model-registry`

Entity: `ModelRegistry` (table `model_registry`)

Fields: `id` (UUID, generated), `name` (String, NOT NULL, max 150), `version`
(String, max 50), `status` (String, max 20, e.g. `ACTIVE`/`SHADOW`/`RETIRED`),
`createdAt` (Instant).

### GET /api/model-registry

```bash
curl http://localhost:8088/api/model-registry
```

Response:

```json
[
  {
    "id": "44444444-4444-4444-4444-444444444441",
    "name": "fraud-xgb-core",
    "version": "2.3.0",
    "status": "ACTIVE",
    "createdAt": "2026-05-01T00:00:00Z"
  }
]
```

### POST /api/model-registry

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "name": "fraud-xgb-core",
  "version": "2.4.0",
  "status": "SHADOW",
  "createdAt": "2026-07-25T10:00:00Z"
}
```

```bash
curl -X POST http://localhost:8088/api/model-registry \
  -H "Content-Type: application/json" \
  -d '{"name":"fraud-xgb-core","version":"2.4.0","status":"SHADOW","createdAt":"2026-07-25T10:00:00Z"}'
```

### GET /api/model-registry/{id}

```bash
curl http://localhost:8088/api/model-registry/44444444-4444-4444-4444-444444444441
```

### PUT /api/model-registry/{id}

```bash
curl -X PUT http://localhost:8088/api/model-registry/44444444-4444-4444-4444-444444444441 \
  -H "Content-Type: application/json" \
  -d '{"id":"44444444-4444-4444-4444-444444444441","name":"fraud-xgb-core","version":"2.3.0","status":"RETIRED"}'
```

### DELETE /api/model-registry/{id}

```bash
curl -X DELETE http://localhost:8088/api/model-registry/44444444-4444-4444-4444-444444444443
```

---

## RiskAssessmentController — `/api/risk-assessments`

Entity: `RiskAssessment` (table `risk_assessments`) — audit records normally
written by `RiskScoringEngine`, but exposed here for direct CRUD as well.

Fields: `id` (UUID, generated), `intentId` (String, max 100), `merchantId`
(String, max 100), `score` (BigDecimal, precision 5 scale 2), `decision`
(String, max 30), `triggeredRules` (String — JSON-encoded array), `features`
(String — JSON-encoded map), `model` (String, max 100), `latencyMs` (Integer),
`createdAt` (Instant).

### GET /api/risk-assessments

```bash
curl http://localhost:8088/api/risk-assessments
```

Response:

```json
[
  {
    "id": "55555555-5555-5555-5555-555555555551",
    "intentId": "pi_1001",
    "merchantId": "mrc_9",
    "score": 82.50,
    "decision": "REVIEW",
    "triggeredRules": "[\"velocity_card_1h\",\"device_new\"]",
    "features": "{\"amount\":5400.00,\"ipCountry\":\"NG\"}",
    "model": "fraud-xgb-core",
    "latencyMs": 45,
    "createdAt": "2026-07-20T09:00:12Z"
  }
]
```

### POST /api/risk-assessments

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "intentId": "pi_4001",
  "merchantId": "mrc_9",
  "score": 12.00,
  "decision": "APPROVE",
  "triggeredRules": "[]",
  "features": "{\"amount\":19.99}",
  "model": "fraud-xgb-core",
  "latencyMs": 18,
  "createdAt": "2026-07-25T10:00:00Z"
}
```

```bash
curl -X POST http://localhost:8088/api/risk-assessments \
  -H "Content-Type: application/json" \
  -d '{"intentId":"pi_4001","merchantId":"mrc_9","score":12.00,"decision":"APPROVE","model":"fraud-xgb-core","latencyMs":18}'
```

### GET /api/risk-assessments/{id}

```bash
curl http://localhost:8088/api/risk-assessments/55555555-5555-5555-5555-555555555551
```

### PUT /api/risk-assessments/{id}

```bash
curl -X PUT http://localhost:8088/api/risk-assessments/55555555-5555-5555-5555-555555555551 \
  -H "Content-Type: application/json" \
  -d '{"id":"55555555-5555-5555-5555-555555555551","decision":"CLOSED_CONFIRMED_FRAUD"}'
```

### DELETE /api/risk-assessments/{id}

```bash
curl -X DELETE http://localhost:8088/api/risk-assessments/55555555-5555-5555-5555-555555555554
```

---

## RuleController — `/api/rules`

Entity: `Rule` (table `rules`)

Fields: `id` (UUID, generated), `scope` (String, max 50, e.g.
`GLOBAL`/`MERCHANT`), `name` (String, max 150), `expr` (String — SpEL
expression evaluated against the feature map), `action` (String, max 20, e.g.
`SCORE`/`CHALLENGE`/`REVIEW`/`DECLINE`/`ESCALATE`), `priority` (Integer),
`enabled` (Boolean), `version` (Integer).

### GET /api/rules

```bash
curl http://localhost:8088/api/rules
```

Response:

```json
[
  {
    "id": "66666666-6666-6666-6666-666666666661",
    "scope": "GLOBAL",
    "name": "high_amount_review",
    "expr": "amount >= 5000",
    "action": "REVIEW",
    "priority": 10,
    "enabled": true,
    "version": 1
  }
]
```

### POST /api/rules

Auth: Bearer JWT — see [Authentication](#authentication)

Request body:

```json
{
  "scope": "MERCHANT",
  "name": "vpn_challenge",
  "expr": "vpn == true",
  "action": "CHALLENGE",
  "priority": 25,
  "enabled": true,
  "version": 1
}
```

```bash
curl -X POST http://localhost:8088/api/rules \
  -H "Content-Type: application/json" \
  -d '{"scope":"MERCHANT","name":"vpn_challenge","expr":"vpn == true","action":"CHALLENGE","priority":25,"enabled":true,"version":1}'
```

### GET /api/rules/{id}

```bash
curl http://localhost:8088/api/rules/66666666-6666-6666-6666-666666666661
```

### PUT /api/rules/{id}

```bash
curl -X PUT http://localhost:8088/api/rules/66666666-6666-6666-6666-666666666661 \
  -H "Content-Type: application/json" \
  -d '{"id":"66666666-6666-6666-6666-666666666661","scope":"GLOBAL","name":"high_amount_review","expr":"amount >= 7500","action":"REVIEW","priority":10,"enabled":true,"version":2}'
```

### DELETE /api/rules/{id}

```bash
curl -X DELETE http://localhost:8088/api/rules/66666666-6666-6666-6666-666666666664
```
