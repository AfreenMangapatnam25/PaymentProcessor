# User Service

## Overview

The User Service is a core platform microservice responsible for managing user identity, profile, contact information, addresses, merchant-scoped customer records, consent, and preferences. It serves as the authoritative source for user-related data while delegating authentication concerns to a dedicated Authentication Service.

---

## Table of Contents

- [Architecture](#architecture)
- [Aggregates](#aggregates)
- [Core Functionalities](#core-functionalities)
  - [User Registration](#user-registration)
  - [User Profile Management](#user-profile-management)
  - [Contact Management](#contact-management)
  - [Address Management](#address-management)
  - [Customer Management](#customer-management)
  - [KYC Status](#kyc-status)
  - [Consent Management](#consent-management)
  - [User Preferences](#user-preferences)
  - [User Status & Lifecycle](#user-status--lifecycle)
  - [Identity Reference](#identity-reference)
  - [User Search](#user-search)
  - [User Audit](#user-audit)
  - [Data Privacy (GDPR)](#data-privacy-gdpr)
- [Event Publishing](#event-publishing)
- [Validation](#validation)
- [Encryption](#encryption)
- [Versioning & Optimistic Locking](#versioning--optimistic-locking)
- [Soft Delete](#soft-delete)

---

## Architecture

The service is modeled around **domain-driven aggregates** rather than a single monolithic `User` entity. This separation improves maintainability, scalability, and clarity of bounded contexts.

### Recommended Aggregates

| Aggregate | Responsibility |
|-----------|----------------|
| **User** | Platform identity, lifecycle status, and identity reference. |
| **UserProfile** | Personal details such as name, date of birth, gender, nationality, language, and time zone. |
| **Contact** | Email addresses and phone numbers, including verification status. |
| **Address** | Multiple physical addresses with default selection support. |
| **Customer** | Merchant-scoped customer record linked to a platform user. |
| **Consent** | Terms, privacy, and marketing consent with a full audit trail. |
| **Preference** | User-specific application preferences (language, currency, theme, notifications). |

---

## Core Functionalities

### User Registration

Handles the creation of platform users.

**Responsibilities:**
- Register a new user
- Verify email and mobile number
- Generate a unique user ID
- Publish a `UserCreated` domain event

---

### User Profile Management

Manages personal and demographic information.

**Stored Fields:**
- First Name
- Last Name
- Date of Birth (DOB)
- Gender
- Nationality
- Preferred Language
- Time Zone

**Supported Operations:**
- `Get Profile`
- `Update Profile`
- `Partial Update Profile`

---

### Contact Management

Maintains communication details with verification tracking.

**Stored Fields:**
- Email
- Mobile Number
- Alternate Phone

**Supported Operations:**
- `Add Email`
- `Verify Email`
- `Change Email`
- `Add Phone`
- `Verify Phone`

---

### Address Management

Supports multiple addresses per user with type classification.

**Address Types:**
- Home
- Office
- Billing
- Shipping

**Supported Operations:**
- `Add Address`
- `Update Address`
- `Delete Address`
- `Mark Default`

---

### Customer Management

In payment processing, a **customer** is merchant-specific. Even if the same person shops with multiple merchants, they should have distinct customer records.

**Example:**
```
Merchant A  →  Customer 101
Merchant B  →  Customer 842
```

**Supported Operations:**
- `Create Customer`
- `Get Customer`
- `Link User`
- `Disable Customer`

---

### KYC Status

The User Service **does not perform KYC** itself. It only stores and reflects the current KYC status received from the KYC Service.

**Status Values:**
- `PENDING`
- `VERIFIED`
- `FAILED`
- `EXPIRED`

---

### Consent Management

Stores user consent with a full audit history.

**Consent Types:**
- Terms & Conditions accepted
- Privacy Policy accepted
- Marketing emails
- SMS notifications
- Cookie consent

---

### User Preferences

Stores user-specific application preferences.

**Preference Types:**
- Language
- Currency
- Theme
- Notification Preferences

---

### User Status & Lifecycle

Tracks the lifecycle state of a user account.

**Status Enum:**
```java
enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    SUSPENDED,
    DELETED
}
```

---

### Identity Reference

Authentication is delegated to a separate **Authentication Service**.

- **User Service** stores only: `identityId`
- **Authentication Service** owns:
  - Password
  - MFA
  - Login
  - Refresh Token
  - Sessions

---

### User Search

Search users by the following identifiers:

- User ID
- Email
- Phone
- Customer ID
- Merchant ID

**Features:**
- Pagination supported

---

### User Audit

Tracks all significant changes for compliance and traceability.

**Tracked Events:**
- Profile Updated
- Address Changed
- Email Verified
- Consent Accepted
- Status Changed

---

### Data Privacy (GDPR)

Supports the following privacy operations:

- **Data Export** — Provide a complete copy of user data
- **Data Masking** — Redact sensitive fields in non-production contexts
- **Soft Delete** — Mark user as deleted without physical removal
- **Crypto Shredding** — Preferred over hard delete for payment systems; encryption keys are destroyed, rendering data irretrievable

---

## Event Publishing

The service publishes domain events for downstream consumers (e.g., Kafka or RabbitMQ) to update their local read models.

**Published Events:**
- `UserCreated`
- `UserUpdated`
- `AddressAdded`
- `EmailVerified`
- `CustomerCreated`
- `CustomerDisabled`
- `ConsentUpdated`

---

## Validation

Input validation is enforced across the following dimensions:

- Email uniqueness
- Phone uniqueness
- Country code validity
- Postal code format
- Date of Birth (DOB) validity
- Age restrictions

---

## Encryption

All Personally Identifiable Information (PII) is encrypted at rest.

**Encrypted Fields:**
- Name
- Date of Birth
- Email
- Phone
- Address

> **Note:** Decrypted data must not be exposed unnecessarily.

---

## Versioning & Optimistic Locking

Each aggregate supports optimistic locking to prevent concurrent update conflicts.

**Versioning Fields:**
- `version`
- `updatedAt`
- `updatedBy`

---

## Soft Delete

Users are **never physically deleted** from the system.

**Soft Delete Fields:**
- `deleted = true`
- `deletedAt`
- `deletedBy`

---

## Tech Stack Context

This service is designed to integrate with:

- **Spring Boot 3.x**
- **Java 21**
- **MongoDB**

It is part of a broader banking application covering transactional concerns such as CRUD operations, entity relationships, N+1 query resolution, optimistic locking, pessimistic locking, MVCC, and write skew scenarios.
