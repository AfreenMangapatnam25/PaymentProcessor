# Postman — automatic JWT, no token generation by hand

**Short answer: yes, it's possible.** A collection-level pre-request script logs in once,
caches the token, and silently re-authenticates when it's about to expire. You never paste
a token, and no code generates one for you.

Files in this folder:

| File                                        | Purpose                                                       |
|---------------------------------------------|---------------------------------------------------------------|
| `PaymentProcessor.postman_collection.json`  | The collection, with the auto-token script and 25 requests    |
| `PaymentProcessor.postman_environment.json` | Base URLs, credentials, and the variables the script fills in |

---

## Quick start

1. **Import both files** into Postman (*Import* → drag both in).
2. Select the **PaymentProcessor - Local** environment (top-right dropdown).
3. Start `authentication-service` (:8081) plus whichever services you want to hit:

   ```bash
   cd authentication-service && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
   ```

4. **Run any request.** The token is fetched automatically.

To see it happen, open Postman's **Console** (`View → Show Postman Console`, or `Ctrl/Cmd+Alt+C`).
You'll see:

```
[auth] no usable token cached - authenticating
[auth] token stored, expires in 900s
[auth] reusing cached token
[auth] reusing cached token
```

---

## How the script works

It lives on the **collection** (right-click collection → *Edit* → *Pre-request Script*), so it
runs before **every** request. The collection's *Authorization* tab is set to
`Bearer {{accessToken}}`, which every request inherits — that's why no individual request
declares auth.

```
                    ┌─────────────────────────────┐
  every request ───▶│ jwtEnabled = false?         │──yes──▶ skip, send unauthenticated
                    └──────────┬──────────────────┘
                               │ no
                    ┌──────────▼──────────────────┐
                    │ cached token still valid?   │──yes──▶ reuse it (costs nothing)
                    │ (reads the JWT's own `exp`) │
                    └──────────┬──────────────────┘
                               │ no / expiring
                    ┌──────────▼──────────────────┐
                    │ have a refresh token?       │──yes──▶ POST /auth/refresh
                    └──────────┬──────────────────┘         (no password sent)
                               │ no / refresh failed
                    ┌──────────▼──────────────────┐
                    │ POST /auth/login            │
                    └─────────────────────────────┘
```

Details worth knowing:

- **Expiry comes from the token, not the clock.** The script base64-decodes the JWT payload and
  reads `exp`, rather than trusting the `expiresIn` field. A server-side TTL change or clock
  skew can't leave a stale token cached.
- **Re-auth happens `tokenSkewSeconds` early** (default 60) so a token never expires mid-request.
- **Recursion guard.** Requests whose path starts `/api/v1/auth/` are skipped, otherwise the
  login call would re-enter the script forever.
- **Password is sent once.** After the first login the refresh-token grant is preferred.
  Verified: a 25-request run produces `login=1, refresh=N`.

---

## Environment variables

| Variable                                     | Default                                | Meaning                                                                                                |
|----------------------------------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------|
| `authUrl`                                    | `http://localhost:8081`                | authentication-service base URL                                                                        |
| `authEmail`                                  | `postman.admin@paymentprocessor.local` | Account the script logs in as                                                                          |
| `authPassword`                               | `DevPassw0rd!2026`                     | Seeded dev password (local profile only)                                                               |
| `jwtEnabled`                                 | `true`                                 | Set `false` when services run with `SECURITY_JWT_ENABLED=false` — the script then skips login entirely |
| `tokenSkewSeconds`                           | `60`                                   | Re-authenticate this long before actual expiry                                                         |
| `accessToken` / `refreshToken` / `sessionId` | *(blank)*                              | **Filled in by the script.** Leave empty                                                               |
| `<service>Url`                               | `http://localhost:80xx`                | One per service                                                                                        |
| `auditApiKey`                                | `local-dev-key`                        | audit-service also gates `/api/*` on an API key                                                        |
| `merchantAdminApiKey`                        | `admin_local_dev_key_change_me`        | merchant-service accepts this *instead of* a JWT                                                       |

### Which account, and why

The script authenticates as `postman.admin@paymentprocessor.local` — an **ADMIN identity with
no MFA factor**, seeded specifically for automation. That matters: an identity with MFA
enabled returns `MFA_REQUIRED` and a challenge instead of tokens, which cannot be completed
from a script without a TOTP code.

Seeded accounts (all share the password `DevPassw0rd!2026`):

| Email                                  | Type  | MFA | Login result                    |
|----------------------------------------|-------|-----|---------------------------------|
| `postman.admin@paymentprocessor.local` | ADMIN | no  | tokens ✅ *(used by the script)* |
| `alice@example.com`                    | USER  | no  | tokens ✅                        |
| `admin@paymentprocessor.local`         | ADMIN | yes | `MFA_REQUIRED` challenge        |
| `owner@merchant-demo.com`              | USER  | yes | `MFA_REQUIRED` challenge        |
| `locked.user@example.com`              | USER  | —   | rejected (locked)               |
| `pending.user@example.com`             | USER  | —   | unverified-email path           |

> These passwords are **real Argon2id hashes** in
`authentication-service/src/main/resources/db/seed/V3__seed_sample_data.sql`,
> generated with the same parameters as `Argon2PasswordEncoder(16, 32, 1, 16384, 3)`. They were
> previously placeholder strings, which is why login didn't work before. The seed only loads under
> the `local` profile — a shared, publicly-known password must never reach an environment with real data.

---

## What's in the collection

**One folder per service, 383 requests** — every endpoint documented in each service's
`API_TESTING.md`, with its sample request body already filled in.

