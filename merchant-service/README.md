# Merchant Service

Manages merchant onboarding, KYB (know-your-business) case tracking, and merchant configuration (API keys, webhooks,
fees, payment methods, settlement accounts) for the platform.

## Role in the Platform

- Owns the merchant system of record: profile, business address, beneficial owners, branding, and configuration.
- Tracks merchant onboarding status via `Merchant.kybStatus`, driven by `KybCase` records — actual sanctions/PEP/credit
  screening is performed by an external system, not inside this service.
- Issues, hashes, and rotates API keys used by merchants to authenticate against the platform (`ApiKeyController` /
  `ApiKeyService`).
- Manages webhook subscriptions and their HMAC secrets, though outbound webhook delivery is not actually implemented
  yet (see Design Notes).
- Publishes merchant domain events to Kafka via a transactional outbox (`OutboxRelay`), rather than direct
  produce-on-write.
- Acts as a read dependency for other services (e.g. `settlement-service` looks up merchants and settlement accounts
  over HTTP); it makes no outbound HTTP calls of its own.

## Tech Stack

- Spring Boot 3.3.2, Java 21
- Server port: `8083` (`server.port: ${SERVER_PORT:8083}` in `application.yml`)
- Datastore: PostgreSQL only (`jdbc:postgresql://localhost:5432/merchant`); no Redis
- Flyway: enabled, migrations own the schema (`hibernate.ddl-auto=validate`)
- Kafka: spring-kafka producer, idempotent, `acks=all`, retries=10, bootstrap `localhost:9092`
- Other notable libs: spring-data-jpa, spring-security, springdoc-openapi 2.6.0, micrometer-prometheus, jackson-jsr310,
  Lombok
- No spring-webflux/WebClient dependency — this service does not call other services

## API Surface

All endpoints are rooted at `/api/v1/merchants`. Controllers:

- **MerchantController** — merchant CRUD and profile management
- **AddressController** — business address management
- **BeneficialOwnerController** — beneficial owner records for KYB
- **KybController** — KYB case/document management and status transitions
- **SettlementAccountController** — merchant settlement (payout) accounts
- **FeeController** — fee configuration per merchant
- **PaymentMethodController** — enabled payment methods per merchant
- **WebhookController** — webhook subscription CRUD, including a `testDelivery` action (see Design Notes — this does not
  send a real HTTP request)
- **ApiKeyController** — API key issuance and rotation
- **MerchantConfigurationController** — general merchant configuration
- **BrandingController** — merchant branding assets

Non-obvious/notable endpoints:

- KYB case/document endpoints on `KybController` drive `Merchant.kybStatus` transitions as decisions are recorded.
- `ApiKeyController` exposes a rotate action that revokes the current key and issues a new one (hash stored, plaintext
  returned once).
- `WebhookController`'s test-delivery endpoint only records a `PENDING` `WebhookDeliveryLog` row — it does not perform
  an actual HTTP callback.

## Data Model

Key entities:

- `Merchant` — core profile, holds `MerchantStatus`, `KybStatus`, and `PricingPlan` enums
- `KybCase` / `KybDocument` — onboarding case and supporting documents; case decisions update `Merchant.kybStatus`
- `BeneficialOwner`, `BusinessAddress` — KYB-related merchant detail records
- `SettlementAccount` — payout destination(s) for a merchant
- `FeeConfiguration`, `PaymentMethodConfig` — per-merchant billing/payment settings
- `ApiKey` — hashed API key records with rotation history
- `Webhook`, `WebhookDeliveryLog` — subscription config (including HMAC secret) and delivery attempt log; `Webhook` has
  `maxRetries`/`timeoutSeconds` fields that are currently unused by any dispatcher
- `MerchantConfiguration`, `MerchantBranding` — misc configuration and branding

### KYB state machine

The KYB workflow is real but delegated: `KybService` persists `KybCase` records and, as decisions are recorded against a
case, updates `Merchant.kybStatus` accordingly. The service does not itself perform sanctions/PEP/credit screening —
that is expected to happen in an external system, with results fed back in as case decisions.

## Inter-Service Integration

- **Outbound**: none. Merchant-service has no WebClient dependency and makes no calls to other services.
- **Inbound**: `settlement-service` calls this service over HTTP (`WebClientMerchantClient`, default
  `MERCHANT_SERVICE_URL=http://localhost:8083`) for `GET /api/v1/merchants/{id}` and settlement-account lookups.
  `gateway-service` routes `/api/v1/merchants/**` to this service. `notification-service` consumes merchant-related
  events.
- **Kafka**: producer-only, via a scheduled `OutboxRelay` that polls an outbox table and publishes to a single topic,
  `merchant.events`. No `@KafkaListener` was found in this service.

## Running Locally

```
./gradlew.bat :merchant-service:bootRun
```

Key environment variables (see `application.yml` for full defaults):

- `SERVER_PORT` (default `8083`)
- PostgreSQL connection settings (default `jdbc:postgresql://localhost:5432/merchant`)
- Kafka `bootstrap-servers` (default `localhost:9092`)

Flyway migrations run automatically on startup against the configured Postgres database.

## Design Notes

- **KYB is intentionally a thin state tracker, not a screening engine.** `KybCase`/`KybDocument` model the workflow and
  `Merchant.kybStatus` reflects the outcome, but the service assumes an external system performs the actual checks and
  reports decisions back in.
- **API key rotation is real and reasonably careful**: keys are generated with `SecureRandom`, stored hashed (never in
  plaintext), and rotation revokes the old key before issuing a new one.
- **Webhook delivery is a stub.** HMAC secret generation and subscription CRUD are implemented, but there is no code
  that actually performs an HTTP POST to a subscriber, no retry/backoff logic, and no scheduled dispatcher — despite
  `maxRetries`/`timeoutSeconds` existing as unused fields on `Webhook`. The `testDelivery` endpoint only writes a
  `PENDING` `WebhookDeliveryLog` row. Anyone relying on outbound webhooks from this service today should treat it as
  not-yet-implemented.
- **Events are relayed via a transactional outbox** (`OutboxRelay`) rather than produced synchronously on write,
  avoiding dual-write inconsistency between Postgres and Kafka — but as a consequence, event delivery is only as timely
  as the relay's poll interval.

*Note: an existing `MerchantReadme.md` in this directory contains some inaccuracies (e.g. it states Java 17 instead of
the actual Java 21, and describes webhook retry/backoff as a working feature) — this README was written from and
verified against the current source.*
