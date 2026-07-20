# Tokenization Service

Stores payment instruments (cards, bank accounts, network tokens) as PCI-scoped, DB-persisted entities so raw card data
lives in one isolated service instead of spreading across the platform.

> This document describes what the current implementation actually does. The aspirational full functional
> specification (network-token orchestration, HSM integration, token lifecycle state machine, etc.) lives in [
`TokenReadme.md`](./TokenReadme.md) — most of it is **not yet implemented**; see "Implementation status" below.

---

## 1. Role in the platform

- Owns the `instruments` table (`Instrument` entity): a surrogate `token`, a `kind`, a `scopeMerchantId`, and a
  `fingerprint` — the record other services reference instead of the raw card/bank data.
- Owns PCI-scoped detail tables kept separate from the instrument record: `CardDetail` (`pan_ciphertext`, `nonce`,
  `aad`, `dek_id`, plus non-sensitive `last4`/`bin`/`brand`/`funding`/`expMonth`/`expYear`) and `BankDetail`.
- Owns `DekRegistry` — wrapped data-encryption-key records (`kek_id`, `wrapped_dek`), i.e. envelope-encryption metadata
  for the ciphertext columns above.
- Owns `NetworkToken` records (network-issued token ciphertext, network, TAR, status) and `BinRange` (card BIN
  metadata).
- Owns `InstrumentAccessLog` — an access-audit table (`actor`, `purpose`, `correlationId`) for who touched which
  instrument.
- Exposes plain REST CRUD over each of the above via one controller per entity; there is currently no dedicated
  `/tokenize` or `/detokenize` endpoint or business-logic service layer — controllers delegate straight to a thin
  service that wraps the Spring Data repository (`findAll`/`findById`/`save`/`deleteById`).

---

## 2. Tech stack

| Concern            | Choice                                                                                                                                                                                                                                                                                                                                                        |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Language / runtime | Java 21 (platform-wide), Spring Boot 3.3.2                                                                                                                                                                                                                                                                                                                    |
| Persistence        | PostgreSQL + Spring Data JPA (Hibernate), `ddl-auto: validate`                                                                                                                                                                                                                                                                                                |
| Schema management  | **No Flyway** — no `db/migration` directory in this module; schema is expected to pre-exist (validated, not migrated)                                                                                                                                                                                                                                         |
| Security           | `spring-boot-starter-security`, `io.jsonwebtoken` (JJWT) on the classpath                                                                                                                                                                                                                                                                                     |
| Service discovery  | Netflix Eureka client                                                                                                                                                                                                                                                                                                                                         |
| Encryption         | No encryption library/HSM client on the classpath — `application.yml` declares `tokenization.encryption.algorithm: AES/GCM/NoPadding` and `key-size: 256`, but no code in the service currently reads those properties or performs encryption; the `*_ciphertext`/`nonce`/`aad` columns exist as byte-array fields with no encrypt/decrypt logic wired up yet |

---

## 3. API surface

Default port **8084**. All controllers follow the same plain CRUD shape (`GET` list, `POST` create, `GET /{id}`,
`PUT /{id}`, `DELETE /{id}`):

| Controller                      | Base path                     |
|---------------------------------|-------------------------------|
| `InstrumentController`          | `/api/instruments`            |
| `CardDetailController`          | `/api/card-details`           |
| `BankDetailController`          | `/api/bank-details`           |
| `NetworkTokenController`        | `/api/network-tokens`         |
| `BinRangeController`            | `/api/bin-ranges`             |
| `DekRegistryController`         | `/api/dek-registries`         |
| `InstrumentAccessLogController` | `/api/instrument-access-logs` |

There is no `/tokenize` (PAN-in, token-out) or `/detokenize` (token-in, PAN-out) endpoint yet — "tokenization" today
means creating an `Instrument` + `CardDetail`/`BankDetail` row via the generic CRUD endpoints and having callers use the
instrument `id` as the token. `GET /api/instruments/{id}` returns the `Instrument` record (`id`, `token`, `kind`,
`scopeMerchantId`) — non-sensitive metadata only, since `CardDetail`/`BankDetail` (which hold the ciphertext) are
separate entities not joined into that response.

---

## 4. Data model

PCI-scoping is present at the **data-model level**, even though the encryption logic behind it isn't wired up yet:

- `Instrument` is the non-sensitive, freely-referenceable record (id/token/kind/scope/fingerprint) that other services
  can hold onto.
- `CardDetail`/`BankDetail` hold the sensitive material (`pan_ciphertext`, `cardholder_name_ciphertext`, `nonce`, `aad`,
  `dek_id`) in a **separate table keyed by `instrument_id`**, not embedded in `Instrument` — so a query or join against
  `Instrument` alone never surfaces ciphertext columns.
