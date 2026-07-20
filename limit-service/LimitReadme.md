# Limit Service

## Overview

The Limit Service is the platform's financial guardrail system. It enforces configurable transaction boundaries across
multiple dimensions — time, amount, count, geography, and entity — to prevent overspending, mitigate fraud exposure,
ensure regulatory compliance, and protect both merchants and customers from unintended financial harm.

Every transaction is evaluated against applicable limits before authorization. The service supports both **hard limits
** (absolute blocks) and **soft limits** (alerts with optional override workflows). It operates on a *
*reserve-then-commit** pattern: limits are reserved during transaction processing and released or consumed based on the
final outcome.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Limit Types](#limit-types)
- [Core Functionalities](#core-functionalities)
    - [Daily Limits](#daily-limits)
    - [Weekly Limits](#weekly-limits)
    - [Monthly Limits](#monthly-limits)
    - [Transaction Count Limits](#transaction-count-limits)
    - [Merchant Limits](#merchant-limits)
    - [Customer Limits](#customer-limits)
    - [Country Limits](#country-limits)
    - [Currency Limits](#currency-limits)
    - [Velocity Limits](#velocity-limits)
    - [Credit Limits](#credit-limits)
    - [Limit Reservation](#limit-reservation)
    - [Limit Release](#limit-release)
- [Limit Enforcement Model](#limit-enforcement-model)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern                    | Description                                                                                                     |
|----------------------------|-----------------------------------------------------------------------------------------------------------------|
| **Boundary Enforcement**   | Prevent transactions from exceeding defined financial, geographic, or frequency thresholds.                     |
| **Fraud Containment**      | Cap exposure from compromised accounts, stolen cards, or merchant abuse through velocity and amount controls.   |
| **Regulatory Compliance**  | Enforce cross-border remittance limits, currency controls, and reporting thresholds mandated by jurisdictions.  |
| **Financial Protection**   | Protect customers from overspending and merchants from chargeback liability through credit and exposure limits. |
| **Operational Resilience** | Reserve capacity during transaction processing to prevent race conditions and double-spending against limits.   |
| **Flexible Governance**    | Support tiered, merchant-specific, and customer-specific limit configurations with override workflows.          |

---

## Limit Types

Limits are classified by their scope and the dimension they constrain.

### By Time Window

| Type               | Window                                           | Reset Behavior                |
|--------------------|--------------------------------------------------|-------------------------------|
| **Daily Limit**    | 00:00–23:59 in the entity's configured time zone | Resets at midnight local time |
| **Weekly Limit**   | Monday 00:00 – Sunday 23:59                      | Resets at start of week       |
| **Monthly Limit**  | 1st – last day of month                          | Resets at start of month      |
| **Rolling Window** | Last N hours/days (e.g., last 24h)               | Continuously sliding          |

### By Constraint Dimension

| Type               | Constrains                | Example                                  |
|--------------------|---------------------------|------------------------------------------|
| **Amount Limit**   | Total monetary value      | $5,000 per day                           |
| **Count Limit**    | Number of transactions    | 10 transactions per hour                 |
| **Velocity Limit** | Rate of change or pattern | 3x increase in volume vs. 30-day average |
| **Credit Limit**   | Outstanding exposure      | $10,000 rolling credit line              |

### By Entity Scope

| Type                      | Applies To                                | Configuration Level        |
|---------------------------|-------------------------------------------|----------------------------|
| **Platform Global Limit** | All transactions on the platform          | Platform admin             |
| **Merchant Limit**        | All transactions for a specific merchant  | Merchant admin or platform |
| **Customer Limit**        | All transactions for a specific customer  | Customer or platform       |
| **Country Limit**         | Transactions involving a specific country | Platform compliance        |
| **Currency Limit**        | Transactions in a specific currency       | Platform risk              |

---

## Core Functionalities

### Daily Limits

Controls the total transaction volume and count within a single calendar day.

**Configuration:**

- `dailyAmountLimit` — Maximum cumulative amount (e.g., $5,000)
- `dailyCountLimit` — Maximum number of transactions (e.g., 20)
- `dailySingleTransactionLimit` — Maximum amount for any single transaction (e.g., $2,000)

**Behavior:**

- Counter resets at midnight in the entity's configured time zone.
- Both successful and pending transactions count toward the limit.
- Cancelled or failed transactions release their reserved amount.

**Example:**

```
Customer daily limit: $5,000
Transaction 1: $1,500 → Remaining: $3,500
Transaction 2: $2,000 → Remaining: $1,500
Transaction 3: $2,000 → BLOCKED (LimitExceeded)
```

---

### Weekly Limits

Controls cumulative activity over a 7-day period.

**Configuration:**

- `weeklyAmountLimit` — Maximum cumulative amount
- `weeklyCountLimit` — Maximum number of transactions

**Behavior:**

- Counter resets at the start of the week (Monday 00:00 by default, configurable).
- Useful for subscription businesses, payroll disbursements, and B2B payment cycles.

---

### Monthly Limits

Controls cumulative activity over a calendar month.

**Configuration:**

- `monthlyAmountLimit` — Maximum cumulative amount
- `monthlyCountLimit` — Maximum number of transactions

**Behavior:**

- Counter resets on the 1st of each month.
- Commonly used for credit line management, corporate expense controls, and regulatory reporting thresholds.

---

### Transaction Count Limits

Restricts the number of transactions independent of monetary value.

**Use Cases:**

- Prevent card testing attacks (hundreds of small authorization attempts).
- Control API abuse or automated bot activity.
- Enforce subscription billing frequency (e.g., max 1 charge per month).

**Configuration:**

- `countLimit` — Maximum allowed transactions
- `timeWindow` — The period over which the count is measured
- `scope` — Per card, per user, per merchant, or per device

---

### Merchant Limits

Limits configured at the merchant level to control their processing exposure and risk.

**Limit Categories:**

| Category                 | Description                                           |
|--------------------------|-------------------------------------------------------|
| **Processing Volume**    | Maximum daily/weekly/monthly gross transaction volume |
| **Payout Volume**        | Maximum settlement amount per payout cycle            |
| **Refund Volume**        | Maximum refund amount or percentage of sales          |
| **Chargeback Threshold** | Maximum chargeback rate (%) before restrictions apply |
| **Reserve Requirement**  | Minimum rolling reserve balance held by the platform  |
| **MCC Restrictions**     | Blocked or restricted merchant category codes         |

**Behavior:**

- New merchants start with conservative limits; limits increase based on processing history and risk profile.
- Breaching a merchant limit may trigger automatic suspension, reserve increase, or manual review.

---

### Customer Limits

Limits configured at the customer level to prevent overspending and fraud.

**Limit Categories:**

| Category               | Description                                            |
|------------------------|--------------------------------------------------------|
| **Spending Limits**    | Daily, weekly, monthly spending caps                   |
| **Transaction Size**   | Maximum single transaction amount                      |
| **Merchant Diversity** | Maximum spend per merchant or minimum merchant count   |
| **Geographic Limits**  | Blocked or allowed countries for transactions          |
| **Method Limits**      | Restrictions on card types, wallets, or bank transfers |

**Behavior:**

- Customers may view their current usage and remaining limits.
- Soft limits can trigger notifications; hard limits block transactions.
- VIP or verified customers may request limit increases through KYC escalation.

---

### Country Limits

Enforces geographic boundaries on transaction flows for compliance and risk management.

**Limit Types:**

| Type                      | Description                                                                      |
|---------------------------|----------------------------------------------------------------------------------|
| **Blocked Countries**     | Complete prohibition on transactions involving sanctioned or high-risk countries |
| **Restricted Countries**  | Transactions allowed but with enhanced scrutiny (lower limits, mandatory 3DS)    |
| **Remittance Limits**     | Maximum cross-border transfer amounts per customer per year (regulatory)         |
| **Local Currency Limits** | Maximum transaction amount in local currency for specific jurisdictions          |

**Evaluated Countries:**

- Card-issuing country (from BIN)
- IP geolocation country
- Billing address country
- Merchant registration country
- Destination country (for payouts/remittances)

---

### Currency Limits

Controls transaction activity denominated in specific currencies.

**Limit Types:**

| Type                        | Description                                                  |
|-----------------------------|--------------------------------------------------------------|
| **Supported Currencies**    | Whitelist of currencies the merchant or platform accepts     |
| **Currency Exposure Limit** | Maximum open position in a non-settlement currency           |
| **FX Transaction Limit**    | Maximum amount for cross-currency transactions               |
| **Stablecoin Limits**       | Restrictions on cryptocurrency or digital asset transactions |

**Behavior:**

- Transactions in unsupported currencies are blocked at creation.
- FX limits protect against currency volatility and regulatory restrictions.

---

### Velocity Limits

Detects and prevents abnormal spikes in transaction activity that indicate fraud or system abuse.

**Velocity Patterns:**

| Pattern                 | Description                                                               | Example                         |
|-------------------------|---------------------------------------------------------------------------|---------------------------------|
| **Amount Velocity**     | Rapid increase in transaction amount vs. historical average               | 5x average transaction size     |
| **Frequency Velocity**  | Unusual number of transactions in a short window                          | 10 transactions in 5 minutes    |
| **Merchant Velocity**   | Same customer transacting with many different merchants                   | 5+ merchants in 1 hour          |
| **Card Velocity**       | Same card used across multiple accounts or devices                        | Card seen on 3+ accounts in 24h |
| **Geographic Velocity** | Impossible travel (transactions from distant locations too close in time) | NYC then London within 2 hours  |

**Behavior:**

- Velocity breaches increment the risk score and may trigger automatic decline.
- Baselines are computed from 30-day, 90-day, and lifetime historical averages.
- Adaptive baselines adjust for seasonal patterns (e.g., holiday shopping spikes).

---

### Credit Limits

Manages outstanding exposure and credit lines for deferred payment products.

**Credit Types:**

| Type                        | Description                                                     |
|-----------------------------|-----------------------------------------------------------------|
| **Transaction Credit Line** | Maximum outstanding authorized-but-not-captured amount          |
| **Rolling Credit Limit**    | Maximum cumulative open balance (e.g., BNPL, invoice financing) |
| **Merchant Advance Limit**  | Maximum capital advance or revenue-based financing exposure     |
| **Platform Exposure Limit** | Maximum aggregate uncaptured authorization across all merchants |

**Behavior:**

- Credit is reserved during authorization and consumed during capture.
- Partial captures release unused credit back to the available pool.
- Expired or cancelled authorizations automatically release reserved credit.
- Real-time available credit queries support split-payment and multi-capture scenarios.

---

### Limit Reservation

Reserves capacity against a limit during transaction processing to prevent race conditions.

**Reservation Flow:**

1. Transaction initiated → Limit Service evaluates all applicable limits.
2. If within bounds → Reserve the transaction amount and/or count against each relevant limit.
3. Reservation is time-bound (e.g., 15 minutes for card authorizations).
4. Publish `LimitReserved` event.

**Reservation Properties:**

- `reservationId` — Unique identifier for the reservation
- `limitType` — Which limit is reserved against
- `reservedAmount` — Amount held
- `reservedCount` — Count held (for count limits)
- `expiresAt` — Reservation expiry timestamp
- `transactionId` — Associated transaction

**Reservation Scenarios:**

| Scenario               | Behavior                                                   |
|------------------------|------------------------------------------------------------|
| Authorization succeeds | Reservation remains until capture or expiry                |
| Authorization fails    | Reservation is immediately released                        |
| Capture succeeds       | Reservation is converted to committed usage                |
| Partial capture        | Reservation reduced by captured amount; remainder released |
| Reservation expires    | Automatic release of reserved capacity                     |

---

### Limit Release

Frees reserved capacity when a transaction completes, fails, or times out.

**Release Triggers:**

| Trigger                   | Action                                                      |
|---------------------------|-------------------------------------------------------------|
| **Transaction Failed**    | Release full reserved amount and count                      |
| **Transaction Cancelled** | Release full reserved amount and count                      |
| **Authorization Expired** | Release full reserved amount and count                      |
| **Partial Capture**       | Release uncaptured portion                                  |
| **Refund Processed**      | Restore refunded amount to applicable limits (configurable) |
| **Manual Override**       | Admin-initiated release for exceptional cases               |

**Release Flow:**

1. Receive release trigger from Payment Service or scheduled job.
2. Identify all active reservations for the transaction.
3. Decrement usage counters and restore available capacity.
4. Publish `LimitReleased` event.
5. Archive reservation record for audit.

---

## Limit Enforcement Model

### Hard Limits vs. Soft Limits

| Aspect           | Hard Limit                          | Soft Limit                            |
|------------------|-------------------------------------|---------------------------------------|
| **Behavior**     | Transaction is blocked              | Transaction proceeds; alert generated |
| **Override**     | Requires elevated approval          | May be overridden by user or merchant |
| **Use Case**     | Regulatory caps, fraud prevention   | Spending awareness, budget alerts     |
| **Notification** | Decline reason to merchant/customer | Push/email notification to user       |

### Limit Hierarchy and Precedence

When multiple limits apply, the most restrictive limit wins:

```
1. Platform Global Hard Limits    (highest precedence)
2. Regulatory / Compliance Limits
3. Merchant-Specific Limits
4. Customer-Specific Limits
5. Platform Default Soft Limits   (lowest precedence)
```

### Limit Evaluation Order

```
Transaction Request
        │
        ▼
┌─────────────────┐
│ 1. Blacklist?   │ ──► BLOCK (skip limits)
└─────────────────┘
        │ No
        ▼
┌─────────────────┐
│ 2. Country      │ ──► BLOCK if restricted
│    Limits       │
└─────────────────┘
        │ Pass
        ▼
┌─────────────────┐
│ 3. Currency     │ ──► BLOCK if unsupported
│    Limits       │
└─────────────────┘
        │ Pass
        ▼
┌─────────────────┐
│ 4. Amount/Count │ ──► BLOCK if exceeded
│    Limits       │
└─────────────────┘
        │ Pass
        ▼
┌─────────────────┐
│ 5. Velocity     │ ──► CHALLENGE or BLOCK
│    Limits       │
└─────────────────┘
        │ Pass
        ▼
┌─────────────────┐
│ 6. Credit       │ ──► BLOCK if insufficient
│    Limits       │
└─────────────────┘
        │ Pass
        ▼
   RESERVE LIMITS
        │
        ▼
   PROCEED TO AUTH
```

---

## Owned Resources

The Limit Service is the authoritative owner of the following data:

| Resource                 | Description                                                                      |
|--------------------------|----------------------------------------------------------------------------------|
| **Limit Configurations** | Defined limit rules, thresholds, scopes, time windows, and enforcement policies. |
| **Usage Counters**       | Real-time and historical counters tracking consumption against each limit.       |

> **Note:** Transaction data is owned by the Payment Service. Merchant and customer profiles are owned by the Merchant
> Service and User Service, respectively. The Limit Service consumes entity data but does not own it.

---

## Domain Events

The Limit Service publishes the following events for downstream consumers:

| Event           | Trigger                                                                                     |
|-----------------|---------------------------------------------------------------------------------------------|
| `LimitExceeded` | A transaction breaches one or more configured limits and is blocked or flagged.             |
| `LimitReserved` | Capacity is successfully reserved against a limit during transaction processing.            |
| `LimitReleased` | Reserved capacity is freed due to transaction completion, failure, expiry, or cancellation. |

---

## Integration Notes

- **Payment Service**: Queries limits before authorization; requests reservation and release during the transaction
  lifecycle.
- **Fraud Service**: Consumes limit breach events as fraud signals; velocity limits may be synchronized with fraud
  velocity rules.
- **User Service**: Retrieves customer-specific limit configurations and displays usage dashboards.
- **Merchant Service**: Retrieves merchant-specific limits; receives alerts on limit breaches and suspension triggers.
- **Authorization Service**: Enforces limit decisions as part of the authorization policy (e.g., decline if limit
  exceeded).
- **Notification Service**: Receives limit events to send customer alerts, merchant warnings, and admin notifications.
- **Audit Service**: Subscribes to all limit events for compliance reporting and dispute evidence.
- **Compliance Service**: Receives regulatory limit breach alerts for mandatory reporting.
