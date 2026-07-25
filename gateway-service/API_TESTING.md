# Gateway Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** `gateway-service` doesn't strictly require anything else running
to start, but it's useless on its own since every route just proxies to a backend
service. Practically:

- **authentication-service** — required for JWT validation (`JWKS_URI`, default
  `http://authentication-service:8080/.well-known/jwks.json`); without it, any
  route protected by the resource-server filter will fail token checks.
- **Whichever backend service(s)** you actually want to call through the gateway
  (audit, authentication, authorization, clearing, dispute, fraud, ledger, limit,
  merchant, notification, payment, reconciliation, reporting, settlement,
  tokenization, user) — the gateway routes to all 16 of them by service name/port
  8080 in each route's `uri:`, so for local testing outside Docker/Eureka you'll
  likely need to override those URIs (`*_SERVICE_URI` env vars) to point at
  `localhost:<actual port>` for each one you want reachable.

**Infrastructure:** Redis (token-bucket rate limiting — required, the gateway is
reactive/WebFlux and uses `spring-boot-starter-data-redis-reactive`). Config Server
is optional.

## What this service does

`gateway-service` is a pure **Spring Cloud Gateway** routing layer. It owns no
domain controllers or business endpoints of its own — every request it
receives is matched against a route's `Path` predicate and proxied to the
appropriate downstream microservice. On top of routing, it applies, per route:

- **Rate limiting** — a Redis-backed token-bucket (`RequestRateLimiter` filter)
  keyed by an `apiKeyResolver` bean (i.e. limits are enforced per API key /
  caller, not globally). Each route has its own `replenishRate` (tokens/sec)
  and `burstCapacity` (bucket size), overridable via environment variables
  (e.g. `PAYMENT_RL_RATE`, `LEDGER_RL_RATE`, `LIMIT_RL_RATE`, ...). A caller
  that exceeds the bucket receives `HTTP 429 Too Many Requests`.
- **Circuit breaking** — a Resilience4j `CircuitBreaker` filter per route
  (count-based sliding window of 20 calls, opens above 50% failure rate or
  80% slow-call rate, waits 10s before probing again). On an open circuit the
  gateway forwards to `forward:/fallback/<service-name>` instead of calling
  downstream.
- **Retry** — most routes (all except `payment-service`'s... note:
  `payment-service` *does* retry, `refund-service`/`merchant-service` do
  **not**) retry idempotent `GET` requests up to twice on `5xx` responses,
  with exponential backoff (50ms → 500ms, factor 2).
- **JWT validation** — the gateway is configured as an OAuth2 **resource
  server** (`spring.security.oauth2.resourceserver.jwt`). It validates
  short-lived JWTs locally against a cached JWKS fetched once from
  `authentication-service` (`JWKS_URI`, default
  `http://authentication-service:8080/.well-known/jwks.json`) — the
  authentication service is **not** called on the hot path. Paths listed
  under `gateway.public-paths` (health/info/fallback endpoints) skip JWT
  validation. The gateway also strips any client-supplied `X-User-Id`,
  `X-User-Roles`, and `X-Merchant-Id` headers before proxying, so callers
  cannot spoof identity that downstream services trust.
- **CORS** — a single global CORS policy applies to `/**`, allowing origins
  matching `CORS_ALLOWED_ORIGINS` (default `https://*.paymentprocessor.com`),
  methods `GET, POST, PUT, PATCH, DELETE, OPTIONS`, and credentials.
- **Request size limit** — request bodies over `MAX_REQUEST_BODY_BYTES`
  (default 1 MiB) are rejected before being proxied.

## Base URL / port

```
server.port: ${SERVER_PORT:8443}
```

Locally (no TLS keystore configured, `TLS_ENABLED=false` by default) the
gateway serves **plain HTTP** on port `8443`:

```
http://localhost:8443
```

In an environment with `TLS_ENABLED=true` and a keystore supplied, it serves
HTTPS on the same port instead.

## Routes (from `application.yml`)

All routes apply `RequestRateLimiter` (Redis token bucket, keyed by API key)
and a per-route `CircuitBreaker` with fallback `forward:/fallback/<id>`.
The **Retry** column marks routes that also retry idempotent `GET` calls
(2 retries, backoff 50ms→500ms) on `5xx`.

