# Settlement Service

## Overview

The Settlement Service is the platform's fund disbursement engine. It is responsible for calculating, scheduling, initiating, and reconciling the transfer of funds owed to merchants for successfully processed transactions. Every captured payment flows through a settlement pipeline that ensures accurate, timely, and auditable payouts while managing fees, reserves, adjustments, and exception handling.

The service operates on a **batch-and-schedule** model: individual transactions are aggregated into settlement batches based on merchant configuration, and payouts are executed according to predefined schedules (daily, weekly, monthly) or on-demand triggers. It integrates directly with banking and payment rails to move money and maintains a complete audit trail of every settlement attempt.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Settlement Lifecycle](#settlement-lifecycle)
- [Core Functionalities](#core-functionalities)
  - [Settlement Calculation](#settlement-calculation)
  - [Settlement Batching](#settlement-batching)
  - [Settlement Scheduling](#settlement-scheduling)
  - [Settlement Initiation](#settlement-initiation)
  - [Settlement Retries](#settlement-retries)
  - [Settlement Reports](#settlement-reports)
  - [Settlement Fees](#settlement-fees)
  - [Settlement Adjustments](#settlement-adjustments)
  - [Settlement Reversal](#settlement-reversal)
  - [Bank Transfer Integration](#bank-transfer-integration)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern | Description |
|---------|-------------|
| **Accurate Disbursement** | Compute the exact amount owed to each merchant after deducting fees, reserves, and adjustments. |
| **Timely Payouts** | Execute settlements according to merchant-configured schedules and regulatory cut-off times. |
| **Batch Aggregation** | Group individual transactions into efficient settlement batches to minimize transfer costs and operational overhead. |
| **Exception Handling** | Manage failed transfers, returned payments, and reconciliation discrepancies through automated retries and manual intervention workflows. |
| **Financial Reconciliation** | Maintain an auditable trail that matches internal settlement records against external bank statements and ledger entries. |
| **Reserve Management** | Hold and release rolling reserves as configured by merchant risk profiles and platform policies. |

---

## Settlement Lifecycle

```
CAPTURED TRANSACTIONS
        │
        ▼
┌─────────────────┐
│ 1. Aggregation  │ ──► Group by merchant, currency, schedule
└─────────────────┘
        │
        ▼
┌─────────────────┐
│ 2. Calculation  │ ──► Gross amount − fees − reserves − adjustments = net settlement
└─────────────────┘
        │
        ▼
┌─────────────────┐
│ 3. Batching     │ ──► Create settlement batch with multiple merchant payouts
└─────────────────┘
        │
        ▼
┌─────────────────┐
│ 4. Scheduling   │ ──► Apply merchant payout schedule and banking cut-off times
└─────────────────┘
        │
        ▼
┌─────────────────┐
│ 5. Initiation   │ ──► Send transfer instructions to bank/payment rail
│    (PENDING)    │
└─────────────────┘
        │
        ├──► SUCCESS ──► COMPLETED
        │
        └──► FAILURE ──► RETRY ──► (max attempts reached) ──► FAILED
```

**Settlement States:**

| State | Description |
|-------|-------------|
| **PENDING** | Settlement calculated and scheduled, awaiting execution window. |
| **INITIATED** | Transfer instruction sent to the bank or payment rail. |
| **PROCESSING** | Bank or rail has acknowledged and is processing the transfer. |
| **COMPLETED** | Funds successfully deposited into the merchant's settlement account. |
| **FAILED** | Transfer failed after all retry attempts; requires manual intervention. |
| **RETURNED** | Funds were returned by the receiving bank (e.g., invalid account, account closed). |
| **RECONCILED** | Internal records match external bank statement. |
| **REVERSED** | Settlement was reversed due to error, fraud, or adjustment. |

---

## Core Functionalities

### Settlement Calculation

Computes the net amount to be disbursed to each merchant for a given settlement period.

**Calculation Formula:**
```
Net Settlement = Gross Transaction Amount
                 − Platform Fees (transaction % + fixed)
                 − Interchange Fees (pass-through or absorbed)
                 − Chargeback Deductions
                 − Refund Deductions
                 − Rolling Reserve (held %, released later)
                 − Settlement Fees (payout transfer cost)
                 + Adjustments (credits, bonuses, corrections)
                 − Previous Period Corrections
```

**Example:**
```
Gross Transaction Volume:        $10,000.00
Platform Fee (2.9% + $0.30):      −$290.00 − $30.00 = −$320.00
Interchange Fee (pass-through):   −$180.00
Chargeback Deductions:            −$500.00
Refund Deductions:                −$200.00
Rolling Reserve (10% hold):       −$1,000.00
Settlement Fee (ACH):             −$1.50
Adjustment (promotional credit):  +$50.00
─────────────────────────────────────────────
Net Settlement Amount:            $7,848.50
Reserve Held (released in 90d):   $1,000.00
```

**Components:**

| Component | Source | Deducted From |
|-----------|--------|---------------|
| **Gross Amount** | Sum of captured transactions in the period | Base |
| **Platform Fee** | Merchant pricing plan (Merchant Service) | Gross |
| **Interchange Fee** | Card network rates (acquirer) | Gross |
| **Chargebacks** | Disputed transactions (Chargeback Service) | Gross |
| **Refunds** | Reversed transactions (Refund Service) | Gross |
| **Rolling Reserve** | Merchant risk profile (Merchant Service) | Net |
| **Settlement Fee** | Payout method cost (bank rail) | Net |
| **Adjustments** | Manual credits/debits (admin action) | Net |

---

### Settlement Batching

Aggregates individual merchant settlements into batches for efficient processing.

**Batching Strategies:**

| Strategy | Description | Use Case |
|----------|-------------|----------|
| **Merchant Batch** | One batch per merchant per currency per day | Standard for most merchants |
| **Currency Batch** | Group multiple merchants by settlement currency | Reduces FX conversion overhead |
| **Rail Batch** | Group by payout method (ACH, SEPA, Wire, FPS) | Optimizes per-rail transaction costs |
| **Consolidated Batch** | Single batch for all merchants | Enterprise or high-volume platforms |

**Batch Properties:**
- `batchId` — Unique identifier
- `batchDate` — The settlement date for the batch
- `currency` — Settlement currency
- `merchantCount` — Number of merchants in the batch
- `totalGrossAmount` — Sum of all gross amounts
- `totalNetAmount` — Sum of all net amounts after deductions
- `totalFeeAmount` — Sum of all platform and settlement fees
- `status` — PENDING, INITIATED, PROCESSING, COMPLETED, FAILED
- `settlementRecords` — List of individual merchant settlements in the batch

**Batch Lifecycle:**
1. **Creation** — Automatically created at the end of the merchant's settlement period.
2. **Validation** — Verify all calculations, check merchant account status, validate bank details.
3. **Approval** — Auto-approved for standard merchants; may require manual approval for high-value or high-risk merchants.
4. **Initiation** — Send batch transfer instructions to the bank or payment rail.
5. **Tracking** — Monitor batch status until all individual settlements reach a terminal state.

---

### Settlement Scheduling

Determines when settlements are executed based on merchant preferences and operational constraints.

**Schedule Types:**

| Type | Frequency | Typical Cut-Off |
|------|-----------|-----------------|
| **Daily** | Every business day | 18:00 UTC for next-day settlement |
| **Weekly** | Every Monday (or configured day) | End of previous week |
| **Bi-Weekly** | Every two weeks | Configurable |
| **Monthly** | 1st of month (or configured date) | End of previous month |
| **On-Demand** | Triggered by merchant request | Subject to minimum threshold and approval |
| **Instant** | Immediate (premium feature) | Real-time rail (e.g., RTP, FPS) |

**Scheduling Rules:**
- Settlements are not processed on weekends or banking holidays (configurable per country).
- Minimum settlement threshold: merchants must accumulate at least $25 (configurable) before a payout is triggered.
- Maximum settlement delay: funds cannot be held beyond regulatory maximums (e.g., T+2 for card payments).
- Reserve releases follow their own schedule (e.g., 90 days after capture) independent of the main settlement schedule.

**Cut-Off Times:**
- ACH: 18:00 UTC for next-business-day settlement
- SEPA: 15:30 CET for next-business-day settlement
- Wire: 14:00 local time for same-day (if before cut-off)
- FPS (UK): 24/7, near-instant
- RTP (US): 24/7, near-instant

---

### Settlement Initiation

Sends the actual fund transfer instructions to the banking or payment rail.

**Initiation Flow:**
1. **Pre-Validation** — Verify merchant settlement account details, merchant status (ACTIVE), and available balance.
2. **Ledger Posting** — Post settlement liability reduction and cash outflow entries to the Ledger Service.
3. **Rail Selection** — Choose the optimal payment rail based on amount, currency, urgency, and cost.
4. **Instruction Generation** — Create formatted transfer instruction (ISO 20022, NACHA, BACS, etc.).
5. **Transmission** — Send to bank API, SFTP, or SWIFT network.
6. **Acknowledgment** — Receive tracking reference from the bank.
7. **State Update** — Transition settlement record to `INITIATED`.
8. **Event Publish** — Emit `SettlementInitiated` event.

**Rail Selection Logic:**

| Amount | Urgency | Currency | Preferred Rail |
|--------|---------|----------|----------------|
| <$10,000 | Standard | USD | ACH |
| <$10,000 | Urgent | USD | RTP / Wire |
| <$100,000 | Standard | EUR | SEPA |
| Any | Instant | GBP | FPS |
| >$100,000 | Any | Any | Wire / SWIFT |

---

### Settlement Retries

Handles failed settlement attempts through an automated retry mechanism.

**Failure Categories:**

| Category | Examples | Retry Behavior |
|----------|----------|----------------|
| **Transient** | Bank timeout, network error, rate limit | Retry 3× with exponential backoff (1h, 4h, 12h) |
| **Recoverable** | Insufficient platform funds, daily limit exceeded | Retry next business day |
| **Merchant-Side** | Invalid account number, account closed, name mismatch | No retry; alert merchant to update details |
| **Rail-Side** | Bank holiday, rail maintenance | Retry on next business day |
| **Fatal** | Sanctions hit, regulatory block | No retry; escalate to compliance |

**Retry Configuration:**
- Maximum retry attempts: 5 (configurable per rail)
- Exponential backoff: 1h, 4h, 12h, 24h, 48h
- Manual intervention trigger: after all retries exhausted
- Alerting: notify operations team on 2nd failure, escalate on final failure

---

### Settlement Reports

Generates comprehensive reports for merchants, finance teams, and auditors.

**Report Types:**

| Report | Audience | Content |
|--------|----------|---------|
| **Merchant Settlement Statement** | Merchant | Transaction-level breakdown, fees, reserves, net payout |
| **Daily Settlement Summary** | Finance | Total settlements by currency, rail, and status |
| **Pending Settlements Report** | Operations | All settlements awaiting execution or in retry |
| **Reconciliation Report** | Finance / Audit | Internal records vs. external bank statements |
| **Reserve Release Schedule** | Risk / Finance | Upcoming reserve releases by merchant and date |
| **Exception Report** | Operations | Failed, returned, and reversed settlements |

**Report Formats:**
- JSON (API)
- CSV (download)
- PDF (merchant-facing statement)
- MT940 / CAMT.053 (bank reconciliation)

---

### Settlement Fees

Tracks and deducts the cost of moving money through various payment rails.

**Fee Structure:**

| Rail | Direction | Fee Model | Typical Cost |
|------|-----------|-----------|--------------|
| **ACH (US)** | Outbound | Flat per transfer | $0.25–$1.50 |
| **RTP (US)** | Outbound | Flat per transfer | $0.25–$0.50 |
| **Wire (US)** | Outbound | Flat per transfer | $15–$30 |
| **SEPA (EU)** | Outbound | Flat per transfer | €0.20–€1.00 |
| **FPS (UK)** | Outbound | Flat per transfer | £0.10–£0.50 |
| **SWIFT** | Outbound | Flat + correspondent fees | $20–$50 |

**Fee Handling:**
- Fees may be absorbed by the platform (deducted from platform revenue) or passed through to the merchant (deducted from net settlement).
- Fee configuration is set per merchant in the Merchant Service and applied during settlement calculation.
- Fee changes require notice period and merchant acknowledgment.

---

### Settlement Adjustments

Allows manual corrections to settlement amounts for exceptional cases.

**Adjustment Types:**

| Type | Description | Example |
|------|-------------|---------|
| **Credit Adjustment** | Add funds to merchant payout | Promotional bonus, fee waiver, goodwill gesture |
| **Debit Adjustment** | Deduct funds from merchant payout | Recovered overpayment, penalty, correction of prior error |
| **Fee Correction** | Adjust incorrectly applied fees | Refund of duplicate fee charge |
| **Reserve Adjustment** | Manually release or hold additional reserve | Risk team decision based on investigation |
| **Currency Adjustment** | Correct for FX rate discrepancies | Settlement in different currency than expected |

**Approval Workflow:**
1. Adjustment requested by operations, risk, or finance team.
2. Reason code and supporting documentation required.
3. Supervisor approval for adjustments > $1,000.
4. Finance director approval for adjustments > $10,000.
5. Posted to ledger with clear audit trail linking to original settlement.

---

### Settlement Reversal

Recovers funds from a merchant when a settlement was issued in error or subsequent events invalidate the payout.

**Reversal Scenarios:**

| Scenario | Trigger |
|----------|---------|
| **Erroneous Payout** | Settlement calculated with wrong amount; excess funds must be recovered |
| **Subsequent Chargeback** | Chargeback received after settlement; recover funds from merchant |
| **Fraud Discovery** | Confirmed fraud on settled transactions; recover funds |
| **Regulatory Order** | Regulatory authority mandates fund recovery |
| **Merchant Termination** | Final settlement reversal after account closure |

**Reversal Mechanisms:**

| Mechanism | Description |
|-----------|-------------|
| **Direct Debit** | Initiate ACH debit or SEPA direct debit against merchant account |
| **Offset Against Future Settlement** | Deduct from next scheduled payout |
| **Wire Recall** | Request recall of a wire transfer (not guaranteed) |
| **Legal Recovery** | Escalate to collections or legal action for large amounts |

**Reversal Rules:**
- Reversal must be approved by finance director for amounts > $5,000.
- Merchant must be notified before reversal execution (except fraud cases under investigation).
- Reversal creates a compensating journal entry in the Ledger Service.
- Reversed settlement state transitions to `REVERSED`.

---

### Bank Transfer Integration

Integrates with banking and payment rails to execute fund movements.

**Supported Rails:**

| Rail | Region | Speed | Use Case |
|------|--------|-------|----------|
| **ACH** | US | 1–2 business days | Standard USD payouts |
| **RTP (Real-Time Payments)** | US | Seconds | Instant USD payouts |
| **Wire (Fedwire)** | US | Same day | Large USD transfers |
| **SEPA Credit Transfer** | EU | 1 business day | Standard EUR payouts |
| **SEPA Instant** | EU | Seconds | Instant EUR payouts |
| **FPS (Faster Payments)** | UK | Seconds | Instant GBP payouts |
| **BACS** | UK | 3 business days | Standard GBP payouts |
| **CHAPS** | UK | Same day | Large GBP transfers |
| **SWIFT** | Global | 1–5 business days | Cross-currency transfers |

**Integration Patterns:**

| Pattern | Description |
|---------|-------------|
| **API-Based** | REST/SOAP APIs provided by modern banks and fintechs (e.g., Stripe Treasury, Banking Circle). |
| **File-Based** | NACHA files for ACH, ISO 20022 XML for SEPA, BACS files via BACSTEL-IP. |
| **SWIFT Network** | MT103 messages for international wires. |
| **Real-Time APIs** | RTP network APIs, FPS Open Banking APIs. |

**Error Handling:**
- Parse bank return/reject codes and map to retry categories.
- Handle bank-specific error formats and reconciliation file formats.
- Maintain bank connection health checks and failover to backup rails.

---

## Owned Resources

The Settlement Service is the authoritative owner of the following data:

| Resource | Description |
|----------|-------------|
| **Settlement Batch** | Aggregated collection of merchant payouts processed together, including batch metadata and status. |
| **Settlement Records** | Individual merchant settlement line items with calculated amounts, deductions, fees, and state transitions. |

> **Note:** Transaction data is owned by the Payment Service. Merchant configuration and bank account details are owned by the Merchant Service. Ledger entries are owned by the Ledger Service. The Settlement Service orchestrates the payout process but does not own the underlying transaction or account data.

---

## Domain Events

The Settlement Service publishes the following events for downstream consumers:

| Event | Trigger |
|-------|---------|
| `SettlementInitiated` | A settlement batch or individual settlement record is submitted to the bank or payment rail for execution. |
| `SettlementCompleted` | Funds are confirmed as successfully deposited into the merchant's settlement account. |
| `SettlementFailed` | A settlement fails after all retry attempts or due to a non-recoverable error (invalid account, sanctions block, etc.). |

---

## Integration Notes

- **Payment Service**: Consumes `PaymentCaptured` events to include transactions in the next settlement cycle.
- **Merchant Service**: Retrieves merchant settlement schedules, pricing plans, reserve percentages, and bank account details.
- **Ledger Service**: Posts settlement liability reduction, cash outflow, fee expense, and reserve entries for every settlement.
- **Refund Service**: Consumes refund events to deduct refunded amounts from the merchant's next settlement.
- **Chargeback Service**: Consumes chargeback events to deduct disputed amounts and fees from settlements.
- **Fraud Service**: Receives fraud confirmations that may trigger settlement holds or reversals.
- **Limit Service**: Enforces platform-level settlement volume limits and per-merchant payout caps.
- **Notification Service**: Sends settlement status updates, merchant statements, and failure alerts to merchants.
- **Audit Service**: Subscribes to all settlement events for regulatory compliance, reconciliation, and dispute evidence.
- **Banking / Payment Rails**: Direct integration for executing fund transfers via ACH, SEPA, FPS, RTP, Wire, and SWIFT.
