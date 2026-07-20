# audit-service

Immutable, hash-chained, Merkle-sealed audit trail for the payment processor platform.

## Role in the platform

- Provides a single, append-only system of record for "who did what, to what, when" across the platform — accessed via
  `POST /api/v1/audit-records` and consumed asynchronously off Kafka.
- Cryptographically chains every record to its predecessor (`prevHash` → `hash`) so that any deletion, edit, or
  reordering of history is detectable by recomputation, not just trusted by access control.
- Periodically (daily, UTC) seals a day's records into a Merkle-rooted, signed "legal copy" batch written to S3 with
  Object Lock (COMPLIANCE mode), then anchors the root externally — turning the live Postgres chain into a durable,
  tamper-evident archive independent of the database itself.
- Screens every incoming record for raw PII via `PiiGuardService` before persistence; `before`/`after` diffs are
  expected to already be redacted, which is what lets these records be retained for years without GDPR erasure
  conflicts.
- Exposes read/search and chain-verification endpoints so operators, compliance jobs, and monitoring can query history
  and re-prove integrity on demand.
- Guards all endpoints with a simple internal API key (`X-Api-Key`, `ApiKeyAuthFilter`), matching the platform's
  internal-service-to-service trust model rather than end-user auth.

## Tech stack

- Java 21 (Gradle `sourceCompatibility`/`targetCompatibility` = `VERSION_17`, but the module targets the platform's
  Spring Boot 3.3.2 / Java 21 baseline like the other services), Spring Boot 3.3.2.
- **Default port: 8095** (`server.port: ${SERVER_PORT:8095}` in `application.yml`) — matches the platform port map.
- Datastore: PostgreSQL (`org.postgresql:postgresql`), accessed via Spring Data JPA/Hibernate.
  `spring.jpa.hibernate.ddl-auto: validate` — schema is owned entirely by Flyway, Hibernate never auto-DDLs.
- Flyway: enabled, `baseline-on-migrate: true`; single migration `src/main/resources/db/migration/V1__init_schema.sql`.
- Kafka (`spring-kafka`) for asynchronous ingestion.
- AWS SDK v2 `software.amazon.awssdk:s3` for the Object-Locked legal-copy store (`S3ObjectLockStore`).
- `springdoc-openapi-starter-webmvc-ui` (Swagger UI at `/swagger-ui.html`).
- Micrometer + `micrometer-registry-prometheus`, Spring Boot Actuator (`/actuator/health`, `/info`, `/prometheus`,
  `/metrics`).

## API surface

All endpoints are internal (`X-Api-Key` header required unless `audit.security.enabled=false`, as in the `local`
profile).

**`AuditRecordController` — `/api/v1/audit-records`** (append-only; intentionally no PUT/DELETE — records are immutable)

- `POST /api/v1/audit-records` — append a record. Idempotent on `eventId`: replaying the same `eventId` returns the
  existing record with `200`; a genuinely new event returns `201` with a `Location` header.
- `GET /api/v1/audit-records/{id}` — fetch by record id (`aud_<ULID>`).
- `GET /api/v1/audit-records` — search by exactly one of `merchantId`, `(resourceType + resourceId)`, or `action`, with
  `limit` (default 50).

**`BatchController` — `/api/v1/audit/batches`** (daily legal-copy batch admin)

- `GET /api/v1/audit/batches/{date}` — manifest for a UTC day (`yyyy-MM-dd`).
- `POST /api/v1/audit/batches/{date}/seal` — manually seal a day's batch (idempotent; for backfills/recovery — sealing
  normally runs on `audit.batch.cron`, default `0 30 0 * * *` UTC, via `BatchScheduler`).

**`VerificationController` — `/api/v1/audit`** (chain integrity)

- `GET /api/v1/audit/verify?fromSeq=&toSeq=` — recomputes hashes over `[fromSeq, toSeq]` (defaults: `1` to the current
  head) and reports whether the chain is intact.
- `GET /api/v1/audit/head` — current head `seq` (`0` if the chain is empty).

## Data model

### Client-assigned IDs and `Persistable<String>`

`AuditRecord`, `AuditBatch`, and `ChainState` all implement Spring Data's `Persistable<String>`. All three have *
*client-assigned, not database-generated, primary keys**:

- `AuditRecord.id` — `aud_<ULID>` (lexicographically sortable), generated in `AuditIngestionService.buildRecord`.
- `AuditBatch.id` — `batch_<yyyy-MM-dd>`, generated in `DailyBatchService.startManifest`.
- `ChainState.id` — the literal singleton value `"GLOBAL"` (`ChainState.GLOBAL_ID`).