```
PaymentProcessor - Auto JWT
├── 00 - Token Setup            5    diagnose auth problems
├── UserService                22    :8082
├── PaymentService             13    :8087
├── MerchantService            52    :8083
├── AuthenticationService      31    :8081
├── AuthorizationService       10    :8086
├── TokenizationService        35    :8084
├── LimitService               12    :8085
├── FraudService               31    :8088
├── ClearingService            16    :8089
├── DisputeService             23    :8090
├── SettlementService          28    :8091
├── LedgerService              32    :8092
├── ReconciliationService      23    :8093
├── NotificationService        27    :8094
├── AuditService                7    :8095
├── ReportingService           12    :8096
└── 99 - Auth behaviour         4    negative tests
```

### Running one service at a time

Each folder targets only its own base URL, so you can start a single service and run just
its folder — right-click the folder → **Run folder**. Folders for services you haven't
started fail with a clear *"No response — is X running on port Y?"* rather than a confusing
`TypeError`.

Every request carries two assertions: no `5xx`, and not `401/403`. That's deliberately loose —
the point is to confirm the endpoint is reachable and authenticated, not to pin down exact
response bodies you may still be changing.

### Path parameters

Paths like `/api/v1/users/{userId}` become `:userId` in Postman. Fill them in the request's
**Path Variables** table (just under the URL bar). Seeded ids are prefilled where the meaning
was obvious — `merchantId`, `customerId`, `userId`, `addressId`, `disputeId`, `accountId`.

### The two utility folders

**`00 - Token Setup`** — you don't normally run these; the script does it for you. Use them to
diagnose a 401: the Login request decodes and asserts the token's claims, so it's the fastest
way to see what you're actually getting.

**`99 - Auth behaviour`** — negative tests proving security rejects what it should: no token,
malformed token, and **a refresh token presented as an access token**, which is exactly what
the `purpose=access` validator exists to stop. These auto-skip when `jwtEnabled=false`.

---

## Two ways to run: auth on or off

**Auth off (easiest).** The `local` profile sets `security.jwt.enabled=false`, so services use
a permit-all chain and the seeded-data examples work with no token at all:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Set `jwtEnabled=false` in the Postman environment to match, and the script won't even try to log in.

**Auth on (realistic).** Exercises the actual security path:

```bash
SPRING_PROFILES_ACTIVE=local SECURITY_JWT_ENABLED=true ./gradlew bootRun
```

Leave `jwtEnabled=true`. Every service now validates tokens against
`http://localhost:8081/.well-known/jwks.json`, so **authentication-service must be running**.

---

## Running from the CLI (Newman / CI)

```bash
npm install -g newman

newman run postman/PaymentProcessor.postman_collection.json \
  -e postman/PaymentProcessor.postman_environment.json
```

Useful flags:

```bash
# one folder only
--folder "01 - Smoke tests (one authenticated call per service)"

# point at a deployed environment without editing the file
--env-var "authUrl=https://auth.staging.example.com" \
--env-var "authPassword=$CI_TEST_PASSWORD"

# JUnit XML for CI
--reporters cli,junit --reporter-junit-export results.xml
```

Newman exits non-zero if any assertion fails, so it drops straight into a pipeline.

**Never commit real credentials.** Pass them as `--env-var` from your CI secret store.

---

## Troubleshooting

| Symptom                                   | Cause                                                                                      |
|-------------------------------------------|--------------------------------------------------------------------------------------------|
| `[auth] authEmail / authPassword not set` | No environment selected — pick *PaymentProcessor - Local*                                  |
| `identity ... requires MFA`               | `authEmail` points at an MFA-enabled account; use `postman.admin@…` or `alice@example.com` |
| `login returned HTTP 401`                 | Seed didn't load. Start with `SPRING_PROFILES_ACTIVE=local` so `db/seed` runs              |
| `login returned HTTP 404`                 | `authUrl` wrong, or authentication-service isn't running                                   |
| *"No response — is X running?"*           | That service isn't started — check its `<service>Url`                                      |
| `getaddrinfo ENOTFOUND {{somename}}`      | Variable name typo. Names are **case-sensitive**: `{{paymentUrl}}`, not `{{paymenturl}}`   |
| Requests 401 with a token present         | Service can't reach the JWKS. Confirm :8081 is up and `JWKS_URI` is correct                |
| Everything 401 after a code change        | Token cached from an older key. Clear `accessToken` in the environment                     |
| Endpoint 404s with `:userId` in the URL   | Path variable not filled — set it in the **Path Variables** table under the URL bar        |

---

## Verification

Static checks on the generated collection:

```
JSON schema          v2.1.0, valid
test scripts         383 checked, 0 syntax errors
request bodies       117 present, 0 invalid JSON
url variables        0 missing from the environment
```

Executed against a mock harness reproducing the real contract (RS256 tokens,
`purpose=access` validation, the `LoginResponse` shape, one shared JWKS across services):

| Scenario | Result |
|---|---|
| `UserService` folder, only :8081 + :8082 up | 23 requests, 44 assertions, **0 failed** |
| `PaymentService` folder, only :8087 up | 14 requests, 26 assertions, **0 failed** |
| `LedgerService` folder, :8092 **down** | fails cleanly — *"No response — is it running?"* |

Each branch of the token script was exercised deliberately:

- **cold start** (empty `accessToken`) → password login, token stored
- **warm** → `reusing cached token`, no network call
- **near expiry** (token TTL 50s < 60s skew) → `login=1, refresh=14`, i.e. the password went
  over the wire exactly once
- **negative tests** → 401s confirmed, including refresh-token-as-access-token

> The mock stands in for the Spring services, which couldn't be started here (this environment
> has JDK 11; Gradle 8.14 needs 17+). It validates the **collection and its scripts**, not the
> services themselves — run the smoke folder against your real services to confirm those.
