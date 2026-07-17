# Tokenization Service

## Overview

The Tokenization Service is the platform's secure vault for sensitive payment data. It replaces Primary Account Numbers (PANs) and other cardholder data with non-sensitive surrogate tokens, ensuring that raw payment credentials never traverse internal systems or persist in merchant databases. By centralizing the storage and management of payment instruments, the service enables PCI DSS scope reduction, protects against data breaches, and supports advanced token types such as network tokens for improved authorization rates and lifecycle management.

Every token is cryptographically generated, uniquely mapped to the original payment instrument, and governed by a strict lifecycle that includes rotation, expiration, and secure deletion. The service is designed to be the sole custodian of sensitive payment data within the platform — no other service stores, logs, or transmits raw PANs.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Token Types](#token-types)
- [Core Functionalities](#core-functionalities)
  - [PAN Tokenization](#pan-tokenization)
  - [Token Detokenization](#token-detokenization)
  - [Network Tokens](#network-tokens)
  - [Token Lifecycle](#token-lifecycle)
  - [Token Rotation](#token-rotation)
  - [Token Expiration](#token-expiration)
  - [Token Lookup](#token-lookup)
  - [Merchant Token Mapping](#merchant-token-mapping)
  - [Vault Integration](#vault-integration)
  - [PCI Compliance](#pci-compliance)
- [Security Architecture](#security-architecture)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern | Description |
|---------|-------------|
| **Data Protection** | Ensure raw PANs and sensitive cardholder data are never exposed outside the secured vault boundary. |
| **PCI DSS Scope Reduction** | Centralize cardholder data environment (CDE) to a single service, reducing compliance burden for all other platform services and merchants. |
| **Token Lifecycle Governance** | Manage the complete lifecycle of every token from creation through rotation, expiration, and secure deletion. |
| **Network Token Orchestration** | Integrate with card networks (Visa VTS, Mastercard MDES, Amex Express) to provision and manage network-level tokens for improved auth rates and lifecycle events. |
| **Access Control** | Enforce strict authentication and authorization for every tokenization and detokenization request. |
| **Audit & Monitoring** | Log every vault access, detect anomalous detokenization patterns, and provide forensic evidence for security investigations. |

---

## Token Types

The service supports multiple token types optimized for different use cases and security requirements.

| Token Type | Description | Use Case |
|------------|-------------|----------|
| **Platform Token** | Service-generated surrogate token replacing the raw PAN within the platform ecosystem. | Internal storage, transaction referencing, merchant API responses. |
| **Network Token** | Card network-issued token (Visa VTS, Mastercard MDES, Amex Express) bound to a specific device or merchant. | Improved authorization rates, automatic card updates, reduced fraud. |
| **Merchant Token** | Scoped token unique to a specific merchant; the same PAN yields different tokens for different merchants. | Merchant-specific storage without revealing cross-merchant relationships. |
| **Single-Use Token** | One-time token valid for a single transaction or short time window. | Checkout flows, guest checkouts, high-risk transactions. |
| **Reversible Token** | Token that can be detokenized back to the original PAN when authorized. | Recurring billing, refunds, chargeback handling. |
| **Non-Reversible Token** | Token that cannot be detokenized; used for analytics and reporting only. | Data analytics, trend analysis, reporting dashboards. |

---

## Core Functionalities

### PAN Tokenization

Converts a raw Primary Account Number (PAN) and associated cardholder data into a secure platform token.

**Tokenization Flow:**
1. **Receive Request** — Accept PAN and cardholder data over a secure, encrypted channel (TLS 1.3).
2. **Validate Input** — Verify PAN passes Luhn check, validate expiry date, verify CVV format (if provided).
3. **Deduplication Check** — Query vault for existing token for this PAN + merchant combination.
4. **Generate Token** — Create cryptographically random surrogate token (e.g., `tok_1Ab2Cd3Ef4Gh5Ij6Kl7Mn8Op`).
5. **Encrypt & Store** — Encrypt PAN using AES-256-GCM with hardware security module (HSM) protected keys; store in the vault.
6. **Map Relationships** — Store token-to-PAN mapping, token-to-merchant mapping, and metadata.
7. **Return Token** — Return only the surrogate token to the caller; raw PAN is never exposed.

**Input Fields:**

| Field | Required | Description |
|-------|----------|-------------|
| `pan` | Yes | Primary Account Number (16–19 digits) |
| `expiryMonth` | Yes | Card expiry month (MM) |
| `expiryYear` | Yes | Card expiry year (YYYY) |
| `cvv` | No | Card verification value (3–4 digits); never stored, used for validation only |
| `cardholderName` | No | Name on card |
| `merchantId` | Yes | Merchant for whom the token is being created |
| `customerId` | No | Platform customer reference for recurring use |
| `paymentMethodType` | Yes | `CREDIT_CARD`, `DEBIT_CARD`, `PREPAID_CARD` |
| `deviceFingerprint` | No | Device fingerprint for risk correlation |

**Output:**
```json
{
  "tokenId": "tok_1Ab2Cd3Ef4Gh5Ij6Kl7Mn8Op",
  "tokenType": "PLATFORM",
  "maskedPan": "411111******1111",
  "cardBrand": "VISA",
  "expiryMonth": "12",
  "expiryYear": "2028",
  "merchantId": "mer_123456789",
  "customerId": "cus_987654321",
  "createdAt": "2026-07-17T14:32:00Z",
  "expiresAt": "2028-12-31T23:59:59Z"
}
```

> **Security:** CVV is validated but never stored. Only the token, masked PAN, and expiry are returned.

---

### Token Detokenization

Retrieves the original PAN and cardholder data from a token for authorized use cases.

**Detokenization Flow:**
1. **Receive Request** — Accept token ID and requesting service identity.
2. **Authorization Check** — Verify the caller has permission to detokenize this token (role-based, merchant-scoped).
3. **Audit Log** — Record the detokenization request (who, what, when, why).
4. **Retrieve & Decrypt** — Fetch encrypted PAN from vault; decrypt using HSM-protected keys.
5. **Validate Token State** — Ensure token is active, not expired, and not revoked.
6. **Return Data** — Return PAN and cardholder data over secure channel; log access.

**Authorized Detokenization Scenarios:**

| Scenario | Authorized Service | Purpose |
|----------|-------------------|---------|
| **Transaction Authorization** | Payment Service | Send PAN to acquirer for authorization |
| **Refund Processing** | Refund Service | Retrieve PAN for refund to original card |
| **Chargeback Handling** | Dispute Service | Retrieve PAN for representment evidence |
| **Network Token Provisioning** | Tokenization Service | Exchange PAN for network token |
| **Compliance Request** | Audit Service | Retrieve PAN for regulatory investigation |

**Access Control Rules:**
- Merchants can only detokenize tokens they created.
- Platform services must authenticate via mTLS and present valid service identity.
- Detokenization for analytics or reporting is blocked; use non-reversible tokens instead.
- Rate limiting: max 100 detokenizations per minute per service.
- Anomaly detection: flag unusual detokenization patterns (e.g., bulk requests, off-hours access).

---

### Network Tokens

Integrates with card network tokenization services to provision and manage network-level tokens.

**Network Token Benefits:**

| Benefit | Description |
|---------|-------------|
| **Higher Auth Rates** | Network tokens are trusted by issuers, reducing false declines. |
| **Automatic Card Updates** | Networks push lifecycle events (expiry updates, lost/stolen replacements) automatically. |
| **Device Binding** | Tokens can be bound to specific devices, reducing fraud. |
| **Reduced PCI Scope** | Merchants store network tokens instead of PANs. |
| **Lifecycle Management** | Networks handle card reissues without merchant intervention. |

**Supported Networks:**

| Network | Service | Token Format |
|---------|---------|--------------|
| **Visa** | Visa Token Service (VTS) | 16-digit network token |
| **Mastercard** | Mastercard Digital Enablement Service (MDES) | 16-digit network token |
| **Amex** | American Express Express | 15-digit network token |
| **Discover** | Discover Digital Exchange (DDE) | 16-digit network token |

**Network Token Flow:**
1. Merchant or platform requests tokenization of a PAN.
2. Tokenization Service checks if network token is available and beneficial.
3. If yes, send PAN to network token service via secure API.
4. Network returns a network token, cryptogram, and expiry.
5. Tokenization Service stores the network token alongside the platform token.
6. For transactions, use the network token + cryptogram instead of the raw PAN.
7. Subscribe to network lifecycle events (expiry update, suspension, deletion).

**Lifecycle Events from Networks:**

| Event | Action |
|-------|--------|
| `TOKEN_UPDATED` | Expiry date or card art changed; update platform records |
| `TOKEN_SUSPENDED` | Card temporarily suspended; block transactions |
| `TOKEN_DELETED` | Card closed or replaced; mark token expired |
| `TOKEN_ACTIVATED` | New token provisioned; activate for transactions |

---

### Token Lifecycle

Governs the complete state machine of every token from creation to deletion.

**Token States:**

```
CREATED
   │
   ├──► ACTIVE ──► (normal usage)
   │       │
   │       ├──► SUSPENDED ──► ACTIVE (unsuspend)
   │       │       │
   │       │       └──► DELETED (permanent)
   │       │
   │       ├──► EXPIRED ──► DELETED (cleanup)
   │       │
   │       └──► DELETED (manual or scheduled)
   │
   └──► FAILED (creation error) ──► DELETED
```

| State | Description |
|-------|-------------|
| **CREATED** | Token generated, awaiting first use or activation. |
| **ACTIVE** | Token is valid and available for transactions and detokenization. |
| **SUSPENDED** | Token temporarily disabled due to risk, fraud, or customer request; can be reactivated. |
| **EXPIRED** | Token reached its natural expiry date (card expiry or configured token TTL). |
| **DELETED** | Token permanently removed; PAN may be retained in encrypted archive for regulatory period. |
| **FAILED** | Token creation failed (invalid PAN, network error); no sensitive data stored. |

**State Transitions:**
- `CREATED` → `ACTIVE` on first successful transaction or explicit activation
- `ACTIVE` → `SUSPENDED` on fraud alert, customer request, or risk trigger
- `SUSPENDED` → `ACTIVE` on fraud clearance or customer reactivation
- `ACTIVE` → `EXPIRED` on card expiry or token TTL expiration
- `EXPIRED` → `DELETED` after grace period (e.g., 30 days post-expiry)
- Any state → `DELETED` on customer data deletion request (GDPR) or merchant account closure

---

### Token Rotation

Replaces an existing token with a new one while preserving the underlying payment instrument relationship.

**Rotation Triggers:**

| Trigger | Description |
|---------|-------------|
| **Scheduled Rotation** | Periodic rotation (e.g., annually) as a security best practice |
| **Security Incident** | Suspected token compromise or breach notification |
| **Customer Request** | Customer reports lost/stolen card or requests new token |
| **Network Event** | Card network pushes card replacement or reissue |
| **Compliance Policy** | Mandatory rotation after a defined period per internal policy |

**Rotation Flow:**
1. Generate new surrogate token.
2. Update token-to-PAN mapping (same PAN, new token).
3. Mark old token as `SUSPENDED` or `EXPIRED`.
4. Update all merchant references to the new token.
5. Notify merchants of token change via webhook.
6. Publish `TokenCreated` event for the new token.

**Rotation Guarantees:**
- Old token remains valid for a grace period (e.g., 24 hours) to prevent transaction interruption.
- Recurring billing subscriptions automatically migrate to the new token.
- No duplicate charges during the transition window.

---

### Token Expiration

Manages the natural end-of-life of tokens based on card expiry or configured time-to-live (TTL).

**Expiration Policies:**

| Policy | Description |
|--------|-------------|
| **Card Expiry** | Token expires on the card's expiry date (month/year). |
| **Token TTL** | Token expires after a fixed duration (e.g., 2 years from creation). |
| **Inactivity TTL** | Token expires after a period of non-use (e.g., 12 months of no transactions). |
| **Merchant-Driven** | Merchant can set custom expiry for single-use or promotional tokens. |

**Expiration Workflow:**
1. **Pre-Expiry Notification** — Notify merchant and customer 30 days before expiry.
2. **Grace Period** — Allow transactions for 7 days post-expiry (configurable).
3. **Token Suspension** — Block new transactions after grace period.
4. **Cleanup** — After 30 days, mark token as `EXPIRED` and queue for deletion.
5. **Archive** — Retain encrypted PAN in cold storage for regulatory period (7 years) if required.
6. **Publish `TokenExpired` event.**

**Expiry Extension:**
- For network tokens, automatic expiry updates are pushed by card networks when cards are reissued.
- For platform tokens, merchants can request re-tokenization with updated card details.

---

### Token Lookup

Enables querying and retrieving token metadata without exposing sensitive cardholder data.

**Lookup Capabilities:**

| Query | Returns | Sensitive Data Exposed? |
|-------|---------|------------------------|
| `GET /tokens/{tokenId}` | Token metadata, masked PAN, expiry, status, merchant | No |
| `GET /tokens?merchantId={id}` | List of tokens for a merchant | No |
| `GET /tokens?customerId={id}` | List of tokens for a customer | No |
| `GET /tokens?cardBrand=VISA` | Filtered token list | No |
| `GET /tokens/{tokenId}/detokenize` | Full PAN and cardholder data | Yes (authorized only) |

**Search Filters:**
- `tokenId` — Exact match
- `merchantId` — All tokens for a merchant
- `customerId` — All tokens for a customer
- `cardBrand` — VISA, MASTERCARD, AMEX, DISCOVER, etc.
- `status` — ACTIVE, SUSPENDED, EXPIRED, DELETED
- `createdAt` — Date range
- `expiryDate` — Expiring before/after date
- `paymentMethodType` — CREDIT_CARD, DEBIT_CARD, PREPAID_CARD

**Pagination & Sorting:**
- Cursor-based pagination for large result sets
- Sort by `createdAt`, `expiryDate`, `lastUsedAt`

---

### Merchant Token Mapping

Maintains scoped token relationships so that the same PAN produces different tokens for different merchants, preventing cross-merchant data leakage.

**Mapping Model:**

```
PAN: 4111111111111111
  ├── Merchant A: tok_A1b2C3d4E5f6G7h8
  ├── Merchant B: tok_B9c0D1e2F3g4H5i6
  └── Merchant C: tok_C7d8E9f0G1h2I3j4
```

**Benefits:**
- **Data Isolation** — Merchant A cannot infer Merchant B's customer relationships.
- **Breach Containment** — If one merchant's token database is compromised, other merchants' tokens are unaffected.
- **Compliance** — Supports merchant-specific data retention and deletion policies.
- **Analytics Safety** — Platform can analyze cross-merchant trends using a separate non-reversible token.

**Mapping Operations:**
- `Create Merchant Mapping` — Generate merchant-scoped token for a PAN
- `Delete Merchant Mapping` — Remove a merchant's access to a token (GDPR, account closure)
- `Transfer Mapping` — Move token ownership between merchants (merger, acquisition)
- `Query by Merchant` — List all tokens accessible to a specific merchant

---

### Vault Integration

The Tokenization Service integrates with a hardened, PCI DSS Level 1 certified vault for secure storage of sensitive data.

**Vault Architecture:**

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Application Layer** | Tokenization Service API | Tokenization, detokenization, lifecycle management |
| **Encryption Layer** | AES-256-GCM | Data-at-rest encryption |
| **Key Management Layer** | HSM (FIPS 140-2 Level 3) | Key generation, storage, and cryptographic operations |
| **Storage Layer** | Encrypted database (separate CDE network) | Persistent storage of encrypted PANs and mappings |
| **Network Layer** | Isolated VLAN, no internet egress | Network segmentation for CDE |

**Vault Security Controls:**

| Control | Implementation |
|---------|---------------|
| **Encryption at Rest** | All PANs encrypted with AES-256-GCM; keys rotated quarterly |
| **Encryption in Transit** | TLS 1.3 with mutual authentication for all service communication |
| **Key Rotation** | Automatic key rotation every 90 days; emergency rotation on compromise suspicion |
| **Access Logging** | Every vault access logged immutably; real-time anomaly alerting |
| **HSM Protection** | Master keys stored in FIPS 140-2 Level 3 HSM; no key export |
| **Network Isolation** | Vault in isolated network segment; access only via bastion hosts |
| **Data Masking** | Only masked PANs (e.g., `411111******1111`) exposed outside vault |
| **Secure Deletion** | Crypto-shredding on token deletion; keys destroyed, data irretrievable |

---

### PCI Compliance

The Tokenization Service is designed to minimize PCI DSS scope for the platform and its merchants.

**PCI DSS Scope Reduction:**

| Approach | Scope Impact |
|----------|-------------|
| **Tokenization** | Merchants and most platform services handle only tokens, not PANs → Reduced or eliminated PCI scope |
| **Network Tokens** | Merchants store network tokens instead of PANs → No PCI scope for card storage |
| **Vault Isolation** | Only the Tokenization Service and vault infrastructure are in the CDE |
| **No PAN Logging** | Raw PANs are never logged by any platform service |
| **Secure Transmission** | PANs enter the platform only through the Tokenization Service's secure endpoint |

**Compliance Artifacts:**

| Artifact | Description |
|----------|-------------|
| **PCI DSS AOC** | Attestation of Compliance for the Tokenization Service (Level 1 Service Provider) |
| **Network Token Compliance** | Certification from Visa VTS, Mastercard MDES, Amex Express |
| **Penetration Testing** | Annual third-party penetration test of vault and APIs |
| **ASV Scanning** | Quarterly vulnerability scans by Approved Scanning Vendor |
| **Audit Logs** | Immutable logs of all tokenization, detokenization, and administrative actions |

**Merchant PCI Impact:**
- Merchants using platform tokens → SAQ A (simplest questionnaire, ~20 questions)
- Merchants using network tokens → SAQ A-EP or SAQ D depending on integration
- Merchants handling raw PANs → Full SAQ D (most complex, ~300 questions)
- Platform strongly recommends tokenization to minimize merchant compliance burden.

---

## Security Architecture

### Threat Model & Mitigations

| Threat | Mitigation |
|--------|-----------|
| **Vault Breach** | HSM-protected keys, encryption at rest, network isolation, no single point of failure |
| **Insider Threat** | Role-based access, dual control for key operations, comprehensive audit logging |
| **Token Enumeration** | Cryptographically random token IDs, rate limiting, anomaly detection |
| **Detokenization Abuse** | Strict authorization, mTLS, rate limiting, real-time monitoring, bulk request detection |
| **Man-in-the-Middle** | TLS 1.3, certificate pinning, mutual authentication |
| **Side-Channel Attacks** | Constant-time cryptographic operations, HSM isolation, no timing leaks |

### Token Format

| Format | Example | Description |
|--------|---------|-------------|
| **Platform Token** | `tok_1Ab2Cd3Ef4Gh5Ij6Kl7Mn8Op` | 32-character alphanumeric, prefix `tok_` |
| **Network Token** | `4895123456789012` | 16-digit format, passes Luhn check |
| **Merchant Token** | `mer_tok_Xy9Za8Bc7De6Fg5` | 24-character alphanumeric, prefix `mer_tok_` |
| **Single-Use Token** | `sut_3Gh4Ij5Kl6Mn7Op8Qr9St0` | 28-character alphanumeric, prefix `sut_`, TTL 15 minutes |

---

## Owned Resources

The Tokenization Service is the authoritative owner of the following data:

| Resource | Description |
|----------|-------------|
| **Tokens** | All surrogate tokens (platform, merchant, single-use) with their metadata, state, and lifecycle history. |
| **Vault References** | Encrypted storage references, key identifiers, and HSM-protected encryption mappings for raw PANs and cardholder data. |

> **Important:** The Tokenization Service is the **only** service authorized to store raw PANs. No other platform service, merchant system, or database may persist sensitive cardholder data.

---

## Domain Events

The Tokenization Service publishes the following events for downstream consumers:

| Event | Trigger |
|-------|---------|
| `TokenCreated` | A new token is successfully generated and stored in the vault. |
| `TokenExpired` | A token reaches its natural expiry date (card expiry or TTL expiration). |
| `TokenDeleted` | A token is permanently removed from active use due to customer request, merchant account closure, GDPR deletion, or security incident. |

---

## Integration Notes

- **Payment Service**: Requests tokenization for new payment methods; requests detokenization during transaction authorization; receives token expiry events to block expired tokens.
- **Merchant Service**: Provides merchant configuration for token scoping and PCI compliance level; receives token lifecycle webhooks.
- **User / Customer Service**: Links tokens to customer profiles for wallet and recurring payment management.
- **Fraud Service**: Receives token creation and usage events for risk scoring; may request token suspension on fraud detection.
- **Dispute Service**: Requests detokenization for chargeback representment evidence and refund processing.
- **Refund Service**: Requests detokenization to process refunds to the original payment instrument.
- **Settlement Service**: Uses token references (not PANs) for settlement reporting and reconciliation.
- **Card Networks (Visa, Mastercard, Amex, Discover)**: Integrates via network token APIs (VTS, MDES, Express) for token provisioning, cryptogram generation, and lifecycle event subscription.
- **HSM Provider**: Interfaces with hardware security modules for key generation, storage, and cryptographic operations.
- **Audit Service**: Subscribes to all token events for PCI compliance audits, security investigations, and regulatory reporting.
- **Analytics / BI Service**: Consumes non-reversible token data for payment method analytics, brand distribution, and expiry trend analysis.
