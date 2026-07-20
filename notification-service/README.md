# notification-service

Records platform domain events and delivers outbound merchant notifications (webhooks, email, SMS), with
suppression/bounce handling and versioned templates.

## Role in the platform

- Ingests domain events (`POST /api/events`) from upstream services and durably records them in a partitioned `events`
  table, keyed by merchant.
- Fans each ingested event out, in the same transaction, to every active `WebhookEndpoint` subscribed to that event
  type, creating `webhook_deliveries` rows in `pending` state.
- Runs an internal dispatcher/scheduler that delivers pending webhooks over HTTP with HMAC-signed payloads, exponential
  backoff, and auto-disable of endpoints after repeated consecutive failures.
- Sends templated email (SendGrid) and SMS (Twilio) messages on request (`POST /api/messages`), rendering versioned
  templates and honoring a per-channel suppression list.
- Maintains a suppression list fed by SendGrid Event Webhook and Twilio status-callback receivers, so
  bounced/complained/unsubscribed recipients stop receiving further messages.
- As of today, also consumes merchant-service's Kafka outbox topic directly (see Inter-service integration below) — its
  first inbound Kafka wiring.

## Tech stack

- Spring Boot 3.3.2, Java 21 toolchain declared at the platform level (module itself targets Java 17 source/target
  compatibility per `build.gradle`).
- Default port: **8094** (`server.port`, overridable via `SERVER_PORT`).
- Datastore: PostgreSQL (`notification_service` DB), via Spring Data JPA/Hibernate (`ddl-auto: validate` — schema is
  migration-owned, not Hibernate-owned).
- Flyway: enabled, migrations in `src/main/resources/db/migration` (currently `V1__init_schema.sql`).
- Messaging: `spring-kafka` for the new inbound consumer.
- Outbound providers: `sendgrid-java` (email), `twilio` SDK (SMS).
- Auth: a lightweight custom `ApiKeyAuthFilter` (not Spring Security) requiring header `X-Internal-Api-Key` on all
  `/api/**` calls when `notification.security.internal-api-key` is set; health/metrics stay open.
- Test stack: Testcontainers (Postgres) + Awaitility for integration tests.

## API surface

All endpoints below are under `/api/**` and require `X-Internal-Api-Key` (when the key is configured):

- `POST /api/events`, `GET /api/events?merchantId=&limit=`, `GET /api/events/{id}` — event ingestion/lookup (
  `EventController`). This is the same path both external callers and the new `MerchantEventConsumer` ultimately drive (
  the consumer calls `EventIngestionService` directly, not this HTTP endpoint).
- `GET/POST /api/webhook-endpoints`, `GET/PUT/DELETE /api/webhook-endpoints/{id}`,
  `POST /api/webhook-endpoints/{id}/enable`, `POST /api/webhook-endpoints/{id}/disable` — merchant webhook subscription
  management (`WebhookEndpointController`).
- `GET /api/webhook-deliveries?endpointId=`, `GET /api/webhook-deliveries/{id}`,
  `POST /api/webhook-deliveries/{id}/retry` — delivery history and an **operator-triggered manual retry** of a
  failed/dead delivery (`WebhookDeliveryController`).
- `GET/POST /api/suppressions`, `GET /api/suppressions/check?channel=&recipient=` — suppression list management (
  `SuppressionController`).
    - `POST /api/suppressions/webhooks/sendgrid` — SendGrid Event Webhook receiver; auto-suppresses on `bounce`/
      `dropped`/`spamreport`/`unsubscribe`. Signature verification is noted as a TODO in the code comment, not yet
      implemented.
    - `POST /api/suppressions/webhooks/twilio` — Twilio status-callback receiver (form-encoded); auto-suppresses on
      `failed`/`undelivered`. Same signature-verification caveat.
- `POST /api/messages`, `GET /api/messages?channel=&recipient=&limit=`, `GET /api/messages/{id}` — send/query outbound
  email/SMS messages (`MessageController`).
- `GET/POST /api/templates`, `GET /api/templates/{key}/{channel}`, `GET /api/templates/{key}/{channel}/{locale}`,
  `GET /api/templates/{key}/{channel}/{locale}/{version}`, `POST /api/templates/preview` — append-only versioned
  templates; every `POST` creates a new version rather than mutating in place (`TemplateController`).

## Data model

- `Event` (`events`, partitioned by `created_at`) — a recorded domain event: id, merchantId, type,
  aggregateType/aggregateId, apiVersion, JSONB payload, per-merchant sequence number. Idempotency is enforced via a
  separate `event_idempotency` table (the events table can't carry a standalone unique constraint on `id` because it's
  partitioned).
- `WebhookEndpoint` (`webhook_endpoints`) — a merchant's registered webhook: URL, KMS-referenced signing secret (
  `secretRef`), subscribed event types (`text[]`), status (`active`/`disabled`/`auto_disabled`), consecutive-failure
  counter driving auto-disable.
- `WebhookDelivery` (`webhook_deliveries`, partitioned by `created_at`) — one row per (endpoint, event): attempt count,
  status (`pending`/`delivering`/`delivered`/`failed`/`dead`), next retry time, response code/latency/error.
- `Message` (`messages`) — an outbound email/SMS send: channel, template id, hashed (not raw) recipient, locale, status,
  provider, provider reference id, sent timestamp.
