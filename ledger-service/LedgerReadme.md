# Ledger Service

## Overview

The Ledger Service is the platform's financial accounting backbone. It maintains an immutable, append-only record of all
monetary movements across the system using double-entry bookkeeping principles. Every transaction, fee, settlement,
refund, and adjustment is recorded as a balanced set of debits and credits, ensuring that the accounting equation —
Assets = Liabilities + Equity — always holds true.

This service is the single source of truth for financial reconciliation, regulatory reporting, audit trails, and trial
balance generation. It is designed to be tamper-evident, horizontally scalable, and fully auditable.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Accounting Principles](#accounting-principles)
- [Core Functionalities](#core-functionalities)
    - [Double-Entry Accounting](#double-entry-accounting)
    - [Debit Entries](#debit-entries)
    - [Credit Entries](#credit-entries)
    - [Account Balances](#account-balances)
    - [Ledger Accounts](#ledger-accounts)
    - [Journal Entries](#journal-entries)
    - [Balance Snapshots](#balance-snapshots)
    - [Reversals](#reversals)
    - [Financial Audit](#financial-audit)
    - [Posting Engine](#posting-engine)
    - [Accounting Periods](#accounting-periods)
    - [Trial Balance](#trial-balance)
- [Chart of Accounts](#chart-of-accounts)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern                    | Description                                                                                                                   |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Immutability**           | Once a journal entry is posted, it can never be modified — only reversed with a compensating entry.                           |
| **Double-Entry Integrity** | Every financial event produces balanced debits and credits; the sum of all debits always equals the sum of all credits.       |
| **Real-Time Balances**     | Compute and expose up-to-date account balances with strong consistency guarantees.                                            |
| **Auditability**           | Provide a complete, time-ordered trail of every financial movement for internal and external auditors.                        |
| **Regulatory Compliance**  | Support GAAP, IFRS, and local regulatory requirements for financial record-keeping and reporting.                             |
| **Reconciliation**         | Enable automated and manual reconciliation between internal ledgers and external statements (bank, card network, settlement). |

---

## Accounting Principles

The Ledger Service adheres to fundamental accounting conventions:

### Double-Entry Bookkeeping

Every financial transaction affects at least two accounts. For each journal entry:

- **Total Debits = Total Credits**
- **Assets + Expenses = Liabilities + Equity + Revenue**

### Normal Balances

| Account Type  | Normal Balance | Debit Effect | Credit Effect |
|---------------|----------------|--------------|---------------|
| **Asset**     | Debit          | Increase     | Decrease      |
| **Liability** | Credit         | Decrease     | Increase      |
| **Equity**    | Credit         | Decrease     | Increase      |
| **Revenue**   | Credit         | Decrease     | Increase      |
| **Expense**   | Debit          | Increase     | Decrease      |

### Accrual Basis

Revenue is recognized when earned, and expenses are recognized when incurred — not necessarily when cash moves. This
ensures accurate financial reporting across accounting periods.

---

## Core Functionalities

### Double-Entry Accounting

The foundational mechanism ensuring financial integrity across all platform operations.

**Entry Structure:**

```
Journal Entry: JE-2026-07-17-001
Transaction: PAY-2026-07-17-8842
Date: 2026-07-17T14:32:00Z

Account                          Debit        Credit
─────────────────────────────────────────────────────
Accounts Receivable — Merchant A   $100.00
    Revenue — Transaction Fees                    $3.00
    Cash — Settlement Account                   $97.00
─────────────────────────────────────────────────────
Total                              $100.00      $100.00 ✓
```

**Guarantees:**

- Atomic posting: all lines of a journal entry succeed or none do.
- Idempotency: duplicate posting requests with the same `idempotencyKey` return the existing entry.
- Immutable: posted entries cannot be edited or deleted.

---

### Debit Entries

Records increases to asset/expense accounts or decreases to liability/equity/revenue accounts.

**Common Debit Scenarios:**

| Scenario               | Account Debited           | Effect            |
|------------------------|---------------------------|-------------------|
| Customer pays merchant | Cash / Settlement Account | Asset increases   |
| Platform earns fee     | Accounts Receivable       | Asset increases   |
| Chargeback issued      | Revenue — Chargebacks     | Expense increases |
| Refund processed       | Revenue — Refunds         | Revenue decreases |
| Reserve held           | Reserve Asset             | Asset increases   |

---

### Credit Entries

Records increases to liability/equity/revenue accounts or decreases to asset/expense accounts.

**Common Credit Scenarios:**

| Scenario                  | Account Credited          | Effect              |
|---------------------------|---------------------------|---------------------|
| Merchant earns revenue    | Revenue — Merchant Sales  | Revenue increases   |
| Fee liability to platform | Fee Payable               | Liability increases |
| Settlement payout         | Cash / Settlement Account | Asset decreases     |
| Reserve released          | Reserve Asset             | Asset decreases     |
| Customer deposit          | Customer Deposits Payable | Liability increases |

---

### Account Balances

Maintains real-time, strongly consistent balances for every ledger account.

**Balance Types:**

| Type                  | Description                                            |
|-----------------------|--------------------------------------------------------|
| **Current Balance**   | Real-time balance including all posted entries.        |
| **Available Balance** | Current balance minus pending reservations and holds.  |
| **Period Balance**    | Balance at the close of a specific accounting period.  |
| **Opening Balance**   | Balance carried forward from the previous period.      |
| **Closing Balance**   | Final balance after all entries in the current period. |

**Balance Computation:**

```
Closing Balance = Opening Balance + Σ(Debits) − Σ(Credits)  [for asset/expense accounts]
Closing Balance = Opening Balance + Σ(Credits) − Σ(Debits)  [for liability/equity/revenue accounts]
```

**Consistency Model:**

- Balances are updated synchronously with journal entry posting.
- Optimistic locking prevents concurrent balance updates from causing drift.
- Balance snapshots are taken at period close for audit and reporting.

---

### Ledger Accounts

The foundational classification structure for all financial records.

**Account Hierarchy:**

```
1 — Assets
  11 — Current Assets
    1101 — Cash — Settlement Account
    1102 — Cash — Operating Account
    1103 — Accounts Receivable — Merchants
    1104 — Reserve Assets
  12 — Non-Current Assets
    1201 — Fixed Assets
    1202 — Intangible Assets

2 — Liabilities
  21 — Current Liabilities
    2101 — Customer Deposits Payable
    2102 — Fee Payable to Merchants
    2103 — Tax Payable
  22 — Non-Current Liabilities
    2201 — Long-Term Debt

3 — Equity
  31 — Share Capital
  32 — Retained Earnings

4 — Revenue
  41 — Transaction Revenue
    4101 — Merchant Transaction Fees
    4102 — Interchange Revenue
    4103 — Subscription Revenue
  42 — Other Revenue

5 — Expenses
  51 — Operating Expenses
    5101 — Payment Processing Costs
    5102 — Chargeback Losses
    5103 — Refund Costs
  52 — Administrative Expenses
```

**Account Properties:**

- `accountCode` — Unique hierarchical identifier (e.g., `1101`)
- `accountName` — Human-readable description
- `accountType` — Asset, Liability, Equity, Revenue, Expense
- `normalBalance` — Debit or Credit
- `currency` — Primary currency for the account
- `isActive` — Whether the account accepts new entries
- `parentAccountId` — For hierarchical rollup reporting

---

### Journal Entries

The atomic unit of financial recording. Every journal entry represents a complete, balanced financial event.

**Entry Properties:**

| Property         | Description                                                               |
|------------------|---------------------------------------------------------------------------|
| `journalEntryId` | Unique identifier (e.g., `JE-2026-07-17-001`)                             |
| `transactionId`  | Reference to the business transaction that triggered the entry            |
| `entryDate`      | The effective accounting date (may differ from posting date)              |
| `postingDate`    | The timestamp when the entry was recorded in the ledger                   |
| `description`    | Human-readable explanation of the entry                                   |
| `lines`          | Array of debit/credit lines with account, amount, and description         |
| `sourceSystem`   | The service that originated the entry (Payment, Settlement, Refund, etc.) |
| `idempotencyKey` | Prevents duplicate postings from retries                                  |
| `postedBy`       | System or user responsible for the entry                                  |
| `version`        | Optimistic locking version                                                |

**Line Item Structure:**

```json
{
  "lineNumber": 1,
  "accountCode": "1101",
  "accountName": "Cash — Settlement Account",
  "debitAmount": 9700,
  "creditAmount": 0,
  "currency": "USD",
  "description": "Settlement for transaction PAY-8842"
}
```

---

### Balance Snapshots

Point-in-time captures of account balances for reporting, audit, and reconciliation.

**Snapshot Types:**

| Type                          | Frequency                    | Purpose                                     |
|-------------------------------|------------------------------|---------------------------------------------|
| **Intraday Snapshot**         | On-demand or hourly          | Real-time monitoring and risk management    |
| **End-of-Day (EOD) Snapshot** | Daily at 23:59:59            | Daily reconciliation and reporting          |
| **Period-End Snapshot**       | Monthly / Quarterly / Yearly | Financial statements and regulatory filings |
| **Audit Snapshot**            | Triggered by auditor request | External audit evidence                     |

**Snapshot Content:**

- Account code and name
- Opening balance
- Total debits and credits during the period
- Closing balance
- Number of entries
- Last entry timestamp
- Snapshot timestamp

**Immutability:**

- Snapshots are immutable once taken.
- Any subsequent corrections are applied to the current period, not retroactively to snapshots.

---

### Reversals

The only mechanism for correcting a posted entry. A reversal creates a new journal entry that negates the original.

**Reversal Rules:**

- Original entry is **never modified** — it remains in the ledger permanently.
- Reversal entry uses the same accounts but swaps debits and credits.
- Reversal must reference the original `journalEntryId`.
- Reversal requires a reason code and approval for entries above a threshold.

**Reversal Example:**

```
Original Entry: JE-001
  Accounts Receivable    $100.00 (Dr)
    Revenue                         $100.00 (Cr)

Reversal Entry: JE-001-R
  Revenue                $100.00 (Dr)
    Accounts Receivable             $100.00 (Cr)

Net Effect: Zero (as if the entry never occurred)
```

**Reason Codes:**

- `ERRONEOUS_ENTRY` — Data entry mistake
- `DUPLICATE_POSTING` — Same transaction posted twice
- `TRANSACTION_REVERSED` — Business transaction was reversed (e.g., chargeback)
- `ADJUSTMENT` — Correcting entry for prior period
- `AUDIT_CORRECTION` — Correction mandated by audit finding

---

### Financial Audit

Provides comprehensive tools and data for internal and external financial audits.

**Audit Capabilities:**

| Capability                 | Description                                                                                           |
|----------------------------|-------------------------------------------------------------------------------------------------------|
| **Entry Traceability**     | Every journal entry links back to the originating business transaction and service.                   |
| **User Attribution**       | Every entry records who or what system posted it.                                                     |
| **Timestamp Integrity**    | All entries carry both effective date and posting date; posting date is immutable.                    |
| **Change Log**             | Reversals and corrections are fully logged with before/after states and approval chains.              |
| **Balance Verification**   | Automated verification that Σ(Debits) = Σ(Credits) across all entries.                                |
| **Reconciliation Reports** | Match internal ledger balances against external statements (bank, card network, settlement provider). |
| **Audit Trail Export**     | Generate immutable, cryptographically signed export files for external auditors.                      |

**Audit Queries:**

- All entries for a specific transaction
- All entries posted by a specific user/system in a date range
- All reversals with their original entries
- Balance changes for a specific account over time
- Entries that failed automated balance verification

---

### Posting Engine

The high-throughput, fault-tolerant component responsible for recording journal entries.

**Posting Flow:**

1. **Receive Request** — Accept posting request from source system with entry details and idempotency key.
2. **Validate** — Verify account existence, currency compatibility, and balance constraints.
3. **Balance Check** — Ensure debits equal credits within the entry.
4. **Reserve** — Acquire optimistic lock on affected account balances.
5. **Post** — Atomically insert journal entry lines and update account balances.
6. **Publish** — Emit `LedgerPosted` event for downstream consumers.
7. **Release** — Commit transaction and release locks.

**Performance Characteristics:**

- Synchronous posting with sub-100ms p99 latency.
- Batch posting support for high-volume settlement and reconciliation jobs.
- Dead letter queue for failed postings with automatic retry and alerting.

---

### Accounting Periods

Divides the continuous flow of financial data into discrete, reportable intervals.

**Period Types:**

| Type          | Duration       | Use Case                                           |
|---------------|----------------|----------------------------------------------------|
| **Daily**     | 1 day          | Operational monitoring and intraday reconciliation |
| **Weekly**    | 7 days         | Management reporting                               |
| **Monthly**   | Calendar month | Financial statements, tax filings                  |
| **Quarterly** | 3 months       | Investor reporting, regulatory submissions         |
| **Yearly**    | Fiscal year    | Annual financial statements, audit                 |

**Period Lifecycle:**

```
OPEN → CLOSING → CLOSED → LOCKED
```

| State       | Behavior                                                                                        |
|-------------|-------------------------------------------------------------------------------------------------|
| **OPEN**    | Entries can be posted to the period.                                                            |
| **CLOSING** | No new entries except period-end adjustments and accruals.                                      |
| **CLOSED**  | Period is closed; no further entries allowed.                                                   |
| **LOCKED**  | Period is frozen; even reversals of prior-period entries are blocked without elevated approval. |

**Period-End Activities:**

- Accrual entries for earned-but-not-received revenue
- Depreciation and amortization entries
- Reserve and provision adjustments
- Balance snapshot generation
- Trial balance computation

---

### Trial Balance

A report that lists all ledger accounts and their balances to verify the fundamental accounting equation.

**Trial Balance Structure:**

| Account Code | Account Name          | Debit Balance     | Credit Balance      |
|--------------|-----------------------|-------------------|---------------------|
| 1101         | Cash — Settlement     | $1,250,000.00     |                     |
| 1103         | Accounts Receivable   | $450,000.00       |                     |
| 2101         | Customer Deposits     |                   | $980,000.00         |
| 4101         | Merchant Fees Revenue |                   | $720,000.00         |
| **Total**    |                       | **$1,700,000.00** | **$1,700,000.00** ✓ |

**Types:**

| Type                           | Description                                                    |
|--------------------------------|----------------------------------------------------------------|
| **Unadjusted Trial Balance**   | Raw balances before period-end adjustments.                    |
| **Adjusted Trial Balance**     | Balances after accruals, depreciation, and corrections.        |
| **Post-Closing Trial Balance** | Balances after all closing entries; verifies period integrity. |

**Generation:**

- Automated generation at period close.
- On-demand generation for any historical period.
- Exportable to CSV, Excel, and XBRL formats for regulatory filing.

---

## Chart of Accounts

The complete, hierarchical listing of all accounts in the ledger system.

**Platform-Specific Accounts:**

| Code | Name                            | Type      | Description                                       |
|------|---------------------------------|-----------|---------------------------------------------------|
| 1101 | Cash — Settlement Account       | Asset     | Funds held for merchant settlement                |
| 1102 | Cash — Operating Account        | Asset     | Platform operating cash                           |
| 1103 | Accounts Receivable — Merchants | Asset     | Uncollected merchant fees and charges             |
| 1104 | Reserve Assets                  | Asset     | Rolling reserve held for risk mitigation          |
| 2101 | Customer Deposits Payable       | Liability | Funds held on behalf of customers                 |
| 2102 | Merchant Settlement Payable     | Liability | Amounts owed to merchants                         |
| 2103 | Tax Payable                     | Liability | Sales tax, VAT, and withholding obligations       |
| 4101 | Merchant Transaction Fees       | Revenue   | Platform fee revenue from merchant transactions   |
| 4102 | Interchange Revenue             | Revenue   | Revenue share from card networks                  |
| 4103 | Subscription Revenue            | Revenue   | Monthly/annual SaaS subscription fees             |
| 5101 | Payment Processing Costs        | Expense   | Acquirer and network processing fees              |
| 5102 | Chargeback Losses               | Expense   | Financial losses from disputed transactions       |
| 5103 | Refund Costs                    | Expense   | Costs associated with processing refunds          |
| 5104 | Fraud Losses                    | Expense   | Write-offs from confirmed fraudulent transactions |

---

## Owned Resources

The Ledger Service is the authoritative owner of the following data:

| Resource            | Description                                                                       |
|---------------------|-----------------------------------------------------------------------------------|
| **Ledger Accounts** | The chart of accounts, including hierarchy, types, currencies, and active status. |
| **Journal Entries** | All posted financial entries with their line items, metadata, and audit trail.    |
| **Balances**        | Real-time and snapshot account balances across all dimensions.                    |

> **Note:** Business transactions (payments, settlements, refunds) are owned by their respective domain services. The
> Ledger Service records the financial impact of these events but does not own the business logic that generates them.

---

## Domain Events

The Ledger Service publishes the following events for downstream consumers:

| Event            | Trigger                                                                          |
|------------------|----------------------------------------------------------------------------------|
| `LedgerPosted`   | A journal entry is successfully posted to the ledger.                            |
| `LedgerReversed` | A previously posted journal entry is reversed with a compensating entry.         |
| `BalanceUpdated` | The balance of one or more ledger accounts changes due to a posting or reversal. |

---

## Integration Notes

- **Payment Service**: Consumes payment authorized, captured, failed, and refunded events to post corresponding journal
  entries (revenue recognition, cash movement, fee accrual).
- **Settlement Service**: Consumes settlement completed events to post cash disbursement entries and update merchant
  payable balances.
- **Refund Service**: Consumes refund processed events to post reversal entries and update revenue and cash accounts.
- **Fraud Service**: Consumes fraud confirmed events to post fraud loss expense entries and reserve adjustments.
- **Merchant Service**: Provides merchant-specific account mappings and fee configurations for accurate revenue
  allocation.
- **Limit Service**: Receives balance queries to enforce credit and exposure limits against ledger account balances.
- **Audit Service**: Subscribes to all ledger events for compliance monitoring and generates immutable audit exports.
- **Reporting / BI Service**: Consumes balance snapshots and trial balance data for financial dashboards, investor
  reports, and regulatory filings.
- **Tax Service**: Receives revenue and tax liability entries for automated tax computation and filing.
