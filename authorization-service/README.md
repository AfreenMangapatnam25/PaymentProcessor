# authorization-service

Two bounded contexts in one deployable: card payment authorization via Stripe, and RBAC/ABAC access-control decisioning
for the rest of the platform.

> A more conceptual/narrative overview of the RBAC model also exists at `AuthorizationReadme.md` in this directory. This
> README is the code-grounded reference — endpoints, config, and schema as actually implemented.

## Role in the platform

- **Payment authorization**: wraps the Stripe SDK to authorize, capture, reverse, reauthorize, and inquire on card
  payments, persisting a local `AuthorizationRecord` that never stores PAN/CVV (Stripe holds the raw card data).
- **Access control**: a separate RBAC/ABAC engine — roles with inherited permissions, scoped role assignments (
  global/merchant/resource), and condition-based policies — that other services or the gateway can query for allow/deny
  decisions.
- **RBAC administration**: exposes a full admin API for managing permissions, roles (including hierarchical parent
  roles), role assignments, and ABAC policies.
- Both contexts share one Spring Boot process, database, and Kafka producer, but are implemented as separate
  packages/entities/controllers with no code coupling between them beyond shared JWT/security config.
- Publishes domain events for both contexts to Kafka so downstream services can react to authorization and
  access-control changes asynchronously.
- Idempotent payment authorization via a stored `idempotency_key` table, mirroring the pattern used in payment-service.

## Tech stack

- Spring Boot (Java 21/17 toolchain per Dockerfile), Gradle
- Default port: **8086** (`server.port: 8086` in `application.yml`)
- Datastore: PostgreSQL (`authorization_db` locally; `authorization_local`/`_dev`/`_prod` per profile)
- **Flyway enabled** (`flyway.enabled: true`, `baseline-on-migrate: true`) — `V1__init_schema.sql`, `V2__seed_rbac.sql`;
  JPA `ddl-auto: validate` (schema is migration-owned, not Hibernate-generated)
- Stripe Java SDK for the payment-authorization context
- Kafka producer (idempotent: `acks=all`, `retries=5`, `enable.idempotence=true`)
- Eureka client for service discovery
- Resilience4j (retry, circuit breaker, time limiter) wrapping the Stripe gateway call
- springdoc-openapi (`/swagger-ui.html`, `/v3/api-docs`)
- Profiles: `local`, `dev`, `prod` (prod adds TLS, larger pool, Prometheus export)

## API surface

**`PaymentAuthorizationController`** — base path `/api/v1/authorizations` (payment authorization, Stripe-backed):

- `POST /api/v1/authorizations` (header `Idempotency-Key`, optional) — authorize a card payment
- `GET /api/v1/authorizations/{id}` — inquiry
- `GET /api/v1/authorizations?paymentReference=...` — list by payment reference
- `POST /api/v1/authorizations/{id}/capture` — full or partial capture
- `POST /api/v1/authorizations/{id}/reversal` — cancel/void
- `POST /api/v1/authorizations/{id}/reauthorize` — non-obvious: re-authorize against the original authorization, tracked
  via `originalAuthorizationId`/`reauthorizationCount` with a configured max (`authorization.max-reauthorizations`,
  default 3)
- `POST /api/v1/authorizations/{id}/synchronize` — reconcile local record against Stripe's current state

**`AccessControlController`** — base path `/api/v1/access` (RBAC/ABAC decisioning, used by other services/gateway):

- `POST /api/v1/access/check` — evaluate an access decision
- `POST /api/v1/access/scopes/validate` — validate OAuth2/API scopes against a requested operation
- `GET /api/v1/access/identities/{identityId}/permissions` — resolve an identity's effective permissions

**`RoleAdminController`** — base path `/api/v1/admin` (RBAC governance):

- `POST/GET /api/v1/admin/permissions`
- `POST/GET /api/v1/admin/roles`, `GET /api/v1/admin/roles/{name}`, `DELETE /api/v1/admin/roles/{name}`
- `POST /api/v1/admin/roles/{name}/permissions/{permission}`, `DELETE .../{name}/permissions/{permission}`
- `POST /api/v1/admin/assignments`, `GET /api/v1/admin/identities/{identityId}/assignments`,
  `DELETE /api/v1/admin/identities/{identityId}/roles/{roleName}`
- `POST/PUT/GET/DELETE /api/v1/admin/policies[/{id}]`

## Data model

**Payment-authorization context**: `AuthorizationRecord` (table `authorization_record`) — status, type,
merchant/customer id, payment reference, requested/approved/captured amounts, currency, masked card metadata (network,
BIN, last4, exp), authorization code, network reference id, gateway provider/id/response code/message/raw response,
AVS/CVV results, `requiresAuthentication`/`authenticationUrl` (3DS), `originalAuthorizationId`/`reauthorizationCount`,
idempotency key, risk score, optimistic `version`. Explicitly does not store PAN/CVV. `IdempotencyKey` (table
`idempotency_key`) backs the idempotent-create flow.

