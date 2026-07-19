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
- Fans requests out to **payment-service**, **merchant-service**, **user-service**
  and the rest.
- Emits request events to **audit-service**.

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

| Path                      | Auth   | Purpose                              |
|---------------------------|--------|--------------------------------------|
| `/api/gateway/health`     | public | Simple liveness                      |
| `/actuator/health/**`     | public | Kubernetes liveness/readiness probes |
| `/actuator/prometheus`    | authz  | Prometheus metrics scrape            |
| `/actuator/gateway/**`    | authz  | Route inspection                     |
| `/fallback/{service}`     | public | Circuit-breaker fallback (503)       |

## Routes

| Route            | Path predicate                              | Downstream (env var)        |
|------------------|---------------------------------------------|-----------------------------|
| payment-service  | `/api/v1/payments/**`, `/api/v1/charges/**` | `PAYMENT_SERVICE_URI`       |
| refund-service   | `/api/v1/refunds/**`                         | `REFUND_SERVICE_URI`        |
| merchant-service | `/api/v1/merchants/**`                       | `MERCHANT_SERVICE_URI`      |
| user-service     | `/api/v1/users/**`                           | `USER_SERVICE_URI`          |

Add or change routes in `src/main/resources/application.yml` — no recompile needed.

## Configuration (environment variables)

| Variable                    | Default                                                   | Notes                              |
|-----------------------------|-----------------------------------------------------------|------------------------------------|
| `SERVER_PORT`               | `8443`                                                    | Listen port                        |
| `TLS_ENABLED`               | `false`                                                   | Set `true` + keystore in prod      |
| `TLS_KEYSTORE`              | –                                                         | Path to PKCS12 keystore            |
| `TLS_KEYSTORE_PASSWORD`     | –                                                         | Keystore password                  |
| `JWKS_URI`                  | `http://authentication-service:8080/.well-known/jwks.json`| Cached JWK set for JWT validation  |
| `JWT_ISSUER_URI`            | – (unset = no issuer check)                               | Set in prod to pin the issuer      |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379`                                     | Rate-limit backing store           |
| `REDIS_PASSWORD`            | –                                                         | Redis auth                         |
| `CORS_ALLOWED_ORIGINS`      | `https://*.paymentprocessor.com`                         | Allowed browser origins            |
| `MAX_REQUEST_BODY_BYTES`    | `1048576`                                                 | Max proxied body size (1 MiB)      |
| `AUDIT_SERVICE_URI`         | `http://audit-service:8080`                              | Audit sink                         |
| `AUDIT_ENABLED`             | `true`                                                   | Toggle audit emission              |
| `ZIPKIN_ENDPOINT`           | `http://zipkin:9411/api/v2/spans`                        | Trace export                       |
| `TRACE_SAMPLE_RATE`         | `0.1`                                                    | Fraction of requests traced        |
| `*_RL_RATE` / `*_RL_BURST`  | see `application.yml`                                     | Per-route replenish / burst        |

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
        roles: [PARTNER]
```

**Production:** load keys from a secret manager and store them hashed. Keys are
compared in constant time to avoid timing side channels.

## Running locally

Prereqs: **JDK 17+**, a running **Redis**, and (optionally) a JWKS endpoint.

```bash
# start Redis
docker run -p 6379:6379 redis:7

# run the gateway on plain HTTP for local dev (TLS off by default)
SERVER_PORT=8080 \
JWKS_URI=http://localhost:9000/.well-known/jwks.json \
PAYMENT_SERVICE_URI=http://localhost:8081 \
./gradlew bootRun
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
