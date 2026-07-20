# Authentication Service

Central identity provider for the payment platform — authenticates users/merchants/admins/services, issues and rotates
JWT/refresh tokens, and enforces password, MFA, device, and lockout policy.

## Role in the platform

- Issues short-lived RS256-signed JWT access tokens plus rotating opaque refresh tokens; downstream services (starting
  with `gateway-service`) verify tokens locally against this service's published JWKS instead of calling back on every
  request.
- Publishes its public signing key at `GET /.well-known/jwks.json` so resource servers can validate signatures without a
  shared secret.
- Owns password lifecycle (change/forgot/reset), TOTP-based MFA enrollment/verification/recovery codes, and
  trusted-device management.
- Enforces account lockout after repeated failed logins, tracking failure counts on the `Identity` record and
  auto-unlocking after a cooldown.
- Issues and validates service-to-service API keys (mint/list/revoke/verify) for machine callers.
- Publishes domain events (login, logout, password change, MFA enabled, account locked, new device) via a transactional
  outbox relay to Kafka, rather than calling other services directly — the service makes no outbound WebClient calls to
  other microservices.

## Tech stack

- Spring Boot 3.3.2, Java 21 (Gradle toolchain)
- PostgreSQL as the datastore (`org.postgresql:postgresql`), no Redis
- Flyway-managed schema (`ddl-auto: none`), migrations `V1__baseline.sql` (identities, credentials, refresh_tokens,
  api_keys, api_key_scopes, mfa_factors, mfa_recovery_codes, login_attempts, devices, password_reset_tokens,
  verification_tokens) and `V2__outbox.sql` (outbox_events)
- JWT/JWKS via Nimbus JOSE+JWT (not jjwt), RS256/2048-bit RSA
- Argon2id password hashing (Spring Security `Argon2PasswordEncoder`)
- Caffeine for local in-memory rate limiting
- Spring Kafka (producer only, via transactional outbox)
- Default port **8081** (`server.port: ${SERVER_PORT:8081}`) — note: the gateway's route/JWKS URI config defaults to
  `http://authentication-service:8080`, an inconsistency likely resolved by `SERVER_PORT` override in deployment; worth
  confirming when wiring environments.

## API surface

All controllers live under `com.paymentprocessor.authenticationservice.controller`:

- `AuthController` (`/api/v1/auth`) — `login`, `login/mfa`, `refresh` (rotates refresh token, family/replaced-by chain),
  `logout`, `logout-all`, `token/introspect`
- `JwksController` (root) — `GET /.well-known/jwks.json`, public, exposes the RSA public key set for token verification
- `MfaController` (`/api/v1/mfa`) — enroll, verify, disable, recovery-codes, list
- `PasswordController` (`/api/v1/passwords`) — change, forgot, reset
- `DeviceController` (`/api/v1/devices`) — list, trust, revoke
- `ApiKeyController` (`/api/v1/api-keys`) — mint (admin), list, revoke, and a public `verify` endpoint used by other
  services for API-key auth
- `AdminIdentityController` (`/api/v1/admin/identities`) — register, get, lock, unlock, disable
- `VerificationController` (`/api/v1/verification`) — email/phone start + confirm

Non-obvious endpoints: the JWKS endpoint is unauthenticated by design (it only serves public keys); `/auth/refresh`
rotates the refresh token rather than just reissuing an access token; `/auth/token/introspect` lets callers
validate/inspect a token server-side; `/admin/identities/*` lock/unlock endpoints are the operational lever for the
lockout policy.

## Data model — key entities and business rules

- `Identity` carries `failed_login_count`, `locked_until`, and `status`; lockout trips at `LOCKOUT_MAX_ATTEMPTS` (
  default **5**) and locks for `LOCKOUT_DURATION` (default `PT15M`). A `LOCKOUT_WINDOW` (default `PT15M`) config key
  exists but the trip logic uses the persistent failure counter rather than a true sliding window — a discrepancy to be
  aware of if tightening the policy.
- `LoginAttempt` records an audit trail of login attempts independent of the lockout counter.
- `RefreshToken` rows are opaque, hashed at rest, and rotated on every use; reuse of a revoked token revokes the whole
  token family.
- `ApiKey`/`ApiKeyScope` back service-to-service authentication.
- `MfaFactor`/`MfaRecoveryCode` back TOTP enrollment and recovery.
- `OutboxEvent` (from `V2__outbox.sql`) backs the transactional outbox relay described below.