Without `Persistable`, Spring Data JPA infers "new vs. existing" from whether `@Id` is null, which it never is here — so
`save()` would silently become a `SELECT`-then-`UPDATE`/`INSERT` **merge**, masking the very race conditions the service
depends on being real database `INSERT` conflicts. Each entity carries a `@Transient transient boolean isNew = true`
field, defaulting new instances to "new," and flips to `false` via a `@PostPersist`/`@PostLoad` callback (
`markNotNew()`). `isNew()` simply returns that flag. The effect: every `save()` on a genuinely new instance issues a
plain `INSERT`, so a duplicate `seq` or `event_id` surfaces immediately as a `DuplicateKeyException` from the unique
constraint rather than being swallowed by an upsert. No `@Version` field is used on any of the three entities —
concurrency control is done manually (see below), not via JPA optimistic locking.

### `AuditRecord` (table `audit_records`)

The queryable copy of the append-only hash chain. Key fields: `id`, `seq` (`UNIQUE`), `ts` (business event time),
`recordedAt` (server receive time), embedded `actor`/`resource`, `merchantId`, redacted `before`/`after` (`JSONB`),
`requestId`/`traceId`, `eventId` (`UNIQUE`, idempotency key), `prevHash`, `hash`, and `batchId` (null until sealed into
a batch).

### `AuditBatch` (table `audit_batches`)

Manifest for a sealed daily batch; the actual legal-copy content lives in S3, this row is metadata: `id` (
`batch_<date>`), `batchDate` (`UNIQUE`), `fromSeq`/`toSeq`/`recordCount`, `rootHash` (Merkle root over that day's record
hashes), `signature`/`signingKeyId` (EC P-256 signature over the signing payload), S3 location (`s3Bucket`/`s3Key`/
`s3VersionId`), `retainUntil` (Object Lock retention), `anchorRef` (external anchor reference), and `status` (
`SEALING → STORED → ANCHORED`, or `FAILED`).

### `ChainState` (table `audit_chain_state`)

A singleton row (`id = "GLOBAL"`) tracking the chain head: `seq` and `headHash`. Advanced with an explicit
compare-and-set (`ChainStateStore.compareAndAdvance`), not JPA optimistic locking.

### Hash-chain and Merkle-seal design

This is the core integrity mechanism, implemented in `crypto/` (`Sha256`, `CanonicalJson`, `MerkleTree`) and
`service/` (`HashChainService`, `ChainVerificationService`) — untouched by the Mongo→Postgres migration, since the
algorithm is storage-agnostic.

1. **Canonical serialization** (`CanonicalJson`) — a deterministic JSON form independent of any library's defaults:
   object keys sorted lexicographically (via `TreeMap`), arrays keep insertion order, only `Map`/`Collection`/`String`/
   `Number`/`Boolean`/`Instant`/`null` are supported (anything else fails fast). This guarantees the same logical record
   always serializes to the same bytes — a prerequisite for hashes to be reproducible on re-verification. These rules
   are explicitly documented as frozen: changing them would invalidate every previously computed hash.

2. **Per-record hash** (`HashChainService.computeHash`) — `hash = sha256(canonical(content))`, where `content` is an
   explicit field set (`id`, `seq`, `ts`, `recordedAt`, `event_id`, `actor`, `action`, `resource`, `merchant_id`,
   `before`, `after`, `request_id`, `trace_id`, `prev_hash`) — everything immutable about the record, **including its
   own `prevHash`**, but excluding the `hash` field itself and the post-hoc `batchId`. Hashes are stored as
   `sha256:<hex>` (`Sha256.hashPrefixed`).

3. **Chaining** — each new record's `prevHash` is set to the current chain head's `headHash` (from `ChainState`)
   *before* its own `hash` is computed. Because `prevHash` is baked into the hash input, altering, deleting, or
   reordering any record changes that record's hash, which no longer matches the `prevHash` the *next* record committed
   to — breaking the chain from that point forward. The very first record chains to a configured genesis hash (
   `audit.chain.genesis-hash`, currently an all-zero sentinel).

4. **Append serialization** (`AuditIngestionService.append`) — concurrent writers all attempt to claim `headSeq + 1`.
   The `UNIQUE` constraint on `audit_records.seq` (added in `V1__init_schema.sql`) is the actual mutual-exclusion
   primitive: exactly one concurrent `INSERT` for a given `seq` can succeed, the rest fail with `DuplicateKeyException`
   and retry (up to `audit.ingestion.max-retries-on-contention`, default 8) against the newly observed head. The record
   row is inserted *before* `ChainState` is advanced, and the head advance itself is a compare-and-set (
   `ChainStateStore.compareAndAdvance`) guarded by the previously observed `(seq, headHash)`, so the head can never be
   advanced to point at a record that was not durably written. `event_id` (also `UNIQUE`) gives idempotent replay:
   append is a no-op (200, existing record returned) if the same `eventId` is submitted twice, at either the REST or
   Kafka ingestion path.

