# gateway-service

**Store:** none durable (Redis for rate limits, JWKS cache, routing state)

## What it is

The single ingress edge for the platform. It terminates TLS, authenticates every
request by validating short-lived JWTs locally against a cached JWKS, enforces
per-key rate limits, and routes to downstream services. It owns **nothing
durable** — if it loses its Redis it degrades, it does not lose data.

## What it owns

Nothing persistent. Ephemeral state only: token buckets, the JWKS cache, and
resolved routing decisions.

## How it relates to other services

- Validates tokens minted by **authentication-service** without calling it on the
  hot path (JWKS is cached locally).
- Fans requests out to all 16 routed downstream services: **payment, refund,
  merchant, user, authentication, tokenization, limit, authorization, fraud,
  clearing, dispute, settlement, ledger, reconciliation, audit, reporting**.
  (There is currently no route to **notification-service** — see Routes below.)
- Emits request events to **audit-service**, both via a normal proxied route
  (`/api/v1/audit-records/**`, `/api/v1/audit/**`) and via a separate
  best-effort async audit sink used by the gateway's own `AuditFilter`.

---

## Architecture

Built on **Spring Cloud Gateway** (reactive / WebFlux). Every request flows through
this ordered pipeline:

1. **Correlation id** — honour an inbound `X-Correlation-Id` or mint one; forward it
   downstream and echo it to the client.
2. **Request size guard** — reject bodies over `gateway.max-request-body-bytes`.
3. **Authentication** — a valid JWT (`Authorization: Bearer …`) verified against the
   cached JWKS, **or** a valid API key (`X-API-Key`). Public paths are exempt.
4. **Rate limiting** — Redis token bucket, bucketed per API key → JWT subject → IP.
5. **Header enrichment** — inject trusted `X-User-Id`, `X-User-Roles`,
   `X-Merchant-Id`. Client-supplied copies are stripped first so identity can't be
   spoofed.
6. **Resilience** — per-route circuit breaker + time limiter, with retries on
   idempotent GETs; opens to a `/fallback/*` 503 envelope.
7. **Routing** — proxy to the matched downstream service.
8. **Audit** — emit an async event to audit-service (best-effort, off the hot path).

Errors everywhere are rendered as a consistent JSON envelope (`error`, `message`,
`status`, `correlationId`).

## Endpoints exposed by the gateway itself

| Path                   | Auth   | Purpose                              |
|------------------------|--------|--------------------------------------|
| `/api/gateway/health`  | public | Simple liveness                      |
| `/actuator/health/**`  | public | Kubernetes liveness/readiness probes |
| `/actuator/prometheus` | authz  | Prometheus metrics scrape            |
| `/actuator/gateway/**` | authz  | Route inspection                     |
| `/fallback/{service}`  | public | Circuit-breaker fallback (503)       |

## Routes

`application.yml` currently declares **16 routes**. Every route uses the same
per-route filter chain: `RequestRateLimiter` (Redis token bucket) →
`CircuitBreaker` (Resilience4j, falls back to `/fallback/{service}`) → `Retry`
(idempotent GETs only, 2 attempts, exponential backoff) — except `refund-service`,
which has rate limiting and a circuit breaker but no retry filter.

All downstream URIs default to `http://<service-name>:8080` (in-cluster service
name + container port 8080), overridable per route via an env var — they are
**not** the 8081–8096 host-port scheme used for local/dev port-forwarding.