## Inter-service integration

- **Outbound**: none — no `WebClient` usage was found anywhere in `authentication-service/src`; this service does not
  call other microservices directly.
- **Inbound (gateway)**: `gateway-service`'s `application.yml` defines a route with `id: authentication-service`
  proxying `/api/v1/auth/**`, `/api/v1/mfa/**`, `/api/v1/passwords/**`, `/api/v1/devices/**`, `/api/v1/api-keys/**`,
  `/api/v1/verification/**`, and `/api/v1/admin/identities/**`, with rate limiting, a circuit breaker, and retry
  filters. The gateway also fetches this service's JWKS (
  `jwk-set-uri: ${JWKS_URI:http://authentication-service:8080/.well-known/jwks.json}`) to validate tokens locally.
- **Inbound (other services)**: no other service makes direct HTTP calls into authentication-service today; coupling is
  indirect — `user-service`'s `application.yml` binds claim-name config to interpret JWTs minted here, and
  `notification-service` has a code comment referencing the `auth.account.locked` topic but no wired Kafka consumer yet.
- **Kafka topics produced** (via the outbox relay, all overridable via `TOPIC_*` env vars): `auth.user.logged-in`,
  `auth.user.logged-out`, `auth.password.changed`, `auth.mfa.enabled`, `auth.account.locked`, `auth.device.new-login`.
  No `@KafkaListener` consumers exist in this service.

## Running locally

Key environment variables (see `application.yml`):

| Variable                             | Default                                            | Purpose                   |
|--------------------------------------|----------------------------------------------------|---------------------------|
| `SERVER_PORT`                        | `8081`                                             | HTTP port                 |
| `DB_URL`                             | `jdbc:postgresql://localhost:5432/authdb`          | Postgres connection       |
| `DB_USERNAME` / `DB_PASSWORD`        | `auth` / `auth`                                    | Postgres credentials      |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | *(blank → ephemeral keypair generated at startup)* | RS256 PEM material        |
| `JWT_KEY_ID`                         | *(computed JWK thumbprint if unset)*               | JWKS `kid`                |
| `JWT_ISSUER`                         | `https://auth.paymentprocessor.local`              | token `iss`               |
| `JWT_AUDIENCE`                       | `payment-platform`                                 | token `aud`               |
| `JWT_ACCESS_TTL`                     | `PT15M`                                            | access token lifetime     |
| `REFRESH_TTL`                        | `P30D`                                             | refresh token lifetime    |
| `LOCKOUT_MAX_ATTEMPTS`               | `5`                                                | failed logins before lock |
| `LOCKOUT_DURATION`                   | `PT15M`                                            | lockout cooldown          |

Run with:

```
./gradlew.bat :authentication-service:bootRun
```

Requires a reachable Postgres instance (Flyway will apply migrations on startup) and, optionally, a Kafka broker for
outbox relay delivery.

## Design notes

- **Transactional outbox instead of dual-write**: business state changes and event staging (`outbox_events`) commit in
  the same database transaction; a separate scheduled `OutboxRelay` claims `PENDING` rows with `FOR UPDATE SKIP LOCKED`
  and publishes them to Kafka at-least-once, so a crash between a DB commit and a Kafka send can't lose or duplicate an
  event without a retry path.
- **No live JWT key rotation**: signing keys come from `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` env vars, or an ephemeral
  in-memory RSA keypair generated at startup if unset. There's no dual-key overlap or scheduled rotation mechanism —
  rotating the key today means redeploying with new PEM values, which invalidates all outstanding refresh sessions
  signed under the old `kid`.
- **JWKS-based trust instead of a shared secret**: because tokens are RS256-signed and the public key is served at
  `/.well-known/jwks.json`, resource servers (like the gateway) verify tokens locally after an initial JWKS fetch,
  avoiding a synchronous call back to this service on every request.
- **Lockout counter vs. configured window**: `LOCKOUT_MAX_ATTEMPTS`/`LOCKOUT_DURATION` are enforced against a persistent
  failure counter on `Identity`, but the also-configured `LOCKOUT_WINDOW` isn't wired into the trip decision — the
  counter doesn't currently reset on a sliding window, only on lock expiry or a successful login/reset.
