# Merchant Service

## Overview

The Merchant Service is the central domain service responsible for managing businesses that use the payment processing
platform. It governs the full merchant lifecycle — from onboarding and business verification to ongoing operations,
configuration, and settlement. Every merchant record represents a distinct business entity with its own profile,
compliance status, financial accounts, and operational settings.

This service acts as the bridge between the platform and its commercial users, ensuring that only verified, compliant
businesses can process payments, receive settlements, and configure their integration.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Core Functionalities](#core-functionalities)
    - [Merchant Onboarding](#merchant-onboarding)
    - [Business Verification (KYB)](#business-verification-kyb)
    - [Merchant Profile Management](#merchant-profile-management)
    - [Business Addresses](#business-addresses)
    - [Bank & Settlement Accounts](#bank--settlement-accounts)
    - [Merchant Status Lifecycle](#merchant-status-lifecycle)
    - [Merchant KYC / KYB Compliance](#merchant-kyc--kyb-compliance)
    - [Fee Configuration & Pricing Plans](#fee-configuration--pricing-plans)
    - [Supported Payment Methods](#supported-payment-methods)
    - [Merchant Webhooks](#merchant-webhooks)
    - [API Keys](#api-keys)
    - [Merchant Configuration](#merchant-configuration)
    - [Merchant Branding](#merchant-branding)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern                           | Description                                                                               |
|-----------------------------------|-------------------------------------------------------------------------------------------|
| **Merchant Lifecycle Management** | Govern the complete journey from application to activation, suspension, and closure.      |
| **Business Verification**         | Validate legal business existence, ownership structure, and operational legitimacy (KYB). |
| **Financial Configuration**       | Manage settlement accounts, fee structures, and payout schedules.                         |
| **Integration Provisioning**      | Issue API credentials, configure webhooks, and enable payment method support.             |
| **Compliance & Risk**             | Track KYC/KYB status and enforce compliance gates before transaction authorization.       |
| **Operational Customization**     | Allow merchants to tailor branding, settings, and notification preferences.               |

---

## Core Functionalities

### Merchant Onboarding

The entry point for businesses joining the platform. Onboarding collects essential business information, initiates
verification workflows, and provisions the merchant account.

**Onboarding Flow:**

1. **Application Submission** — Collect business legal name, registration number, industry, and ownership details.
2. **Document Upload** — Request business licenses, tax IDs, bank statements, and identity proofs of beneficial owners.
3. **Verification Queue** — Submit to KYB/KYC pipelines for automated and manual review.
4. **Account Provisioning** — Create merchant record, generate API keys, and configure default settings upon approval.
5. **Activation** — Publish `MerchantActivated` event; merchant can begin processing.

---

### Business Verification (KYB)

Know Your Business (KYB) verifies the legal and operational legitimacy of the applying entity.

**Verification Dimensions:**

| Check                         | Description                                                                      |
|-------------------------------|----------------------------------------------------------------------------------|
| **Legal Existence**           | Validate business registration with government or corporate registries.          |
| **Ownership Structure**       | Identify beneficial owners, directors, and authorized signatories.               |
| **Operational Legitimacy**    | Verify website, business address, and trading activity.                          |
| **Sanctions & PEP Screening** | Screen against global sanctions lists and politically exposed persons databases. |
| **Credit & Risk Assessment**  | Evaluate financial health and industry risk profile.                             |

---

### Merchant Profile Management

Maintains the core identity and descriptive attributes of the merchant.

**Stored Fields:**

- Legal Business Name
- Trading / DBA Name (Doing Business As)
- Business Registration Number
- Tax / VAT ID
- Industry / MCC (Merchant Category Code)
- Business Type (Sole Proprietorship, Partnership, Corporation, etc.)
- Website URL
- Support Contact Email
- Support Phone Number

**Operations:**

- `Get Merchant Profile`
- `Update Merchant Profile`
- `Partial Update Merchant Profile`

---

### Business Addresses

Merchants may have multiple addresses for different purposes.

**Address Types:**

| Type                   | Purpose                                            |
|------------------------|----------------------------------------------------|
| **Registered Address** | Official legal address from business registration. |
| **Operating Address**  | Primary place of business operations.              |
| **Billing Address**    | Address for platform fee invoicing.                |
| **Mailing Address**    | Correspondence and document delivery.              |

**Operations:**

- `Add Business Address`
- `Update Business Address`
- `Delete Business Address`
- `Mark Primary Address`

---

### Bank & Settlement Accounts

Manages the financial destinations where processed funds are settled.

**Account Types:**

| Type                   | Description                                                          |
|------------------------|----------------------------------------------------------------------|
| **Settlement Account** | Primary bank account for receiving payout of processed transactions. |
| **Reserve Account**    | Account for holding rolling reserves or risk collateral.             |
| **Fee Account**        | Dedicated account for platform fee deductions (optional).            |

**Stored Fields:**

- Account Holder Name
- Bank Name
- Bank Code / Routing Number / SWIFT / IBAN
- Account Number (encrypted)
- Currency
- Country
- Account Type (Checking / Savings / Current)

**Operations:**

- `Add Settlement Account`
- `Update Settlement Account`
- `Delete Settlement Account`
- `Mark Default Settlement Account`
- `Validate Account` (micro-deposit or instant verification)

> **Security:** Bank account numbers are encrypted at rest. Decrypted values are never exposed through APIs.

---

### Merchant Status Lifecycle

Tracks the operational state of a merchant throughout their relationship with the platform.

**Status Enum:**

```java
enum MerchantStatus {
    PENDING,        // Application submitted, awaiting verification
    UNDER_REVIEW,   // KYB/KYC review in progress
    ACTIVE,         // Fully operational, can process payments
    SUSPENDED,      // Temporarily disabled due to risk or compliance
    RESTRICTED,     // Limited operations (e.g., no payouts, only processing)
    TERMINATED,     // Permanently closed, no longer on platform
    BLACKLISTED     // Blocked from re-onboarding due to fraud or violation
}
```

**Status Transitions:**

- `PENDING` → `UNDER_REVIEW` → `ACTIVE`
- `ACTIVE` → `SUSPENDED` (risk trigger, compliance violation)
- `SUSPENDED` → `ACTIVE` (issue resolved) or `TERMINATED` (irreversible)
- `ACTIVE` → `RESTRICTED` (velocity limit, reserve requirement)

---

### Merchant KYC / KYB Compliance

Tracks the compliance verification status of the merchant and its key personnel.

**KYC Status (for beneficial owners / directors):**

- `PENDING`
- `VERIFIED`
- `FAILED`
- `EXPIRED`

**KYB Status (for the business entity):**

- `PENDING`
- `VERIFIED`
- `FAILED`
- `EXPIRED`

**Behavior:**

- A merchant cannot be activated until both KYB and all associated KYC statuses are `VERIFIED`.
- Periodic re-verification is triggered based on risk profile and regulatory requirements.
- Status updates are received from the KYC Service and KYB Service, respectively.

---

### Fee Configuration & Pricing Plans

Defines the cost structure applied to the merchant's transactions.

**Fee Components:**

| Component                        | Description                                                 |
|----------------------------------|-------------------------------------------------------------|
| **Transaction Fee**              | Percentage or fixed amount per transaction.                 |
| **Interchange Fee Pass-Through** | Pass interchange costs directly to merchant or absorb them. |
| **Monthly Platform Fee**         | Recurring subscription or SaaS fee.                         |
| **Chargeback Fee**               | Fixed fee applied per chargeback incident.                  |
| **Refund Fee**                   | Fee applied when a refund is processed.                     |
| **Payout Fee**                   | Fee for transferring funds to the settlement account.       |

**Pricing Plans:**

- `STANDARD` — Default fee schedule for new merchants.
- `VOLUME_TIERED` — Reduced rates based on monthly transaction volume.
- `ENTERPRISE` — Custom-negotiated rates for large merchants.
- `STARTER` — Discounted rates for small businesses or pilot programs.

**Operations:**

- `Assign Pricing Plan`
- `Configure Custom Fees`
- `Preview Fee Calculation`

---

### Supported Payment Methods

Controls which payment instruments the merchant is enabled to accept.

**Payment Methods:**

| Method                   | Description                                     |
|--------------------------|-------------------------------------------------|
| **Credit / Debit Cards** | Visa, Mastercard, Amex, Discover, JCB, UnionPay |
| **Bank Transfers**       | ACH, SEPA, Wire, FPS                            |
| **Digital Wallets**      | Apple Pay, Google Pay, PayPal, Samsung Pay      |
| **Buy Now Pay Later**    | Klarna, Afterpay, Affirm                        |
| **Cryptocurrency**       | BTC, ETH (if supported by platform)             |
| **Local Methods**        | iDEAL, Bancontact, Giropay, etc.                |

**Operations:**

- `Enable Payment Method`
- `Disable Payment Method`
- `Configure Method-Specific Settings` (e.g., 3DS preferences for cards)

---

### Merchant Webhooks

Allows merchants to receive real-time event notifications at their endpoints.

**Webhook Events:**

- `payment.succeeded`
- `payment.failed`
- `refund.processed`
- `chargeback.received`
- `payout.completed`
- `merchant.status_changed`

**Configuration:**

- Endpoint URL
- Event types to subscribe
- Secret for HMAC signature verification
- Retry policy (exponential backoff)
- Timeout settings

**Operations:**

- `Register Webhook Endpoint`
- `Update Webhook Configuration`
- `Delete Webhook Endpoint`
- `Test Webhook Delivery`
- `View Webhook Delivery Logs`

---

### API Keys

Provisions and manages credentials for merchant API integration.

**Key Types:**

| Type                       | Purpose                                                          |
|----------------------------|------------------------------------------------------------------|
| **Production Key**         | Live transaction processing and production data access.          |
| **Sandbox Key**            | Testing and integration development without real money movement. |
| **Read-Only Key**          | Access to GET endpoints only; no mutation allowed.               |
| **Webhook Signing Secret** | Used by merchants to verify webhook authenticity.                |

**Operations:**

- `Generate API Key Pair` (public / secret)
- `Rotate API Key`
- `Revoke API Key`
- `List Active Keys`
- `Configure IP Allowlist`

> **Security:** Secret keys are shown only once upon generation and are hashed before storage.

---

### Merchant Configuration

Centralized settings governing merchant behavior and platform interaction.

**Configuration Areas:**

| Area             | Settings                                                                                       |
|------------------|------------------------------------------------------------------------------------------------|
| **Transaction**  | Auto-capture vs. manual capture, 3DS enforcement, velocity limits                              |
| **Payout**       | Payout schedule (daily, weekly, monthly), minimum payout threshold, instant payout eligibility |
| **Risk**         | Fraud screening level, chargeback threshold alerts, reserve percentage                         |
| **Notification** | Email alerts for payouts, chargebacks, account status changes                                  |
| **Compliance**   | Required KYC refresh interval, document expiry reminders                                       |

---

### Merchant Branding

Allows merchants to customize the payment experience presented to their customers.

**Branding Elements:**

| Element                   | Description                                                      |
|---------------------------|------------------------------------------------------------------|
| **Logo**                  | Merchant logo displayed on checkout pages and email receipts.    |
| **Brand Colors**          | Primary and secondary colors for checkout theming.               |
| **Business Name Display** | How the business name appears on customer card statements.       |
| **Custom Domain**         | Optional white-label checkout domain (e.g., `pay.merchant.com`). |
| **Email Templates**       | Branded receipt and notification emails.                         |

---

## Owned Resources

The Merchant Service is the authoritative owner of the following data:

| Resource                   | Description                                                                   |
|----------------------------|-------------------------------------------------------------------------------|
| **Merchant**               | Core merchant identity, status, and lifecycle record.                         |
| **Business Details**       | Legal name, registration, tax ID, industry, ownership structure.              |
| **Settlement Accounts**    | Bank account details for fund disbursement (encrypted).                       |
| **Merchant Configuration** | Operational settings, payout schedules, risk thresholds, and feature toggles. |

> **Note:** Transaction data is owned by the Transaction Service. User/Customer data is owned by the User Service.
> KYC/KYB verification logic is owned by the Compliance Service; this service only stores the resulting status.

---

## Domain Events

The Merchant Service publishes the following events for downstream consumers:

| Event               | Trigger                                                                             |
|---------------------|-------------------------------------------------------------------------------------|
| `MerchantCreated`   | A new merchant application is submitted and the record is created.                  |
| `MerchantActivated` | Merchant passes all verification checks and is enabled for processing.              |
| `MerchantSuspended` | Merchant is temporarily disabled due to risk, compliance, or administrative action. |
| `MerchantUpdated`   | Any material change to merchant profile, configuration, or status.                  |

---

## Integration Notes

- **User Service**: Merchants are linked to platform users (e.g., merchant owner accounts) via `userId` references.
- **Authentication / Authorization Service**: Merchant admins authenticate through the platform and are authorized via
  merchant-scoped roles.
- **KYC / KYB Service**: Receives verification results to update merchant and beneficial owner compliance status.
- **Transaction Service**: Queries merchant configuration (payment methods, fees, risk settings) during transaction
  processing.
- **Payout / Settlement Service**: Reads settlement account details and payout schedules to execute fund transfers.
- **Notification Service**: Consumes merchant events to send onboarding reminders, status alerts, and webhook
  deliveries.
- **Audit Service**: Subscribes to all merchant events for regulatory compliance and dispute resolution.

---

## Implementation

This repository contains a production-oriented Spring Boot implementation of the service described above.

### Tech stack

- **Java 17**, **Spring Boot 3.3** (Web, Data JPA, Validation, Security, Actuator)
- **PostgreSQL** with **Flyway** migrations (`spring.jpa.hibernate.ddl-auto=validate`)
- **Spring Kafka** for domain-event publishing via a **transactional outbox**
- **springdoc-openapi** (Swagger UI), **Micrometer/Prometheus** metrics, **Lombok**

### Architecture

```
controller  ──▶  service  ──▶  repository  ──▶  PostgreSQL
                   │
                   ├─▶ OutboxWriter ─(same TX)─▶ outbox_event table
                   │
   OutboxRelay (scheduled) ─▶ Kafka  ─▶ downstream services
```

- **Layering:** thin REST controllers → transactional services holding all business rules → Spring Data JPA
  repositories. DTOs (validated Java records) never expose entities directly.
- **Transactional outbox:** every state change appends a `DomainEvent` to `outbox_event` in the *same* transaction.
  `OutboxRelay` polls pending rows on a schedule and publishes them to Kafka (at-least-once; consumers must be
  idempotent via `eventId`). Events: `MerchantCreated`, `MerchantActivated`, `MerchantSuspended`, `MerchantUpdated`,
  `MerchantStatusChanged`, `MerchantConfigurationUpdated`.
- **Lifecycle:** `MerchantStatusPolicy` enforces legal transitions; activation is gated on `KYB = VERIFIED` and all
  beneficial owners `KYC = VERIFIED`.
- **Security at rest:** bank account numbers are encrypted with AES-256-GCM (`EncryptionService`); only the last 4
  digits are ever returned. API-key and webhook secrets are SHA-256 hashed and shown exactly once.
- **AuthN/Z:** stateless API-key authentication (`Authorization: Bearer <keyId>.<secret>` or a platform admin key).
  READ_ONLY keys are limited to GET; onboarding, lifecycle transitions, KYB decisions and pricing are admin-only;
  merchant keys are scoped to their own `merchantId`.

### Configuration (environment variables)

| Variable                               | Purpose                                          | Default (dev)    |
|----------------------------------------|--------------------------------------------------|------------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection                            | local Postgres   |
| `KAFKA_BOOTSTRAP_SERVERS`              | Kafka brokers                                    | `localhost:9092` |
| `MERCHANT_ENCRYPTION_KEY`              | Base64 256-bit AES key (**override in prod**)    | dev key          |
| `MERCHANT_ADMIN_API_KEY`               | Platform admin credential (**override in prod**) | dev key          |

### Build & run

```bash
# One-time: generate the Gradle wrapper (needs a local Gradle 8.5+; see gradle-wrapper-note.md)
gradle wrapper --gradle-version 8.9

# Run the full stack (Postgres + Kafka + service)
docker compose up --build

# Or run locally against your own Postgres/Kafka
./gradlew bootRun

# Tests
./gradlew test
```

API docs: `http://localhost:8080/swagger-ui.html` · Health: `http://localhost:8080/actuator/health`

### Notable API surface

`/api/v1/merchants` (onboard, list) · `/{id}` (get/put/patch) · `/{id}/status` · `/{id}/pricing-plan` ·
`/{id}/addresses` · `/{id}/settlement-accounts` · `/{id}/beneficial-owners` · `/{id}/kyb/cases` ·
`/{id}/kyb/documents` ·
`/{id}/fees` (+`/preview`) · `/{id}/payment-methods` · `/{id}/webhooks` · `/{id}/api-keys` · `/{id}/configuration` ·
`/{id}/branding`

> The pre-existing `bin/` and `build/` directories are stale IDE/Gradle output and can be deleted; they are ignored by
`.gitignore` and regenerated on build.