| Route                  | Path predicate(s)                                                                                                                                                                                   | Default URI (env var)                                               |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| payment-service        | `/api/v1/payments/**`, `/api/v1/charges/**`                                                                                                                                                         | `http://payment-service:8080` (`PAYMENT_SERVICE_URI`)               |
| refund-service         | `/api/v1/refunds/**`                                                                                                                                                                                | `http://refund-service:8080` (`REFUND_SERVICE_URI`)                 |
| merchant-service       | `/api/v1/merchants/**`                                                                                                                                                                              | `http://merchant-service:8080` (`MERCHANT_SERVICE_URI`)             |
| user-service           | `/api/v1/users/**`                                                                                                                                                                                  | `http://user-service:8080` (`USER_SERVICE_URI`)                     |
| authentication-service | `/api/v1/auth/**`, `/api/v1/mfa/**`, `/api/v1/passwords/**`, `/api/v1/devices/**`, `/api/v1/api-keys/**`, `/api/v1/verification/**`, `/api/v1/admin/identities/**`                                  | `http://authentication-service:8080` (`AUTHENTICATION_SERVICE_URI`) |
| tokenization-service   | `/api/instruments/**`, `/api/card-details/**`, `/api/network-tokens/**`, `/api/bin-ranges/**`, `/api/bank-details/**`, `/api/dek-registry/**`, `/api/instrument-access-log/**`                      | `http://tokenization-service:8080` (`TOKENIZATION_SERVICE_URI`)     |
| limit-service          | `/api/v1/limits/**`, `/api/v1/limit-configs/**`, `/api/v1/reservations/**`                                                                                                                          | `http://limit-service:8080` (`LIMIT_SERVICE_URI`)                   |
| authorization-service  | `/api/v1/authorizations/**`, `/api/v1/access/**`, `/api/v1/admin/**`                                                                                                                                | `http://authorization-service:8080` (`AUTHORIZATION_SERVICE_URI`)   |
| fraud-service          | `/api/fraud/**`, `/api/cases/**`, `/api/rules/**`, `/api/risk-assessments/**`, `/api/lists/**`, `/api/model-registry/**`, `/api/devices/**`                                                         | `http://fraud-service:8080` (`FRAUD_SERVICE_URI`)                   |
| clearing-service       | `/api/v1/clearing/**`                                                                                                                                                                               | `http://clearing-service:8080` (`CLEARING_SERVICE_URI`)             |
| dispute-service        | `/api/v1/disputes/**`, `/api/v1/reason-codes/**`                                                                                                                                                    | `http://dispute-service:8080` (`DISPUTE_SERVICE_URI`)               |
| settlement-service     | `/api/payouts/**`, `/api/payout-returns/**`, `/api/settlement-runs/**`, `/api/settlement-items/**`, `/api/settlement-batches/**`, `/api/adjustments/**`, `/api/reserves/**`, `/api/reports/**`      | `http://settlement-service:8080` (`SETTLEMENT_SERVICE_URI`)         |
| ledger-service         | `/api/v1/accounts/**`, `/api/v1/journals/**`, `/api/v1/entries/**`, `/api/v1/periods/**`, `/api/v1/currencies/**`, `/api/v1/account-types/**`, `/api/v1/trial-balance/**`, `/api/balance-shards/**` | `http://ledger-service:8080` (`LEDGER_SERVICE_URI`)                 |
| reconciliation-service | `/api/v1/recon-runs/**`, `/api/v1/records/**`, `/api/v1/matches/**`, `/api/v1/exceptions/**`, `/api/v1/statements/**`, `/api/v1/adjustments/**`                                                     | `http://reconciliation-service:8080` (`RECONCILIATION_SERVICE_URI`) |
| audit-service          | `/api/v1/audit-records/**`, `/api/v1/audit/**`                                                                                                                                                      | `http://audit-service:8080` (`AUDIT_SERVICE_URI_ROUTE`)             |
| reporting-service      | `/api/v1/reports/**`, `/api/v1/analytics/**`, `/api/v1/query/**`, `/api/v1/schedules/**`                                                                                                            | `http://reporting-service:8080` (`REPORTING_SERVICE_URI`)           |

**Notable gap:** there is **no route to notification-service** in `application.yml`
today, despite it existing as one of the platform's 17 services. If it needs to be
reachable through the edge, a route must be added.

Note also that `settlement-service` and `reporting-service` both claim the
`/api/reports/**` and `/api/v1/reports/**` predicates respectively — these are
different path prefixes so they don't collide, but keep that in mind when adding
new report-related paths.

