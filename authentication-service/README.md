# Authentication Service

Central identity provider for the payment platform. Authenticates users, merchants,
admins and services; issues **RS256 JWT access tokens** plus rotating opaque refresh
tokens; manages passwords, MFA, devices, account lockout and API keys; and publishes
domain events to Kafka for the Audit, Notification and Risk services.

The domain specification lives in [`AuthenticationReadme.md`](./AuthenticationReadme.md).
This file covers building, running and integrating the service.

## Tech stack

Java 21 · Spring Boot 3.3 · Spring Security · Spring Data JPA · PostgreSQL · Flyway ·
Spring Kafka · Nimbus JOSE+JWT (RS256/JWKS) · Argon2id (BouncyCastle) · Caffeine ·
springdoc OpenAPI.

## How the gateway trusts this service

Access tokens are signed with a private RSA key. This service publishes the matching
**public** key at `GET /.well-known/jwks.json`. The `gateway-service` (and any other
resource server) fetches that JWK set once, caches it by `kid`, and verifies every
incoming token locally — no shared secret and no per-request call back here. The
gateway should validate `iss`, `aud`, `exp` and the signature, then read `identity_id`,
`principal_type` and `scope` from the claims to perform its own authorization.

Access-token claims:

```
sub / identity_id   the authenticated identity id
principal_type      USER | MERCHANT | ADMIN | SERVICE
scope               space-delimited scopes
sid                 refresh-token family (session) id
amr                 methods used, e.g. ["pwd","mfa"]
purpose             "access" (resource filter rejects anything else)
iss, aud, iat, nbf, exp, jti
```

## Prerequisites

- JDK 21
- PostgreSQL 14+ (a database the service can migrate with Flyway)
- Kafka broker (events are best-effort; the service still runs if the broker is down)

## Configuration

All settings are environment-driven; see [`.env.example`](./.env.example). Key ones:

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local Postgres | datasource |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | event broker |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | *(blank)* | RS256 PEM material |
| `JWT_ISSUER` / `JWT_AUDIENCE` | see file | token `iss` / `aud` |
| `JWT_ACCESS_TTL` / `REFRESH_TTL` | `PT15M` / `P30D` | token lifetimes |
| `LOCKOUT_MAX_ATTEMPTS` / `LOCKOUT_DURATION` | `5` / `PT15M` | brute-force protection |

> If `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` are blank an **ephemeral** keypair is generated
> at startup (tokens die on restart). Fine for local dev, never for production.

### Generating an RS256 keypair

```bash
# Private key (PKCS#8) and matching public key
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out auth-private.pem
openssl rsa -pubout -in auth-private.pem -out auth-public.pem

# Export as single-line env values (strip headers/newlines)
export JWT_PRIVATE_KEY=$(grep -v -- '-----' auth-private.pem | tr -d '\n')
export JWT_PUBLIC_KEY=$(grep -v -- '-----' auth-public.pem | tr -d '\n')
export JWT_KEY_ID=auth-key-1
```

## Build & run

```bash
./gradlew build            # compile + run tests
./gradlew bootRun          # start on :8081 (uses .env / exported vars)
# or
java -jar build/libs/authentication-service-1.0.0.jar
```

Flyway applies `src/main/resources/db/migration/V1__baseline.sql` on startup.
Hibernate runs with `ddl-auto=none` — **Flyway is the single source of truth** for the
schema. Add new changes as `V2__*.sql`, `V3__*.sql`, … (never edit an applied migration).

Swagger UI: `http://localhost:8081/swagger-ui.html`

## API surface

Base path `/api/v1`. Public endpoints need no token; the rest require
`Authorization: Bearer <access token>`; `/admin/**` requires an `ADMIN` token.

### Public
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/auth/login` | password login → tokens or MFA challenge |
| POST | `/auth/login/mfa` | complete login with a second factor |
| POST | `/auth/refresh` | rotate refresh token → new access token |
| POST | `/auth/logout` | revoke a refresh token |
| POST | `/auth/token/introspect` | validate/inspect an access token |
| POST | `/passwords/forgot` | request a reset (no account enumeration) |
| POST | `/passwords/reset` | reset with a reset token |
| POST | `/verification/email/confirm` · `/verification/phone/confirm` | confirm ownership |
| POST | `/api-keys/verify` | service-to-service API-key validation |
| GET | `/.well-known/jwks.json` | public JWK set for token verification |

### Authenticated
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/auth/logout-all` | revoke every session for the caller |
| POST | `/passwords/change` | change password (re-auth + session reset) |
| POST | `/mfa/enroll` · `/mfa/verify` · `/mfa/disable` | TOTP factor lifecycle |
| POST | `/mfa/recovery-codes` | (re)generate recovery codes |
| GET | `/mfa` | list active MFA methods |
| POST | `/verification/email/start` · `/verification/phone/start` | begin verification |
| GET | `/devices` · POST `/devices/{id}/trust` · DELETE `/devices/{id}` | device trust |