| Route id               | Path pattern(s)                                                                                                                                                                                     | Downstream URI (env override)                                               | Rate limit (replenish/burst) | Retry on GET 5xx |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|------------------------------|------------------|
| payment-service        | `/api/v1/payments/**`, `/api/v1/charges/**`                                                                                                                                                         | `PAYMENT_SERVICE_URI` (default `http://payment-service:8080`)               | 50 / 100                     | Yes              |
| refund-service         | `/api/v1/refunds/**`                                                                                                                                                                                | `REFUND_SERVICE_URI` (default `http://refund-service:8080`)                 | 20 / 40                      | No               |
| merchant-service       | `/api/v1/merchants/**`                                                                                                                                                                              | `MERCHANT_SERVICE_URI` (default `http://merchant-service:8080`)             | 30 / 60                      | No               |
| user-service           | `/api/v1/users/**`, `/api/v1/customers/**`, `/api/v1/addresses/**`, `/api/v1/consents/**`                                                                                                           | `USER_SERVICE_URI` (default `http://user-service:8080`)                     | 30 / 60                      | No               |
| authentication-service | `/api/v1/auth/**`, `/api/v1/mfa/**`, `/api/v1/passwords/**`, `/api/v1/devices/**`, `/api/v1/api-keys/**`, `/api/v1/verification/**`, `/api/v1/admin/identities/**`                                  | `AUTHENTICATION_SERVICE_URI` (default `http://authentication-service:8080`) | 30 / 60                      | Yes              |
| tokenization-service   | `/api/instruments/**`, `/api/card-details/**`, `/api/network-tokens/**`, `/api/bin-ranges/**`, `/api/bank-details/**`, `/api/dek-registry/**`, `/api/instrument-access-log/**`                      | `TOKENIZATION_SERVICE_URI` (default `http://tokenization-service:8080`)     | 30 / 60                      | Yes              |
| **limit-service**      | `/api/v1/limits/**`, `/api/v1/limit-configs/**`, `/api/v1/reservations/**`                                                                                                                          | `LIMIT_SERVICE_URI` (default `http://limit-service:8080`)                   | 30 / 60                      | Yes              |
| authorization-service  | `/api/v1/authorizations/**`, `/api/v1/access/**`, `/api/v1/admin/**`                                                                                                                                | `AUTHORIZATION_SERVICE_URI` (default `http://authorization-service:8080`)   | 50 / 100                     | Yes              |
| fraud-service          | `/api/fraud/**`, `/api/cases/**`, `/api/rules/**`, `/api/risk-assessments/**`, `/api/lists/**`, `/api/model-registry/**`, `/api/devices/**`                                                         | `FRAUD_SERVICE_URI` (default `http://fraud-service:8080`)                   | 30 / 60                      | Yes              |
| clearing-service       | `/api/v1/clearing/**`                                                                                                                                                                               | `CLEARING_SERVICE_URI` (default `http://clearing-service:8080`)             | 30 / 60                      | Yes              |
| dispute-service        | `/api/v1/disputes/**`, `/api/v1/reason-codes/**`                                                                                                                                                    | `DISPUTE_SERVICE_URI` (default `http://dispute-service:8080`)               | 30 / 60                      | Yes              |
| settlement-service     | `/api/payouts/**`, `/api/payout-returns/**`, `/api/settlement-runs/**`, `/api/settlement-items/**`, `/api/settlement-batches/**`, `/api/adjustments/**`, `/api/reserves/**`, `/api/reports/**`      | `SETTLEMENT_SERVICE_URI` (default `http://settlement-service:8080`)         | 30 / 60                      | Yes              |
| **ledger-service**     | `/api/v1/accounts/**`, `/api/v1/journals/**`, `/api/v1/entries/**`, `/api/v1/periods/**`, `/api/v1/currencies/**`, `/api/v1/account-types/**`, `/api/v1/trial-balance/**`, `/api/balance-shards/**` | `LEDGER_SERVICE_URI` (default `http://ledger-service:8080`)                 | 30 / 60                      | Yes              |
| reconciliation-service | `/api/v1/recon-runs/**`, `/api/v1/records/**`, `/api/v1/matches/**`, `/api/v1/exceptions/**`, `/api/v1/statements/**`, `/api/v1/adjustments/**`                                                     | `RECONCILIATION_SERVICE_URI` (default `http://reconciliation-service:8080`) | 30 / 60                      | Yes              |
| notification-service   | `/api/events/**`, `/api/messages/**`, `/api/templates/**`, `/api/suppressions/**`, `/api/webhook-deliveries/**`, `/api/webhook-endpoints/**`                                                        | `NOTIFICATION_SERVICE_URI` (default `http://notification-service:8080`)     | 30 / 60                      | Yes              |
| audit-service          | `/api/v1/audit-records/**`, `/api/v1/audit/**`                                                                                                                                                      | `AUDIT_SERVICE_URI_ROUTE` (default `http://audit-service:8080`)             | 30 / 60                      | Yes              |
| reporting-service      | `/api/v1/reports/**`, `/api/v1/analytics/**`, `/api/v1/query/**`, `/api/v1/schedules/**`                                                                                                            | `REPORTING_SERVICE_URI` (default `http://reporting-service:8080`)           | 30 / 60                      | Yes              |

> **Note on ledger-service coverage:** the `ledger-service` route only
> matches `/api/v1/accounts/**` (which covers the nested
> `/api/v1/accounts/{id}/balance`, `/api/v1/accounts/{id}/holds`,
> `/api/v1/accounts/{id}/snapshots`, `/api/v1/accounts/{id}/statement`
> sub-paths) plus the top-level paths listed above. The *alias* endpoints
> `GET /api/v1/balances/{accountId}`, `POST/GET /api/v1/holds/**`, and
> `POST/GET /api/v1/snapshots` are **not** matched by any predicate in this
> config and are currently only reachable by calling `ledger-service`
> directly — this may be worth adding to the route's `Path=` list if those
> aliases need to be exposed through the gateway.

## Example curl commands (through the gateway)

Assuming the gateway is running locally on `http://localhost:8443` (plain
HTTP, `TLS_ENABLED=false`) and, where JWT validation is enforced, a bearer
token issued by `authentication-service`:

```bash
# Call ledger-service's "list accounts" endpoint through the gateway
curl http://localhost:8443/api/v1/accounts \
  -H "Authorization: Bearer <jwt>"

# Call limit-service's limit-check endpoint through the gateway
curl -X POST http://localhost:8443/api/v1/limits/check \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{
        "customerId": "CUST-1001",
        "currency": "USD",
        "amount": 250.00,
        "country": "US"
      }'

# Reserve limit capacity through the gateway (called by payment-service in
# practice, but reachable the same way for testing)
curl -X POST http://localhost:8443/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
        "transactionId": "TXN-2026-0001",
        "customerId": "CUST-1001",
        "currency": "USD",
        "amount": 250.00
      }'
```

If a downstream service is unavailable or its circuit is open, the gateway
responds via the configured fallback (`forward:/fallback/<service-id>`)
instead of hanging until the 8s response-timeout ceiling.