5. **Verification** (`ChainVerificationService.verifyRange`) — recomputes the chain over a `[fromSeq, toSeq]` window (
   paged 1000 rows at a time): for every record it checks (a) `seq` is contiguous (no gaps — a deleted row would show up
   here), (b) the record's `prevHash` equals the previous record's stored `hash` (a broken link — tampering with an
   earlier record without recomputing everything after it), and (c) recomputing `HashChainService.computeHash` on the
   stored fields reproduces the stored `hash` exactly (content tampering). The first failure of any kind reports the
   specific `seq` and reason via `GET /api/v1/audit/verify`.

6. **Daily Merkle seal** (`DailyBatchService.sealDay`, driven by `BatchScheduler` on `audit.batch.cron`) — pulls all
   records `recordedAt` within one UTC day, computes `MerkleTree.computeRoot` over their `hash` values (SHA-256 pairwise
   tree, promoting an odd node up unchanged per level, `EMPTY_ROOT` sentinel for a record-less day), signs a canonical
   payload of `(batchDate, fromSeq, toSeq, recordCount, root)` with an EC P-256 key (`BatchSigner`), writes the full
   record set plus manifest as one immutable object to S3 under Object Lock in `COMPLIANCE` mode (`S3ObjectLockStore`,
   retention = `audit.batch.retention-years`, default 10), and finally anchors the root externally (
   `ExternalAnchorService` — `log` mode just logs it, `http` mode POSTs it to a configured endpoint, e.g. a
   notary/timestamping service). A single Merkle root therefore attests to every record in that day in one anchored
   value, and the design leaves room for compact per-record inclusion proofs later even though none are exposed today.
   Sealing is idempotent per day (`batch_<date>` id, `UNIQUE(batch_date)`) and resumable: it re-reads any `SEALING`/
   `STORED` manifest and continues rather than restarting.

## Inter-service integration

- **Kafka consumer** (`AuditEventConsumer`) — listens on topic `${audit.kafka.topic}`, which defaults to *
  *`audit.events`** (`AuditProperties`/`application.yml`: `audit.kafka.topic: ${AUDIT_KAFKA_TOPIC:audit.events}`).
  Consumer group `${spring.kafka.consumer.group-id}` (default `audit-service`), `spring.kafka.listener.ack-mode: manual`
  with concurrency 3. The listener parses the payload into the same `CreateAuditRecordRequest` DTO the REST API uses and
  calls `AuditIngestionService.append`, acknowledging only after the record is durably appended — a crash mid-processing
  replays the message rather than losing it, and replay is safe because ingestion is idempotent on `eventId`.
  Non-retryable failures (malformed JSON, validation errors, detected PII) throw and are expected to be routed by the
  Kafka error handler to the dead-letter topic `${audit.kafka.dlt-topic}` (default `audit.events.DLT`).
- No other service in this repository was found publishing to `audit.events` or any similarly named topic — a repo-wide
  grep for `audit.events`/`audit-service` found only `audit-service` itself and `gateway-service`.
- **Direct REST caller: `gateway-service`.** `gateway-service`'s `AuditPublisher` (
  `gateway-service/src/main/java/.../audit/AuditPublisher.java`) makes a fire-and-forget, best-effort `POST` to
  audit-service (`GatewayProperties.Audit`: `uri: ${AUDIT_SERVICE_URI:http://audit-service:8080}`,
  `path: ${AUDIT_SERVICE_PATH:/api/v1/audit/events}`) with a 2s timeout and small backoff retry; failures are logged and
  swallowed so gateway request handling is never blocked by audit availability. Note the gateway's configured default
  path (`/api/v1/audit/events`) does not match audit-service's actual controller path (`/api/v1/audit-records`) — this
  only matters if `AUDIT_SERVICE_PATH` isn't overridden to the real path in deployment, and is worth reconciling but is
  outside this README's remit (no source changes were made here to fix it).
- The gateway's Spring Cloud Gateway route table also proxies `audit-service` at
  `${AUDIT_SERVICE_URI_ROUTE:http://audit-service:8080}` for any externally-exposed admin/query access to its REST API.

## Running locally

Environment variables (all have working defaults for local dev, shown in `application.yml`):