### Admin (`ADMIN` token)
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/identities` | provision an identity + password |
| GET | `/admin/identities/{id}` | fetch identity |
| POST | `/admin/identities/{id}/lock` · `/unlock` · `/disable` | status control |
| POST · GET · DELETE | `/api-keys` | mint / list / revoke service API keys |

### Example: login

```bash
curl -sX POST localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"Sup3rSecret!!","deviceFingerprint":"fp-1"}'
```

`AUTHENTICATED` returns `tokens`; `MFA_REQUIRED` returns a `challenge.mfaToken` to POST
to `/auth/login/mfa` with the TOTP code.

## Security model

- **Passwords**: Argon2id hashing; configurable complexity + history; never stored or
  returned in plaintext.
- **Refresh tokens**: opaque, SHA-256 at rest, **rotated on every use**. Replaying a
  revoked token revokes the whole token family (stolen-token defence).
- **Lockout**: after N failed logins the account locks for a cooldown; `AccountLocked`
  is published; a password reset also clears the lock.
- **MFA**: RFC 6238 TOTP with recovery codes; a password-only success yields a
  short-lived `mfa` ticket that the resource filter refuses to accept as an access token.
- **Rate limiting**: in-memory per email+IP for login and per IP for reset. For a
  multi-instance deployment back this with a shared store (e.g. Redis).
- **Event delivery**: transactional outbox (see below) — no dual-write between the
  database and Kafka.

## Domain events — Transactional Outbox + Kafka

| Event | Default topic |
|-------|---------------|
| `UserLoggedIn` | `auth.user.logged-in` |
| `UserLoggedOut` | `auth.user.logged-out` |
| `PasswordChanged` | `auth.password.changed` |
| `MFAEnabled` | `auth.mfa.enabled` |
| `AccountLocked` | `auth.account.locked` |
| `NewDeviceLogin` | `auth.device.new-login` |

Events are **not** written directly to Kafka (that would be a dual-write: the DB
commit and the Kafka send are separate, so a crash between them loses or fabricates
events). Instead this service uses the **Transactional Outbox** pattern:

```
Service method  (@Transactional)
  ├─ business change      -> Postgres          ┐  one atomic
  └─ events.publish(...)  -> outbox_events row  ┘  commit

OutboxRelay  (@Scheduled, separate tx)
  └─ claim PENDING rows (FOR UPDATE SKIP LOCKED) -> send to Kafka -> mark PUBLISHED
```

Because the event row and the state change commit in the **same transaction**, they
are all-or-nothing — the dual-write window is gone. The relay then delivers to Kafka
**at least once**, retrying on failure (`attempts`/`max-attempts`, then `FAILED` for a
dead-letter sweep). `SKIP LOCKED` lets every service instance run the relay without
double-sending. Each event carries a unique `eventId`, so **consumers must be
idempotent** (dedupe on `eventId`).

Relay tuning (env): `OUTBOX_RELAY_ENABLED`, `OUTBOX_BATCH_SIZE`, `OUTBOX_MAX_ATTEMPTS`,
`OUTBOX_POLL_MS`. Set `OUTBOX_RELAY_ENABLED=false` on instances that should stage but
not relay (e.g. if you dedicate one node to relaying).

## Tests

```bash
./gradlew test
```

Covers TOTP generation/verification, password policy, JWT issue/verify + tamper
rejection, refresh-token rotation & reuse detection, and an end-to-end login flow
(MockMvc + H2, Kafka publisher mocked). Integration tests run against in-memory H2 with
Hibernate-generated schema; production runs on Postgres via Flyway.

## Notes / next steps

- WebAuthn/FIDO2 and SMS/email OTP factors are modelled (`MfaKind`) but only TOTP is
  wired end-to-end in this pass.
- Verification/reset raw tokens are returned to the caller layer for delivery via the
  Notification Service and are only stored hashed.
- MFA/TOTP secrets are stored as `secret_ref`; wrap them with envelope encryption (KMS)
  before production.
