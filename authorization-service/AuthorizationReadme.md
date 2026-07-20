# Authorization Service

## Overview

The Authorization Service is the central policy enforcement point for the platform. It determines what an authenticated identity — whether a user, merchant, or administrator — is permitted to do. It evaluates access decisions based on roles, permissions, attributes, and contextual policies, returning real-time approve/decline verdicts for both API-level and resource-level requests.

This service operates downstream of the Authentication Service. While Authentication answers *"Who are you?"*, Authorization answers *"What are you allowed to do?"*

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Core Functionalities](#core-functionalities)
  - [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
  - [Attribute-Based Access Control (ABAC)](#attribute-based-access-control-abac)
  - [Role & Permission Management](#role--permission-management)
  - [API Authorization](#api-authorization)
  - [Resource Authorization](#resource-authorization)
  - [Scope Validation](#scope-validation)
  - [Policy Evaluation Engine](#policy-evaluation-engine)
  - [Permission Caching](#permission-caching)
  - [Real-Time Decisioning](#real-time-decisioning)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern | Description |
|---------|-------------|
| **Access Decisioning** | Evaluate and render real-time approve/decline verdicts for every protected action. |
| **Role & Permission Governance** | Define, assign, and lifecycle-manage roles and their associated permissions. |
| **Policy Enforcement** | Interpret and apply attribute-based and context-aware policies across the platform. |
| **Scope Validation** | Ensure OAuth2/API scopes granted during authentication match the requested operation. |
| **Real-Time Controls** | Support transactional authorization scenarios such as fund holds, AVS checks, and card validation. |
| **Performance at Scale** | Cache permission resolutions to minimize latency on high-volume authorization checks. |

---

## Core Functionalities

### Role-Based Access Control (RBAC)

RBAC is the primary authorization model. Access is granted through roles assigned to identities, and roles carry collections of permissions.

**Key Concepts:**
- **Role** — A named collection of permissions representing a job function (e.g., `MERCHANT_ADMIN`, `SUPPORT_AGENT`).
- **Permission** — A granular, atomic right to perform a specific action on a resource (e.g., `transaction:read`, `user:update`).
- **Role Assignment** — The binding of a role to an identity within a specific context (global, merchant-scoped, or resource-scoped).

**Role Types:**

| Category | Examples |
|----------|----------|
| **Platform Roles** | `SUPER_ADMIN`, `PLATFORM_AUDITOR`, `SYSTEM` |
| **Merchant Roles** | `MERCHANT_OWNER`, `MERCHANT_ADMIN`, `MERCHANT_VIEWER`, `MERCHANT_BILLING` |
| **User Roles** | `END_USER`, `PREMIUM_USER` |

---

### Attribute-Based Access Control (ABAC)

ABAC extends RBAC by evaluating dynamic attributes at decision time. This enables fine-grained, context-aware authorization beyond static role assignments.

**Evaluated Attributes:**

| Attribute Type | Examples |
|----------------|----------|
| **Subject (User)** | KYC status, account tier, age, location |
| **Resource** | Merchant ID, transaction amount, account balance, card type |
| **Action** | CREATE, READ, UPDATE, DELETE, APPROVE, DECLINE |
| **Environment** | Time of day, IP address, device trust level, geo-risk score |

**Example ABAC Policy:**
```
ALLOW transaction:create
IF user.role == "MERCHANT_ADMIN"
AND transaction.amount <= merchant.daily_limit
AND user.kyc_status == "VERIFIED"
AND device.trust_level >= 0.8
AND time_of_day BETWEEN 06:00 AND 22:00
```

> ABAC is optional for initial rollout but recommended for high-sensitivity operations.

---

### Role & Permission Management

Administrative operations for governing the authorization model.

**Role Operations:**
- Create / Update / Delete role definitions
- Assign permissions to roles
- Assign roles to identities (users, merchants, admins)
- List roles by identity or scope
- Revoke role assignments

**Permission Operations:**
- Define new permissions with resource and action granularity
- Grant permissions to roles
- Revoke permissions from roles
- Query effective permissions for an identity

---

### API Authorization

Protects REST and gRPC endpoints by evaluating whether the caller's identity and scopes permit access to the requested operation.

**Mechanism:**
1. Extract identity claims from the JWT (via Authentication Service).
2. Resolve effective roles and permissions for the identity.
3. Validate that the requested endpoint/action is within the permission set.
4. Return `ALLOW` or `DENY` with an optional reason code.

---

### Resource Authorization

Evaluates access to specific data instances, not just endpoint-level access.

**Examples:**
- Can Merchant A's admin view Merchant B's transactions? → **DENY**
- Can User 101 update their own profile? → **ALLOW**
- Can User 101 update User 102's profile? → **DENY**

**Mechanism:**
- Enforce ownership checks (e.g., `resource.merchantId == caller.merchantId`).
- Apply ABAC policies for cross-merchant or escalated access.
- Support delegation patterns (e.g., admin acting on behalf of a user).

---

### Scope Validation

Validates that the OAuth2 or API scopes granted during the authentication flow authorize the current request.

**Scope Examples:**
- `profile:read` — Read own profile
- `profile:write` — Update own profile
- `transactions:read` — Read transaction history
- `payments:write` — Initiate payments
- `admin:merchants` — Full merchant administration

**Behavior:**
- Reject requests where the required scope is absent from the token.
- Support scope hierarchies and wildcards where applicable.

---

### Policy Evaluation Engine

The core decisioning component that interprets policies and renders access verdicts.

**Evaluation Modes:**

| Mode | Description |
|------|-------------|
| **Allow-List (Default Deny)** | Only explicitly permitted actions are allowed. |
| **Deny-List** | Everything is allowed except explicitly denied actions. |
| **Conflict Resolution** | Explicit deny overrides allow (deny-by-default on conflict). |

**Policy Types:**
- Static role-permission mappings
- Dynamic ABAC rules
- Time-bound or context-bound conditional policies
- Hierarchical inheritance (e.g., merchant roles inherit from platform templates)

---

### Permission Caching

To achieve sub-millisecond authorization latency, resolved permission sets are cached.

| Cache Strategy | Description |
|----------------|-------------|
| **Identity-Level Cache** | Cache the full effective permission set per identity. |
| **Role-Level Cache** | Cache permission-to-role mappings. |
| **Invalidation** | Evict on role assignment changes, permission grants, or policy updates. |
| **TTL** | Short TTL (e.g., 5 minutes) with eager refresh for high-traffic identities. |

---

### Real-Time Decisioning

Beyond static access control, the Authorization Service supports transactional, real-time authorization scenarios common in payment and financial systems.

| Capability | Description |
|------------|-------------|
| **Real-Time Approve / Decline** | Evaluate risk signals, velocity limits, and policy rules to approve or decline a transaction in real time. |
| **Funds Holds** | Verify sufficient funds and place a temporary hold as part of the authorization decision. |
| **AVS (Address Verification Service)** | Validate billing address against card issuer records as a policy input. |
| **Card Validation** | Check card status (active, expired, blocked) and BIN risk before authorization. |

**Decision Flow:**
1. Receive authorization request (transaction, API call, or resource access).
2. Evaluate RBAC/ABAC policies.
3. Apply real-time rules (velocity, limits, AVS, card state).
4. Render verdict: `APPROVED`, `DECLINED`, or `CHALLENGE` (step-up MFA required).
5. Publish decision event for audit and downstream processing.

---

## Owned Resources

The Authorization Service is the authoritative owner of the following data:

| Resource | Description |
|----------|-------------|
| **Roles** | Named role definitions with metadata, hierarchy, and scope constraints. |
| **Permissions** | Atomic action rights mapped to resources and operations. |
| **Policies** | ABAC rules and conditional access logic. |
| **Role Assignments** | Bindings between identities and roles, including scope and validity period. |

> **Note:** Identity profiles and credentials are owned by the User Service and Authentication Service, respectively. This service consumes identity claims but does not own them.

---

## Domain Events

The Authorization Service publishes the following events for downstream consumers:

| Event | Trigger |
|-------|---------|
| `RoleAssigned` | A role is assigned to an identity. |
| `PermissionGranted` | A permission is added to a role or directly to an identity. |
| `PermissionRevoked` | A permission is removed from a role or identity. |

---

## Integration Notes

- **Authentication Service**: Consumes JWT claims and identity assertions to resolve roles and permissions.
- **User Service**: References user attributes (KYC status, tier) for ABAC policy evaluation.
- **API Gateway**: Calls the Authorization Service for per-request access decisions before routing to backend services.
- **Payment / Transaction Service**: Requests real-time approve/decline verdicts, fund hold checks, and card validation.
- **Audit Service**: Subscribes to all authorization events and decision logs for compliance and forensic analysis.
- **Risk Engine**: Provides risk scores and velocity data as ABAC policy inputs.

---

# Implementation Guide

> This section documents the production implementation of the service. In addition to the
> RBAC/ABAC access-control capabilities described above, the service also implements **payment
> authorization** (obtaining approve/decline verdicts from a card gateway), matching the
> transactional real-time decisioning responsibilities. Both capabilities share one deployable.

## Technology

- Java 17, Spring Boot 3.3, Spring Data JPA (PostgreSQL), Spring Kafka, Netflix Eureka client
- **Stripe** as the external payment gateway (manual-capture PaymentIntents)
- Resilience4j (retry + circuit breaker) around gateway calls
- Caffeine permission cache, Flyway migrations, springdoc OpenAPI, Micrometer/Prometheus

## Architecture

```
api/            REST controllers (payment + access control + admin)
service/payment Authorization, capture, reversal, re-auth, idempotency, maintenance
service/access  RBAC resolver, scope validation, ABAC policy engine, decision service, admin
gateway/        GatewayClient abstraction + Stripe implementation + resilience wrapper
domain/         JPA entities (payment + access) and enums
repository/     Spring Data repositories
event/          Kafka domain-event publisher (authorization + access-control events)
security/       JWT claim extraction (identity token from the Authentication Service)
config/         Typed properties, caching, async, Kafka topics, OpenAPI
```

The external gateway call is performed **outside** any database transaction; persistence is handled
by the transactional `AuthorizationStore`. Authorizations are idempotent via the `Idempotency-Key`
header and duplicate active authorizations for the same `paymentReference` are collapsed.

## Payment Authorization API (`/api/v1/authorizations`)

| Method & Path | Purpose |
|---------------|---------|
| `POST /` (header `Idempotency-Key`) | Authorize a payment (place a hold) |
| `GET /{id}` | Authorization inquiry |
| `GET /?paymentReference=...` | List authorizations for a payment |
| `POST /{id}/capture` | Capture a hold (full or partial) |
| `POST /{id}/reversal` | Reverse (void) a hold |
| `POST /{id}/reauthorize` | Re-authorize an expired/insufficient auth |
| `POST /{id}/synchronize` | Refresh state from the gateway (e.g. after 3-DS) |

### Stripe mapping

| Operation | Stripe |
|-----------|--------|
| Authorize | `PaymentIntent.create(capture_method=manual, confirm=true)` → `requires_capture` = hold placed |
| Capture   | `PaymentIntent.capture(amount_to_capture?)` |
| Reversal  | `PaymentIntent.cancel(...)` |
| 3-D Secure | `requires_action` → `REQUIRES_AUTHENTICATION` with a redirect URL |

## Access Control API

- `POST /api/v1/access/check` — ALLOW / DENY / CHALLENGE decision (RBAC + ownership + scopes + ABAC)
- `POST /api/v1/access/scopes/validate` — OAuth2 scope validation
- `GET  /api/v1/access/identities/{id}/permissions` — effective permissions
- `/api/v1/admin/**` — manage permissions, roles, assignments and policies

## Domain events (Kafka)

- `authorization.events` — `AuthorizationApproved/Declined/PartiallyApproved/RequiresAuthentication/Captured/Reversed/Expired/Reauthorized`
- `authorization.access.events` — `RoleAssigned/RoleRevoked/PermissionGranted/PermissionRevoked`

## Configuration (environment variables)

| Variable | Purpose |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `local` \| `dev` \| `prod` |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials (dev/prod) |
| `STRIPE_API_KEY` | Stripe secret key (`sk_test_...` locally) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers |
| `JWT_HMAC_SECRET` or `JWT_PUBLIC_KEY`, `JWT_ISSUER` | Identity-token verification |
| `SSL_KEYSTORE_PATH`, `SSL_KEYSTORE_PASSWORD` | TLS (prod) |

## Running locally

```bash
# 1. Postgres + Kafka available locally; create the database:
createdb authorization_local

# 2. Provide a Stripe test key and run (Flyway creates the schema and seeds baseline roles):
export STRIPE_API_KEY=sk_test_xxx
gradle bootRun --args='--spring.profiles.active=local'

# API docs: http://localhost:8086/swagger-ui.html
# Health:   http://localhost:8086/actuator/health
```

Build a container: `docker build -t authorization-service .`

## Notes

- Card data is always **tokenized upstream**; only network-safe metadata (BIN, last4, brand,
  expiry) is persisted — no PAN/CVV is stored.
- Schema is owned by Flyway (`src/main/resources/db/migration`); Hibernate runs in `validate` mode.
- No automated tests are included per the project requirement.