- `Suppression` (`suppressions`, composite key `channel` + hashed recipient) — a recipient blocked from further sends,
  with a reason (bounce/complaint/unsubscribe).
- `Template` (`templates`) — append-only versioned template rows keyed by (key, channel, locale, version), holding
  subject/body.

## Inter-service integration

**Inbound Kafka (new today):** `MerchantEventConsumer` (`src/main/java/.../event/MerchantEventConsumer.java`) subscribes
to the `merchant.events` topic (`events.kafka.merchant-events-topic`, defaults to `merchant.events`, overridable via
`MERCHANT_EVENTS_TOPIC`) using consumer group `spring.kafka.consumer.group-id` (defaults to `notification-service`,
overridable via `KAFKA_GROUP_ID`), with manual ack-mode (`spring.kafka.listener.ack-mode: MANUAL`,
`enable-auto-commit: false`). This topic is produced by merchant-service's transactional outbox (`OutboxWriter`/
`OutboxRelay`), envelope shape `{eventId, eventType, aggregateType, aggregateId, occurredAt, schemaVersion, data}`. The
consumer only accepts envelopes with `aggregateType == "merchant"`, maps them into an `EventIngestRequest`, and calls
`EventIngestionService.ingest(...)` directly — the same recording + webhook fan-out path used by the REST endpoint. The
offset is only acknowledged after `ingest()` returns successfully, and ingestion is idempotent on event id, so
at-least-once redelivery on a crash mid-processing does not produce duplicate webhook deliveries. Malformed/unparseable
payloads are logged and acked (skipped) rather than blocking the partition.

Per the class-level javadoc, this is intentionally the **only** inbound Kafka wiring today. Other candidate topics —
e.g. authentication-service's account-locked event — are identity-scoped rather than merchant-scoped and don't carry a
resolvable recipient address, so they can't be mapped onto `EventIngestionService`'s merchant-fan-out model without
fabricating data, and are deliberately left unwired.

**REST callers:** No confirmed live caller of notification-service's REST API was found in this codebase.

- `gateway-service/README.md` explicitly states there is **no gateway route** to notification-service today — it is not
  reachable through the edge.
- `dispute-service` has a `NotificationClient` interface with only a `LoggingNotificationClient` stub implementation (
  `dispute-service/.../integration/stub/LoggingNotificationClient.java`). Its own doc comment explains why:
  dispute-service only has a merchant id and free-text subject/body, not a resolved recipient address or a template key,
  so it can't call `POST /api/messages` without fabricating data — it logs instead of calling.
- The `ApiKeyAuthFilter` javadoc names payment-service, ledger-service, merchant-service, settlement-service, and
  dispute-service as the intended internal callers of `/api/**`, but no actual outbound HTTP client to
  notification-service was found in any of those services' source during this review — treat that list as documented
  intent, not verified traffic.

## Running locally

Key environment variables (all have local-friendly defaults in `application.yml`):

- `SERVER_PORT` (default `8094`)
- `DB_URL` (default `jdbc:postgresql://localhost:5432/notification_service`), `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE`
- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`), `KAFKA_GROUP_ID` (default `notification-service`)
- `EVENTS_KAFKA_ENABLED` (default `true`), `MERCHANT_EVENTS_TOPIC` (default `merchant.events`)
- `INTERNAL_API_KEY` — shared secret required on `/api/**` requests (leave blank to disable the check locally)
- `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`, `SENDGRID_FROM_NAME`
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`
- `SECRETS_PROVIDER` (default `env`) — resolves webhook signing `secret_ref` values via env vars locally (
  `EnvSecretResolver`); point at a real KMS in production.

Run (from repo root, Gradle module `notification-service`):

```
./gradlew :notification-service:bootRun
```

Requires a reachable Postgres (`notification_service` DB) and, for the Kafka consumer to actually receive traffic, a
reachable Kafka broker with merchant-service producing to `merchant.events`. Setting `EVENTS_KAFKA_ENABLED=false`
disables the listener (`autoStartup`) if Kafka isn't available locally.

## Design notes

- **Partitioned tables with a workaround for uniqueness:** `events` and `webhook_deliveries` are range-partitioned by
  `created_at` in Postgres, which forces the partition key into any unique constraint — so the JPA `@Id` alone (`id` /
  identity `id`) can't be backed by a DB-level unique constraint on the physical table. Event idempotency is instead
  claimed via a small non-partitioned `event_idempotency` table with an `INSERT ... ON CONFLICT DO NOTHING`, checked
  before the event row is written.
- **Recipient hashing, not raw storage:** `Message.recipientHash` and `SuppressionId`'s recipient field store hashed (
  via `RecipientHasher`), not raw, email/phone values, limiting PII exposure in the messages/suppressions tables while
  still supporting exact-match suppression checks.
- **Deliberately narrow Kafka surface:** rather than wiring every plausible event-producing topic, only
  `merchant.events` is consumed, because it's the only one whose envelope carries a merchant id that maps onto the
  existing webhook-fan-out model. This is a conscious scope decision recorded in `MerchantEventConsumer`'s javadoc, not
  an oversight — identity-scoped events (e.g. account-locked) are left for a future design that can resolve a recipient.
- **Manual-ack, ingest-then-acknowledge Kafka consumption:** offsets are only committed after
  `EventIngestionService.ingest()` returns, combined with idempotent ingestion, giving at-least-once delivery semantics
  without duplicate webhook fan-out on redelivery.
