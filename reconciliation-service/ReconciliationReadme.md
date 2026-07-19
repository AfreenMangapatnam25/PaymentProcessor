# Reconciliation Service

## Overview

The Reconciliation Service is the platform's financial integrity guardian. It ensures that internal financial records — maintained by the Ledger, Payment, and Settlement services — match external records from banks, acquirers, card networks, and payment gateways. By continuously comparing, matching, and validating transactions across systems, it detects discrepancies, prevents financial leakage, and provides the audit evidence required for regulatory compliance and financial reporting.

The service operates on a **match-detect-resolve** model: it ingests external statement files, matches them against internal records using configurable rules, flags mismatches for investigation, and tracks every exception through to resolution. Reconciliation is performed at multiple levels — transaction, batch, and summary — to catch errors ranging from individual missing transactions to systemic settlement drift.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Reconciliation Architecture](#reconciliation-architecture)
- [Core Functionalities](#core-functionalities)
  - [Bank Reconciliation](#bank-reconciliation)
  - [Acquirer Reconciliation](#acquirer-reconciliation)
  - [Card Network Reconciliation](#card-network-reconciliation)
  - [Gateway Reconciliation](#gateway-reconciliation)
  - [Ledger Reconciliation](#ledger-reconciliation)
  - [Settlement Reconciliation](#settlement-reconciliation)
  - [Mismatch Detection](#mismatch-detection)
  - [Auto Matching](#auto-matching)
  - [Manual Reconciliation](#manual-reconciliation)
  - [Exception Reporting](#exception-reporting)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern | Description |
|---------|-------------|
| **Financial Integrity** | Guarantee that every dollar recorded internally has a corresponding external confirmation, and vice versa. |
| **Discrepancy Detection** | Identify missing, duplicated, or misstated transactions before they compound into material errors. |
| **Fraud Detection** | Surface anomalies such as unauthorized settlements, phantom transactions, or tampered records. |
| **Regulatory Compliance** | Provide auditable evidence that the platform maintains accurate books and records per GAAP, IFRS, and local regulations. |
| **Operational Efficiency** | Automate the matching of high-volume, routine transactions while escalating only genuine exceptions for human review. |
| **Audit Readiness** | Generate immutable reconciliation reports and exception logs for internal audits, external auditors, and regulators. |

---

## Reconciliation Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    EXTERNAL SYSTEMS                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  Banks   │  │Acquirers │  │  Card    │  │Gateways  │  │
│  │          │  │          │  │Networks  │  │          │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │             │             │             │          │
│       ▼             ▼             ▼             ▼          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           STATEMENT INGESTION LAYER                  │  │
│  │  (SFTP, API, File Upload: MT940, CAMT, NACHA,      │  │
│  │   ISO 20022, CSV, XML, JSON)                         │  │
│  └─────────────────────┬────────────────────────────────┘  │
│                        │                                    │
│                        ▼                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           NORMALIZATION & TRANSFORMATION             │   │
│  │  (Parse, validate, map to canonical format,          │   │
│  │   enrich with metadata)                              │   │
│  └─────────────────────┬────────────────────────────────┘   │
│                        │                                     │
│                        ▼                                     │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              MATCHING ENGINE                         │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │    │
│  │  │ Auto Match  │  │ Fuzzy Match │  │  Manual     │ │    │
│  │  │ (Exact)     │  │ (Rules/ML)  │  │  Queue      │ │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │    │
│  └─────────────────────┬────────────────────────────────┘    │
│                        │                                      │
│                        ▼                                      │
│  ┌──────────────────────────────────────────────────────┐     │
│  │           MISMATCH DETECTION & CLASSIFICATION        │     │
│  │  (Categorize, severity score, route to resolution)     │     │
│  └─────────────────────┬────────────────────────────────┘     │
│                        │                                      │
│                        ▼                                      │
│  ┌──────────────────────────────────────────────────────┐     │
│  │           EXCEPTION MANAGEMENT & RESOLUTION          │     │
│  │  (Auto-correction, manual review, adjustment,       │     │
│  │   escalation, audit trail)                           │     │
│  └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Functionalities

### Bank Reconciliation

Matches internal cash movement records against bank statement entries to ensure all deposits, withdrawals, fees, and interest are accounted for.

**Reconciliation Scope:**

| Account Type | Reconciled Against | Frequency |
|--------------|-------------------|-----------|
| **Settlement Account** | Bank statement (MT940 / CAMT.053) | Daily |
| **Operating Account** | Bank statement | Daily |
| **Reserve Account** | Bank statement | Daily |
| **Escrow / Trust Account** | Bank statement | Daily |
| **Sweep Account** | Bank statement | Daily |

**Matching Criteria:**
- Amount (exact match, tolerance for FX rounding)
- Date (same day ± 1 business day for cross-border)
- Reference number (transaction ID, settlement batch ID)
- Counterparty (merchant bank account, acquirer identifier)

**Common Discrepancies:**

| Discrepancy | Cause | Resolution |
|-------------|-------|------------|
| **Missing internal entry** | Bank processed a transfer not yet recorded internally | Post missing entry to ledger |
| **Missing bank entry** | Internal transfer not yet reflected in bank statement | Wait for next statement cycle |
| **Amount mismatch** | FX rate difference, bank fees not recorded | Adjust for FX variance or fee accrual |
| **Duplicate entry** | Same transaction recorded twice internally | Reverse duplicate |
| **Timing difference** | End-of-day cutoff mismatch | Mark as "in transit", reconcile next day |

---

### Acquirer Reconciliation

Reconciles transaction records against acquirer settlement files to ensure the platform receives the correct funds for processed transactions.

**Reconciliation Scope:**

| Data Source | Format | Frequency |
|-------------|--------|-----------|
| **Acquirer Settlement File** | CSV, XML, ISO 20022 | Daily |
| **Acquirer Fee Report** | CSV, PDF | Monthly |
| **Chargeback File** | CSV, XML | Daily |
| **Refund Confirmation** | API callback, file | Real-time / Daily |

**Matching Criteria:**
- Acquirer reference number (ARN — Acquirer Reference Number)
- Transaction amount (gross, net, fee breakdown)
- Settlement date
- Merchant identifier
- Card BIN and last four digits

**Fee Reconciliation:**
- Verify interchange fees match card network published rates
- Verify scheme fees (assessment, network access)
- Verify acquirer markup matches contractual rate
- Flag any unbilled or overbilled fees

**Common Discrepancies:**

| Discrepancy | Cause | Resolution |
|-------------|-------|------------|
| **Missing settlement** | Transaction authorized but not settled by acquirer | Investigate with acquirer; may be pending or declined |
| **Short settlement** | Acquirer deducted additional fees or chargebacks | Reconcile fee breakdown; post adjustment |
| **Excess settlement** | Acquirer overpaid or included prior period adjustments | Verify and post credit adjustment |
| **ARN mismatch** | Internal ARN mapping error | Correct mapping; re-reconcile |
| **Currency discrepancy** | FX rate applied by acquirer differs from internal rate | Post FX gain/loss entry |

---

### Card Network Reconciliation

Validates that interchange and scheme fees reported by card networks align with internal calculations and acquirer charges.

**Reconciliation Scope:**

| Network | Report Type | Frequency |
|---------|-------------|-----------|
| **Visa** | VSS (Visa Settlement Service), VMID | Daily / Monthly |
| **Mastercard** | MDS (Mastercard Data Suite), GCMS | Daily / Monthly |
| **Amex** | Amex OptBlue, Direct | Daily / Monthly |
| **Discover** | DFS (Discover Financial Services) | Daily / Monthly |
| **UnionPay** | CUPS settlement files | Daily / Monthly |

**Matching Criteria:**
- Transaction count and volume by card product (debit, credit, prepaid, corporate)
- Interchange fee category and rate
- Scheme fee (assessment, cross-border, NABU, etc.)
- Chargeback count and volume
- Refund count and volume

**Common Discrepancies:**

| Discrepancy | Cause | Resolution |
|-------------|-------|------------|
| **Interchange rate mismatch** | Transaction downgraded to higher interchange category | Verify MCC, data quality, 3DS usage |
| **Volume discrepancy** | Network reports different transaction count | Investigate pending, reversed, or duplicate transactions |
| **Scheme fee variance** | New fee introduced by network | Update fee accrual model |
| **Chargeback not reflected** | Chargeback processed by network but not yet received from acquirer | Accrue estimated chargeback liability |

---

### Gateway Reconciliation

Matches internal payment gateway records against provider reports to ensure all authorized, captured, and refunded transactions are correctly recorded.

**Reconciliation Scope:**

| Gateway | Report Type | Frequency |
|---------|-------------|-----------|
| **Stripe** | Payout reconciliation report, balance transactions | Daily |
| **Adyen** | Settlement details report, payment accounting report | Daily |
| **Braintree** | Transaction search, settlement batch report | Daily |
| **PayPal** | Transaction details, settlement report | Daily |
| **Custom Acquirer** | Settlement file, transaction log | Daily |

**Matching Criteria:**
- Gateway transaction ID
- Internal payment ID
- Amount (authorized, captured, refunded, fees)
- Status (authorized, captured, failed, refunded, chargeback)
- Timestamp

**Common Discrepancies:**

| Discrepancy | Cause | Resolution |
|-------------|-------|------------|
| **Status mismatch** | Gateway shows "captured" but internal shows "authorized" | Verify capture callback was processed; reconcile state |
| **Fee discrepancy** | Gateway fee differs from internal fee configuration | Update fee model or investigate gateway rate change |
| **Missing refund** | Refund processed internally but not reflected by gateway | Verify refund API response; retry if needed |
| **Pending transaction** | Gateway shows "pending" but internal shows "failed" | Reconcile based on final gateway status |

---

### Ledger Reconciliation

Ensures internal ledger balances are internally consistent and tie back to sub-ledger balances from domain services.

**Reconciliation Scope:**

| Check | Description | Frequency |
|-------|-------------|-----------|
| **Trial Balance Integrity** | Σ(Debits) = Σ(Credits) across all accounts | Daily |
| **Sub-Ledger to General Ledger** | Payment sub-ledger → Revenue accounts; Settlement sub-ledger → Cash accounts | Daily |
| **Inter-Company Reconciliation** | Balances between related entities or subsidiaries | Monthly |
| **Control Account Reconciliation** | Accounts Receivable control = Σ(individual merchant AR balances) | Daily |
| **Reserve Reconciliation** | Reserve asset = Σ(individual merchant reserve balances) | Daily |

**Common Discrepancies:**

| Discrepancy | Cause | Resolution |
|-------------|-------|------------|
| **Trial balance out of balance** | Posting engine error, race condition, or partial failure | Identify unbalanced journal entry; reverse and re-post |
| **Sub-ledger drift** | Domain service posted to ledger but failed to update its own state | Reconcile and correct sub-ledger state |
| **Duplicate posting** | Same event processed twice by ledger | Reverse duplicate entry |
| **Missing posting** | Event lost in transit to ledger | Replay event; post missing entry |

---

### Settlement Reconciliation

Verifies that settlements initiated by the platform were actually received by merchants and that the amounts match.

**Reconciliation Scope:**

| Check | Description | Frequency |
|-------|-------------|-----------|
| **Settlement vs. Bank Debit** | Internal settlement batch = Bank account debit | Daily |
| **Settlement vs. Merchant Confirmation** | Merchant reports receipt of funds | Daily / On-demand |
| **Reserve Release vs. Bank Transfer** | Reserve release batch = Bank account debit | Daily |
| **Returned Settlements** | Bank returns (R01, R02, R03 for ACH; AC04 for SEPA) | Real-time |

**Common Discrepancies:**

| Discrepancy | Cause | Resolution |
|-------------|-------|------------|
| **Settlement not received** | Bank delay, incorrect account details, or rail failure | Verify with bank; retry or update account details |
| **Partial receipt** | Bank deducted fees not anticipated | Adjust settlement fee accrual |
| **Returned settlement** | Invalid account, account closed, or name mismatch | Notify merchant; update account; retry or offset |
| **Duplicate settlement** | Same batch sent twice due to retry logic | Reverse duplicate; recover funds |

---

### Mismatch Detection

Identifies and classifies discrepancies between internal and external records.

**Mismatch Categories:**

| Category | Severity | Description |
|----------|----------|-------------|
| **Missing Internal** | High | External record exists but no matching internal record |
| **Missing External** | Medium | Internal record exists but no matching external record |
| **Amount Mismatch** | High | Same reference but different amounts |
| **Date Mismatch** | Low | Same transaction but different dates (timing difference) |
| **Status Mismatch** | Medium | Same transaction but different statuses |
| **Duplicate** | Medium | Same transaction recorded multiple times on one side |
| **FX Variance** | Low | Amount difference due to foreign exchange rounding or rate changes |
| **Fee Variance** | Medium | Fee amount differs from expected |
| **Reference Mismatch** | High | Different reference numbers for the same transaction |

**Detection Rules:**
- Exact match on amount, date, and reference → Auto-reconciled
- Amount within tolerance (e.g., ±$0.01 for FX rounding) → Auto-reconciled with variance note
- Same reference, different amount → Mismatch flagged
- Same amount, different reference → Fuzzy match attempted; if no match, flag as missing
- Multiple internal records match one external record → Duplicate detection

**Severity Scoring:**
```
Severity = (Amount × Materiality Weight) + (Age × Aging Weight) + (Category Base Score)
```

| Score Range | Action |
|-------------|--------|
| 0–30 | Low priority; batch into weekly review |
| 31–60 | Medium priority; daily review queue |
| 61–90 | High priority; immediate alert to operations |
| 91–100 | Critical; block related activity; escalate to finance director |

---

### Auto Matching

Automatically reconciles routine transactions using configurable matching rules and machine learning.

**Exact Matching Rules:**

| Rule | Weight | Description |
|------|--------|-------------|
| **Amount + Reference** | 100% | Exact amount and exact reference number match |
| **Amount + Date ± 1d** | 90% | Exact amount and date within 1 business day |
| **Amount + Merchant + Date** | 85% | Exact amount, same merchant, same date |
| **Reference Only** | 70% | Exact reference but amount differs (investigate) |
| **Amount + Last 4 Digits** | 60% | Exact amount and card last 4 digits match |

**Fuzzy Matching:**
- Levenshtein distance on reference numbers (e.g., `TXN-12345` vs. `TXN-12345-A`)
- Amount tolerance for FX transactions (e.g., ±0.5%)
- Date tolerance for cross-border transfers (e.g., ±3 business days)
- ML model trained on historical manual matches to suggest matches for ambiguous cases

**Auto-Correction:**
- Post missing entries for "missing internal" cases with high confidence
- Reverse duplicates automatically
- Post FX variance adjustments within tolerance
- Flag for manual review if confidence < 95%

---

### Manual Reconciliation

A human-in-the-loop workflow for exceptions that cannot be auto-matched or auto-corrected.

**Manual Queue Routing:**

| Condition | Queue | SLA |
|-----------|-------|-----|
| Amount > $10,000 | Senior Analyst | 4 hours |
| Age > 5 business days | Escalated | 24 hours |
| Suspected fraud | Fraud Investigation | 2 hours |
| Regulatory report discrepancy | Compliance | 4 hours |
| Standard mismatch | Operations Analyst | 48 hours |

**Analyst Actions:**
- **Match** — Link internal and external records with a reconciliation note
- **Create Entry** — Post a missing journal entry to the ledger
- **Reverse** — Reverse an incorrect internal entry
- **Adjust** — Post an adjustment entry for amount or fee variance
- **Escalate** — Send to finance director, compliance, or fraud team
- **Defer** — Mark as timing difference; auto-reconcile on next cycle

**Audit Trail:**
- Every manual action is logged with analyst ID, timestamp, reason code, and before/after state
- Requires dual approval for adjustments > $5,000
- Immutable record of all reconciliation decisions

---

### Exception Reporting

Generates comprehensive reports on reconciliation health, mismatches, and resolution metrics.

**Report Types:**

| Report | Audience | Content |
|--------|----------|---------|
| **Daily Reconciliation Summary** | Finance / Operations | Match rate, mismatch count by category, aging analysis |
| **Mismatch Detail Report** | Analysts | Every open mismatch with context, severity, and recommended action |
| **Aging Analysis** | Finance Manager | Mismatches grouped by age (0–5d, 6–15d, 16–30d, >30d) |
| **Auto-Match Performance** | Engineering / Product | Auto-match rate, false positive rate, model accuracy |
| **Exception Resolution Metrics** | Operations Manager | Average resolution time, resolution rate, analyst workload |
| **Regulatory Reconciliation Certificate** | External Auditors | Signed attestation that books are reconciled and accurate |

**Key Metrics:**

| Metric | Target | Description |
|--------|--------|-------------|
| **Auto-Match Rate** | > 98% | Percentage of transactions matched without human intervention |
| **Same-Day Reconciliation** | > 95% | Percentage of daily volume reconciled within 24 hours |
| **Open Mismatch Count** | < 0.1% of volume | Outstanding mismatches as a percentage of total transactions |
| **Average Resolution Time** | < 48 hours | Mean time from mismatch detection to resolution |
| **Material Misstatement Rate** | 0% | Number of material errors (>$10,000) undetected for > 5 days |

---

## Owned Resources

The Reconciliation Service is the authoritative owner of the following data:

| Resource | Description |
|----------|-------------|
| **Reconciliation Jobs** | Scheduled and on-demand reconciliation runs, including configuration, status, and execution logs. |
| **Reconciliation Results** | Outcome of each reconciliation job, including matched records, mismatches, and variance summaries. |
| **Exceptions** | Discrepancies detected during reconciliation, including classification, severity, status, and resolution trail. |

> **Note:** Transaction data is owned by the Payment Service. Ledger entries are owned by the Ledger Service. Settlement records are owned by the Settlement Service. External statements are owned by banks, acquirers, and card networks. The Reconciliation Service consumes and compares data from all these sources but does not own the underlying records.

---

## Domain Events

The Reconciliation Service publishes the following events for downstream consumers:

| Event | Trigger |
|-------|---------|
| `ReconciliationCompleted` | A reconciliation job finishes execution, producing a result set of matched and unmatched records. |
| `MismatchDetected` | A discrepancy is identified between internal and external records that exceeds auto-match tolerance or fails validation rules. |

---

## Integration Notes

- **Ledger Service**: Consumes journal entries and account balances for internal record comparison; receives correction entries for auto-resolved mismatches.
- **Payment Service**: Consumes transaction records (authorized, captured, refunded, chargeback) to match against external acquirer and gateway reports.
- **Settlement Service**: Consumes settlement batch and record data to reconcile against bank statements and merchant confirmations.
- **Merchant Service**: Retrieves merchant configurations and settlement account details for matching criteria enrichment.
- **Banking Partners**: Receives statement files (MT940, CAMT.053, BAI2) via SFTP or API for cash account reconciliation.
- **Acquirers**: Receives settlement and fee reports via SFTP, API, or file exchange for transaction-level reconciliation.
- **Card Networks**: Receives interchange and scheme fee reports for fee validation and volume reconciliation.
- **Payment Gateways**: Receives transaction and settlement reports via API for gateway-specific reconciliation.
- **Notification Service**: Sends alerts to operations teams on high-severity mismatches, aging exceptions, and reconciliation completion.
- **Audit Service**: Subscribes to all reconciliation events and exception resolutions for compliance logging and external audit evidence.
- **Fraud Service**: Receives anomaly patterns from mismatch detection (e.g., sudden spikes in missing settlements) as potential fraud signals.