| Variable                                                                    | Default                                                                     | Purpose                 |
|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------|-------------------------|
| `SERVER_PORT`                                                               | `8095`                                                                      | HTTP port               |
| `DB_HOST` / `DB_PORT` / `DB_NAME`                                           | `localhost` / `5432` / `audit_db`                                           | PostgreSQL connection   |
| `DB_USERNAME` / `DB_PASSWORD`                                               | `audit` / `audit`                                                           | PostgreSQL credentials  |
| `DB_POOL_SIZE`                                                              | `20`                                                                        | HikariCP max pool size  |
| `KAFKA_BOOTSTRAP_SERVERS`                                                   | `localhost:9092`                                                            | Kafka connection        |
| `KAFKA_GROUP_ID`                                                            | `audit-service`                                                             | Consumer group id       |
| `KAFKA_CONCURRENCY`                                                         | `3`                                                                         | Listener concurrency    |
| `AUDIT_API_KEYS` / `AUDIT_API_KEY_ENABLED`                                  | `local-dev-key` / `true`                                                    | Internal API key auth   |
| `AUDIT_KAFKA_ENABLED` / `AUDIT_KAFKA_TOPIC` / `AUDIT_KAFKA_DLT_TOPIC`       | `true` / `audit.events` / `audit.events.DLT`                                | Kafka ingestion         |
| `AUDIT_BATCH_ENABLED` / `AUDIT_BATCH_CRON` / `AUDIT_RETENTION_YEARS`        | `true` / `0 30 0 * * *` / `10`                                              | Daily sealing job       |
| `AUDIT_S3_ENABLED` / `AUDIT_S3_BUCKET` / `AWS_REGION` / `AUDIT_S3_ENDPOINT` | `true` / `payment-processor-audit-legal` / `us-east-1` / (empty = real AWS) | Legal-copy S3 store     |
| `AUDIT_SIGNING_KEY` / `AUDIT_SIGNING_KEY_ID`                                | (empty = signing disabled) / `audit-batch-signer-v1`                        | Batch signing key       |
| `AUDIT_ANCHOR_MODE` / `AUDIT_ANCHOR_ENDPOINT`                               | `log` / (empty)                                                             | External root anchoring |

A `local` Spring profile disables API-key auth, S3, batching, and Kafka, so a bare Postgres instance is enough to
exercise the REST create/query/verify endpoints.

Postgres must exist and be migratable by Flyway (`audit_db` database, `audit`/`audit` credentials by default); the
schema itself comes entirely from `V1__init_schema.sql`, no manual DDL needed.

Run from the repo root:

```bash
./gradlew :audit-service:bootRun
# or, with the local profile (no Kafka/S3/API-key required):
./gradlew :audit-service:bootRun --args='--spring.profiles.active=local'
```

Swagger UI: `http://localhost:8095/swagger-ui.html`. Health: `http://localhost:8095/actuator/health`.

## Design notes

- **Hash chain + Merkle seal as two integrity layers, not one.** The per-record `prevHash`/`hash` chain gives cheap,
  fine-grained tamper detection at query time (`GET /api/v1/audit/verify`) directly against the live Postgres table. The
  daily Merkle root + signature + Object Lock + external anchor gives a second, independent, storage-agnostic
  attestation: even if the Postgres database itself were compromised or rolled back, the signed, anchored S3 legal
  copies from prior days remain an external check on history — the two layers protect against different threat models (
  live tampering vs. wholesale database compromise).
- **`Persistable<String>` was the deliberate fix, not an accident, for client-assigned IDs surviving the Postgres
  migration.** All three entities generate their own primary key before `save()` is ever called (`aud_<ULID>`,
  `batch_<date>`, `"GLOBAL"`). Spring Data JPA's default new-vs-existing heuristic (`@Id == null`) always evaluates to "
  existing" for these entities, which would make Hibernate emit a `SELECT` to check existence and then either `UPDATE`
  or `INSERT` — silently turning appends into merges and hiding unique-constraint races behind an extra round trip. The
  `@Transient isNew` flag plus `@PostPersist`/`@PostLoad` callback pattern used here forces a plain `INSERT` for
  genuinely new rows every time.
- **The `UNIQUE` constraint on `audit_records.seq` is the actual concurrency primitive**, not an incidental integrity
  check. `AuditIngestionService` does no row locking or `SELECT ... FOR UPDATE`; it lets every concurrent writer race to
  `INSERT` the same candidate `seq`, and trusts Postgres to let exactly one succeed. Losers see `DuplicateKeyException`
  and retry against the (now-advanced) head. This turns "serialize concurrent appends" into a property the database
  already guarantees, rather than application-level locking that would have to be gotten right independently.
- **`event_id` uniqueness gives idempotency for free across two ingestion paths.** Because both the synchronous REST
  `POST /api/v1/audit-records` and the asynchronous Kafka consumer funnel through the same
  `AuditIngestionService.append`, at-least-once Kafka redelivery and client-side HTTP retries are both absorbed by the
  same `findByEventId` fast-path / unique-constraint fallback, without either caller needing its own dedup logic.