**Access-control context**: `Permission` (atomic right, e.g. `transaction:read`), `Role` (named permission set,
self-referencing `parent` for hierarchical inheritance, `category` PLATFORM/MERCHANT/USER, `systemRole` flag),
`RoleAssignment` (identity + role + scope, unique per identity/role/scope/scopeId), `Policy` (ABAC — `condition_json`,
`effect`, `priority`, `enabled`, indexed on resource+action). Seeded (`V2__seed_rbac.sql`) with 11 permissions and 9
system roles (`SUPER_ADMIN`, `PLATFORM_AUDITOR`, `SYSTEM`, `MERCHANT_OWNER`, `MERCHANT_ADMIN`, `MERCHANT_VIEWER`,
`MERCHANT_BILLING`, `END_USER`, `PREMIUM_USER`), including a sample ABAC policy (`high-value-transaction-guard`) that
requires `subject.kyc_status = VERIFIED` and `environment.device_trust_level >= 0.8`.

No shared DB-level or code-level relationship between `AuthorizationRecord` and the RBAC tables — the two contexts are
logically independent and only coexist because they're deployed together.

## Inter-service integration

- **Stripe** (`gateway/stripe/StripeGatewayClient`, external, not a platform service):
  `PaymentIntent.create/capture/cancel/retrieve`, `apiKey` from `STRIPE_API_KEY`, wrapped in Resilience4j retry +
  circuit breaker + time limiter (`resilience4j.*.instances.gateway.*`). Card exceptions map to a DECLINED result; other
  Stripe errors become a wrapped `GatewayException`.
- **Kafka topics** (`config/KafkaTopicConfig`): `authorization.events` (6 partitions) for the payment-authorization
  context, `authorization.access.events` (3 partitions) for the RBAC context. Published via `EventPublisher` (`@Async`,
  failures logged not propagated) — fire-and-forget, not part of the request's transactional guarantee.
- **Inbound**: gateway-service routes `/api/v1/authorizations/**`, `/api/v1/access/**`, `/api/v1/admin/**` to this
  service, with a per-route Redis rate limiter, circuit breaker, and retry(2). Note: the gateway's default fallback URI
  is `http://authorization-service:8080` — a Docker/Eureka hostname that doesn't literally match this service's actual
  port 8086; real deployments rely on `AUTHORIZATION_SERVICE_URI` being set or Eureka service discovery resolving the
  port correctly.
- No other in-repo service was found calling this service's HTTP API directly in Java code — access-control checks from
  other services, if any, would need to go through the gateway or Eureka client, not a hardcoded client class.

## Running locally

```
./gradlew :authorization-service:bootRun --args='--spring.profiles.active=local'
```

Key env vars (defaults from `application.yml` / `application-local.yml`):

- `SERVER_PORT` implicit 8086
- Postgres: local profile targets `authorization_local` at `localhost:5432` (user/pass `postgres`/`postgres`)
- `STRIPE_API_KEY` (empty by default — logs a warning if unset; Stripe calls will fail without it)
- `JWT_HMAC_SECRET` / `JWT_PUBLIC_KEY` / `JWT_ISSUER` — local profile disables signature verification (
  `security.jwt.verify-signature: false`) for convenience
- Kafka `bootstrap-servers` (default `localhost:9092`)
- Eureka registration is disabled under the `local` profile

Requires PostgreSQL and (for event publishing) Kafka running locally; Flyway applies migrations automatically on boot.

## Design notes

- **Two bounded contexts, one deployable, by design or by convenience** — worth calling out explicitly since it's the
  most surprising structural fact about this service: payment authorization (external, Stripe-backed, PCI-adjacent) and
  RBAC/ABAC (internal, platform-wide access control) share nothing but infrastructure. A reader expecting "
  authorization-service" to mean either "card auth" or "access control" alone will be wrong; it's both.
- **Idempotency mirrors payment-service's pattern** (dedicated `idempotency_key` table keyed on request hash),
  suggesting a deliberate platform-wide convention for safely retrying mutating POSTs rather than a one-off.
- **Fail-fast fire-and-forget events**: `EventPublisher` logs but swallows Kafka publish failures rather than blocking
  or rolling back the originating request — consistent with treating events as best-effort notifications, not as part of
  the authorization decision's correctness guarantee.
- **Hierarchical roles via self-referencing `parent`** let `MERCHANT_OWNER` inherit all of `MERCHANT_ADMIN`'s
  permissions plus `role:manage`, avoiding duplicated permission grants in the seed data — a reasonable RBAC design
  choice, though it also means permission resolution must walk the parent chain rather than being a flat lookup.