- `DekRegistry` implements envelope encryption's key hierarchy (a KEK id + wrapped DEK per registry row), matching the
  `dek_id` foreign key on `CardDetail`, even though no code currently generates/wraps/unwraps a DEK.
- `InstrumentAccessLog` is a standalone audit table, append-style (`actor`, `purpose`, `correlationId`, `createdAt`),
  separate from the instrument tables themselves.

---

## 5. Inter-service integration

**payment-service now really calls this service**, but the integration is narrower than the entity model suggests.
`payment-service`'s `VaultClient` (connector package) is a real WebClient wired against `localhost:8084` — as of today
it replaced a placeholder port — but it calls exactly one endpoint:

- `GET /api/instruments/{id}` — to fetch masked, non-sensitive instrument metadata (`token`, `kind`, `scopeMerchantId`)
  for receipts. Per the `VaultClient` Javadoc, payment-service "never handles raw PAN/CVV," and since
  `InstrumentController` only supports lookup by its own `id` primary key (no lookup-by-token endpoint),
  `PaymentIntent.instrumentToken` is expected to already hold that instrument id. This call is best-effort: on
  timeout/error it logs and returns `null` rather than failing the payment.

Routed through gateway-service at `TOKENIZATION_SERVICE_URI` (default `http://tokenization-service:8080`), with a
`tokenizationCircuitBreaker` fallback configured (`gateway-service/src/main/resources/application.yml`).

No other inbound callers or outbound calls (Kafka producers/consumers, other WebClients) were found in this service's
source — `TokenReadme.md`'s "Integration Notes" section (
merchant/user/fraud/dispute/refund/settlement/audit/card-network integrations) is aspirational and not reflected in the
current code.

---

## 6. Running locally

Prerequisites: JDK 21, PostgreSQL (schema must already exist — no Flyway here), and (optionally) Eureka.

```bash
gradle clean build     # or ./gradlew clean build if a wrapper is present
SPRING_PROFILES_ACTIVE=local gradle bootRun
```

Default datasource: `jdbc:postgresql://localhost:5432/tokenization_db` (`postgres`/`postgres` in `application.yml`;
overridable via profile). Default port: **8084**.

Notable config keys (`application.yml`):

```yaml
server:
  port: 8084
tokenization:
  encryption:
    algorithm: AES/GCM/NoPadding
    key-size: 256
  vault:
    retention-days: 2555
  audit:
    enabled: true
```

---

## 7. Design notes

- **Ciphertext lives outside the instrument row, not inside it.** Splitting `CardDetail`/`BankDetail` from `Instrument`
  means the frequently-queried, freely-shared record can never accidentally leak ciphertext columns through a naive
  `SELECT *` or serialization — a real PCI-scope-isolation pattern, even ahead of the encryption code that will populate
  those columns.
- **Envelope-encryption schema (`DekRegistry`) precedes the encryption implementation.** The DEK/KEK wrapping model is
  already normalized correctly (rotatable DEK per `dek_id`, `rotated_at` tracked), which is the harder design decision
  to get right; the AES-GCM call sites that would populate `panCiphertext`/`nonce`/`aad` are the comparatively
  mechanical part still to be written.
- **payment-service is deliberately restricted to the least-sensitive endpoint.** `VaultClient` only calls
  `GET /api/instruments/{id}`, never anything that could return `CardDetail`; that keeps payment-service structurally
  unable to receive a PAN even after it's implemented here, without relying on payment-service's own discipline.
- **No Flyway in this module** is a real gap, not a design choice worth defending: `ddl-auto: validate` means the schema
  must be created out-of-band, so there's currently no reproducible way to stand up `tokenization_db` from scratch.

---

## 8. Implementation status vs. `TokenReadme.md`

`TokenReadme.md` describes a full token lifecycle (state machine, rotation, expiration, single-use/merchant/network
tokens, HSM-backed encryption, card-network integrations, rate limiting/anomaly detection, PCI compliance artifacts).
The current codebase implements only:

- The data model shell for instruments, card/bank details, network tokens, BIN ranges, DEK registry, and access log.
- Generic CRUD REST endpoints for each entity.

Not yet implemented: dedicated tokenize/detokenize endpoints, PAN validation (Luhn/expiry/CVV), actual
encryption/decryption, HSM integration, token state machine, rotation/expiration jobs, network-token provisioning, rate
limiting, anomaly detection, and Kafka domain events (`TokenCreated`/`TokenExpired`/`TokenDeleted`). Treat
`TokenReadme.md` as the target design, and this file as the current state.