Add or change routes in `src/main/resources/application.yml` — no recompile needed.

## Configuration (environment variables)

| Variable                    | Default                                                    | Notes                                                                                                             |
|-----------------------------|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `SERVER_PORT`               | `8443`                                                     | Listen port                                                                                                       |
| `TLS_ENABLED`               | `false`                                                    | Set `true` + keystore in prod                                                                                     |
| `TLS_KEYSTORE`              | –                                                          | Path to PKCS12 keystore                                                                                           |
| `TLS_KEYSTORE_PASSWORD`     | –                                                          | Keystore password                                                                                                 |
| `JWKS_URI`                  | `http://authentication-service:8080/.well-known/jwks.json` | Cached JWK set for JWT validation                                                                                 |
| `JWT_ISSUER_URI`            | – (unset = no issuer check)                                | Set in prod to pin the issuer                                                                                     |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379`                                       | Rate-limit backing store                                                                                          |
| `REDIS_PASSWORD`            | –                                                          | Redis auth                                                                                                        |
| `CORS_ALLOWED_ORIGINS`      | `https://*.paymentprocessor.com`                           | Allowed browser origins                                                                                           |
| `MAX_REQUEST_BODY_BYTES`    | `1048576`                                                  | Max proxied body size (1 MiB)                                                                                     |
| `AUDIT_SERVICE_URI`         | `http://audit-service:8080`                                | Target for the gateway's own async `AuditFilter` sink                                                             |
| `AUDIT_SERVICE_PATH`        | `/api/v1/audit/events`                                     | Path the `AuditFilter` posts to                                                                                   |
| `AUDIT_SERVICE_URI_ROUTE`   | `http://audit-service:8080`                                | Downstream URI for the proxied `audit-service` **route** (distinct from the two vars above)                       |
| `AUDIT_ENABLED`             | `true`                                                     | Toggle audit emission                                                                                             |
| `API_KEY_HEADER`            | `X-API-Key`                                                | Header name checked for API-key auth                                                                              |
| `ZIPKIN_ENDPOINT`           | `http://zipkin:9411/api/v2/spans`                          | Trace export                                                                                                      |
| `TRACE_SAMPLE_RATE`         | `0.1`                                                      | Fraction of requests traced                                                                                       |
| `{SERVICE}_SERVICE_URI`     | `http://<service-name>:8080`                               | Per-route downstream URI override, one per routed service (e.g. `PAYMENT_SERVICE_URI`, `LEDGER_SERVICE_URI`, ...) |
| `*_RL_RATE` / `*_RL_BURST`  | see `application.yml`                                      | Per-route Redis token-bucket replenish rate / burst capacity                                                      |

### API keys

For non-secret environments, keys can be declared under `gateway.apikey.keys` in
config:

```yaml
gateway:
  apikey:
    keys:
      - key: ${PARTNER_A_KEY}
        principal: partner-a
        merchantId: M-1001
        roles: [ PARTNER ]
```

**Production:** load keys from a secret manager and store them hashed. Keys are
compared in constant time to avoid timing side channels.

## Running locally

Prereqs: **JDK 21** (Gradle toolchain), a running **Redis**, and (optionally) a
JWKS endpoint.

```bash
# start Redis
docker run -p 6379:6379 redis:7

# run the gateway on plain HTTP for local dev (TLS off by default)
SERVER_PORT=8080 \
JWKS_URI=http://localhost:9000/.well-known/jwks.json \
PAYMENT_SERVICE_URI=http://localhost:8081 \
./gradlew.bat :gateway-service:bootRun
```

Health check:

```bash
curl http://localhost:8080/api/gateway/health
```

## Build

```bash
./gradlew clean build      # produces build/libs/gateway-service-1.0.0.jar
java -jar build/libs/gateway-service-1.0.0.jar
```

> This service ships without automated tests by design (integration depends on live
> Redis + downstream services). Validate behaviour against a running Redis and the
> downstream stubs in your environment.
