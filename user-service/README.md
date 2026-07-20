# user-service

Owns platform users and merchant-scoped customer records: profiles, addresses, consent, and
GDPR-compliant erasure via crypto-shredding. Authentication itself (credentials, JWTs, MFA)
is a separate concern owned by `authentication-service` — this service is about *who someone
is*, not *how they log in*.

> Note: a pre-existing design doc, `UserServiceReadMe.md`, describes an aspirational MongoDB
> + Redis design. That doc predates the implementation and is inaccurate on both points — the
    > real service runs on PostgreSQL with no Redis dependency at all. This README describes the
    > actual, verified-from-source implementation.

## Role in the platform

- Manages platform `User` accounts and merchant-scoped `Customer` records as distinct
  entities, not one aggregate — a platform user and a merchant's customer are different
  things with different lifecycles.
- Owns polymorphic `Address` and `Consent` records that can attach to either a user or a
  customer (`ownerType`/`subjectType` discriminator, not a foreign key to a single table).
- Encrypts PII at the field level (name, DOB, email, phone, address) rather than relying on
  disk/column-level encryption, with a **blind index** for equality lookups on encrypted
  fields (HMAC-SHA256 deterministic index, so you can query "find user by email" without
  decrypting every row).
- Implements GDPR "crypto-shredding": erasure doesn't delete the row, it destroys the
  encryption key for that subject, which makes the ciphertext permanently unrecoverable —
  much cheaper and more auditable than a hard delete across every table that references PII.
- Publishes every state change to Kafka via a transactional outbox; exposes one
  internal-only endpoint for service-to-service customer lookups.

## Tech stack

- Spring Boot 3.5.3, Java 21.
- Port: `8082` (`SERVER_PORT` env var).
- Datastore: PostgreSQL (`user_service` DB), Flyway-managed (`classpath:db/migration`).
- **No Redis** — despite the platform-level `ARCHITECTURE.md` claiming Redis caching for this
  service, `CacheConfig`/`CustomerCacheService` in the code are empty, unused stubs.
- Security: Spring Security + OAuth2 resource server (validates tokens issued by
  `authentication-service`).
- Notable libraries: MapStruct (DTO mapping), Lombok, ULID (entity IDs), springdoc-openapi.
- Architecture: genuine hexagonal/DDD layering —
  `domain/` (framework-free core: entities, value objects, repository interfaces, domain
  events) → `application/` (commands, queries, services, outbound port interfaces) →
  `infrastructure/` (JPA adapters, Kafka outbox, encryption/KMS, security) → `api/`
  (REST controllers, request/response DTOs). This is the most rigorously layered service in
  the platform.

## API surface

All under `/api/v1`:

| Controller                   | Base path                  | Notable endpoints                                                                                                             |
|------------------------------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `UserController`             | `/users`                   | `POST` create, `GET /{id}`, `PATCH /{id}/status`, `DELETE /{id}` (GDPR erasure)                                               |
| `UserProfileController`      | `/users/{id}/profile`      | `GET`/`PUT`                                                                                                                   |
| `CustomerController`         | `/customers`               | full CRUD + `POST /{id}/erasure` (GDPR erasure, separate from delete)                                                         |
| `AddressController`          | `/addresses`               | full CRUD, polymorphic owner (`USER`/`CUSTOMER`)                                                                              |
| `ConsentController`          | `/consents`                | grant/revoke/get/list, polymorphic subject                                                                                    |
| `InternalCustomerController` | `/internal/customers/{id}` | service-to-service lookup, gated by `@PreAuthorize("hasAuthority('SCOPE_internal.customers.read')")` — not for client traffic |

`HealthController` is an intentionally empty stub; real health comes from Spring Boot
Actuator.

## Data model

`UserEntity` (`users`), `UserProfileEntity` (`user_profiles`, encrypted PII columns),
`CustomerEntity` (`customers`), `AddressEntity` (`addresses`), `ConsentEntity` (`consents`),
`CryptoKeyEntity` (`crypto_keys` — the DEKs crypto-shredding destroys), `OutboxEventEntity`
(`outbox_events`).

No JPA `@OneToMany`/`@ManyToOne` relationships anywhere — cross-entity references are plain
string-typed FK-style columns, consistent with the platform-wide rule of no cross-service
foreign keys and, within this service, a deliberate choice to keep aggregates independently
loadable.

**Encryption design** (`infrastructure/encryption/`): `AesGcmEncryptionService` performs
AES-GCM encryption with a `[nonce][ciphertext][tag]` layout; `DataKeyService` implements
envelope encryption (per-subject DEKs wrapped by a KMS-managed key); `BlindIndexService`
computes an HMAC-SHA256 index over the plaintext so encrypted fields remain searchable by
exact match; `CryptoShreddingService` implements `CryptoShredderPort` — erasure is exactly
`dataKeyService.destroyFor(subjectType, subjectId)`, nothing more, which is the entire point:
once the key is gone, the ciphertext is unrecoverable everywhere it's stored, including in
Kafka event history and backups.

## Inter-service integration

- **Outbound**: none — no WebClient, no synchronous calls to any other service. All outward
  integration is event-driven.
- **Inbound**: `authentication-service` issues the JWTs this service validates. Other
  services can call `InternalCustomerController` directly for customer lookups (a
  service-to-service, non-gateway path).
- **Gateway**: `gateway-service` routes `/api/v1/users/**`, `/api/v1/customers/**`,
  `/api/v1/addresses/**`, and `/api/v1/consents/**` to this service (the customer/address/
  consent routes were added alongside this README so the gateway actually exposes the whole
  API surface, not just `/users`).
- **Kafka (producer only)**: transactional outbox (`infrastructure/outbox/OutboxRelay.java`,
  `KafkaTemplate<String, byte[]>`, scheduled drain) publishes to `user-service.users.v1`,
  `user-service.customers.v1`, `user-service.addresses.v1`, `user-service.consents.v1`. No
  `@KafkaListener` — this service does not consume events from anywhere.

## Running locally

```bash
./gradlew.bat :user-service:bootRun
```

Key env vars: `SERVER_PORT` (default `8082`), Postgres connection (`DB_HOST`/`DB_PORT`/
`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD`, default DB name `user_service`), `KAFKA_BOOTSTRAP_SERVERS`
(default `localhost:9092`). Profiles available: `dev`, `local`, `prod` (no `docker` profile).

## Design notes

- The hexagonal layering isn't cosmetic — `domain/` genuinely has zero framework imports
  (no `@Entity`, no Spring annotations), so the core business rules (crypto-shredding
  semantics, consent lifecycle) are testable without spinning up JPA or Spring context. This
  is the strongest architecture-quality talking point in the whole platform.
- Crypto-shredding is a cheap, auditable answer to "how do you actually delete PII across a
  system with Kafka event history" — you can't un-publish a Kafka message, but you can make
  its encrypted payload permanently unreadable by destroying the key.
- The blind-index pattern (deterministic HMAC for encrypted-field lookup) is the standard
  answer to "how do you query encrypted data without decrypting every row," and is worth
  being able to explain: it trades a small amount of queryability leakage (exact-match only,
  no range queries) for real lookup performance.
- Known gap, worth naming honestly if asked: `CacheConfig`/`CustomerCacheService` are unused
  stubs and there's no Redis dependency, despite the platform architecture doc describing
  Redis caching for this service — a good example of documentation drifting from
  implementation, the same pattern this whole documentation pass was written to catch.
