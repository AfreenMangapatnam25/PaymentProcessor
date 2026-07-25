# Dispute Service

## Overview

The Dispute Service is the platform's end-to-end chargeback and dispute management system. It governs the entire
lifecycle of payment disputes — from the moment a chargeback is received from a card network or bank, through evidence
collection and representment, to final resolution (won, lost, or arbitrated). The service ensures merchants are properly
notified, deadlines are tracked, evidence is organized, and financial impacts are accurately recorded across the ledger
and settlement systems.

Disputes are time-sensitive, regulated events with strict network-imposed deadlines (typically 7–10 days for initial
response, up to 45 days for arbitration). Missing a deadline results in automatic loss of the dispute and irreversible
financial liability. The service is therefore designed with aggressive deadline tracking, automated escalation, and
proactive merchant communication.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Dispute Lifecycle](#dispute-lifecycle)
- [Core Functionalities](#core-functionalities)
    - [Dispute Creation](#dispute-creation)
    - [Chargeback Processing](#chargeback-processing)
    - [Evidence Collection](#evidence-collection)
    - [Document Upload](#document-upload)
    - [Representment](#representment)
    - [Arbitration](#arbitration)
    - [Merchant Notifications](#merchant-notifications)
    - [Dispute Status Management](#dispute-status-management)
    - [Refund Coordination](#refund-coordination)
    - [Deadline Tracking](#deadline-tracking)
- [Dispute Reasons & Codes](#dispute-reasons--codes)
- [Financial Impact](#financial-impact)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern                             | Description                                                                                                   |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------|
| **Dispute Lifecycle Management**    | Track every dispute from receipt through resolution with full audit trail.                                    |
| **Deadline Compliance**             | Monitor and enforce card network deadlines to prevent automatic losses due to missed response windows.        |
| **Evidence Orchestration**          | Collect, validate, and submit merchant evidence to card networks in the required format and within deadlines. |
| **Financial Accuracy**              | Ensure chargeback debits, representment credits, and fee postings are correctly recorded in the ledger.       |
| **Merchant Advocacy**               | Maximize merchant win rates by guiding merchants through evidence requirements and dispute best practices.    |
| **Regulatory & Network Compliance** | Adhere to card network rules (Visa, Mastercard, Amex, Discover) and regional consumer protection laws.        |

---

## Dispute Lifecycle

```
CHARGEBACK RECEIVED
        │
        ▼
┌─────────────────┐
│ 1. CREATION     │ ──► Validate dispute, link to original transaction,
│    (Open)       │     create dispute record, notify merchant
└─────────────────┘
        │
        ▼
┌─────────────────┐
│ 2. EVIDENCE     │ ──► Merchant uploads evidence, platform reviews
│    (Pending)    │     and packages for submission
└─────────────────┘
        │
        ├──► Merchant accepts liability ──► CLOSED (Lost)
        │
        ▼
┌─────────────────┐
│ 3. REPRESENTMENT│ ──► Submit evidence to card network
│    (Submitted)  │
└─────────────────┘
        │
        ├──► Issuer accepts ──► CLOSED (Won)
        │
        ├──► Issuer rejects ──► PRE-ARBITRATION
        │
        └──► Issuer escalates ──► ARBITRATION
        │
        ▼
┌─────────────────┐
│ 4. ARBITRATION  │ ──► Card network adjudicates final decision
│    (Arbitrated) │
└─────────────────┘
        │
        ├──► Won ──► CLOSED (Won)
        │
        └──► Lost ──► CLOSED (Lost)
```

**Dispute States:**

| State                | Description                                                                           |
|----------------------|---------------------------------------------------------------------------------------|
| **OPEN**             | Dispute received, merchant notified, awaiting response or evidence.                   |
| **PENDING_EVIDENCE** | Merchant has been requested to provide evidence; deadline clock running.              |
| **EVIDENCE_REVIEW**  | Evidence uploaded by merchant; platform team reviewing for completeness and validity. |
| **REPRESENTED**      | Evidence package submitted to the card network; awaiting issuer decision.             |
| **PRE_ARBITRATION**  | Issuer rejected representment; optional pre-arbitration response required.            |
| **ARBITRATION**      | Case escalated to card network for final binding decision.                            |
| **ACCEPTED**         | Merchant or platform accepted liability; no further action.                           |
| **WON**              | Dispute resolved in merchant's favor; chargeback reversed.                            |
| **LOST**             | Dispute resolved against merchant; chargeback stands.                                 |
| **CLOSED**           | Final state; no further action possible.                                              |

---

## Core Functionalities

### Dispute Creation

Initiates a new dispute record when a chargeback or retrieval request is received from a card network, bank, or payment
provider.

**Creation Triggers:**

| Source              | Type              | Description                                                        |
|---------------------|-------------------|--------------------------------------------------------------------|
| **Card Network**    | Chargeback        | Customer disputed transaction with their issuing bank              |
| **Card Network**    | Retrieval Request | Issuer requests transaction documentation before formal chargeback |
| **Acquirer**        | Notification      | Acquirer forwards network chargeback notification                  |
| **Payment Gateway** | Dispute Event     | Gateway-specific dispute webhook (e.g., Stripe dispute.created)    |
| **Internal**        | Manual Creation   | Operations team creates dispute for non-standard scenarios         |

**Dispute Record Fields:**

| Field               | Description                                                             |
|---------------------|-------------------------------------------------------------------------|
| `disputeId`         | Unique platform dispute identifier                                      |
| `chargebackId`      | Network-assigned chargeback reference (e.g., Visa TC40, Mastercard ARD) |
| `transactionId`     | Reference to the original platform transaction                          |
| `paymentId`         | Reference to the original payment                                       |
| `merchantId`        | Merchant who received the original payment                              |
| `customerId`        | Customer who initiated the dispute                                      |
| `amount`            | Disputed amount (may be partial)                                        |
| `currency`          | Dispute currency                                                        |
| `reasonCode`        | Network-specific dispute reason code                                    |
| `reasonDescription` | Human-readable reason                                                   |
| `network`           | Visa, Mastercard, Amex, Discover, etc.                                  |
| `receivedAt`        | Timestamp when dispute was received                                     |
| `deadlineAt`        | Response deadline imposed by the network                                |
| `status`            | Current dispute state                                                   |
| `liability`         | `MERCHANT`, `PLATFORM`, or `PENDING`                                    |

**Creation Flow:**

1. Receive chargeback notification from network/acquirer/gateway.
2. Validate the dispute (exists, not duplicate, within time limits).
3. Link to original transaction and payment.
4. Create dispute record with network deadlines.
5. Debit merchant's account for the disputed amount + chargeback fee.
6. Publish `DisputeCreated` event.
7. Notify merchant immediately (email, dashboard alert, webhook).

---

### Chargeback Processing

Handles the financial and operational impact of a chargeback on the merchant and platform.

**Immediate Actions on Chargeback Receipt:**

| Action                    | Description                                                              |
|---------------------------|--------------------------------------------------------------------------|
| **Fund Debit**            | Deduct disputed amount from merchant's next settlement or reserve        |
| **Fee Assessment**        | Apply chargeback fee ($15–$100 depending on network and merchant volume) |
| **Reserve Adjustment**    | Increase rolling reserve if chargeback rate exceeds threshold            |
| **Merchant Notification** | Alert merchant with dispute details and response deadline                |
| **Transaction Flagging**  | Mark original transaction as disputed in Payment Service                 |
| **Ledger Posting**        | Post chargeback loss and fee expense entries to Ledger Service           |

**Chargeback Fee Structure:**

| Network        | Fee Range | Notes                                     |
|----------------|-----------|-------------------------------------------|
| **Visa**       | $15–$25   | Higher for excessive chargeback merchants |
| **Mastercard** | $15–$30   | Additional fee for second chargeback      |
| **Amex**       | $25–$50   | Higher due to direct issuer relationship  |
| **Discover**   | $20–$40   | Varies by merchant agreement              |

**Chargeback Rate Monitoring:**

- Monthly chargeback rate = (Chargeback count / Transaction count) × 100
- Visa threshold: 0.9% (standard), 1.8% (excessive)
- Mastercard threshold: 1.5% (standard), 3.0% (excessive)
- Exceeding thresholds triggers network monitoring programs (Visa VMMP, Mastercard ECP) and potential fines.

---

### Evidence Collection

Guides merchants through gathering and organizing the documentation needed to fight a dispute.

**Evidence Requirements by Dispute Reason:**

| Reason Code Category                | Required Evidence                                                      | Optional Evidence                                    |
|-------------------------------------|------------------------------------------------------------------------|------------------------------------------------------|
| **Fraud (10.4)**                    | AVS result, CVV result, 3DS proof, device fingerprint, IP geolocation  | Customer communication, delivery confirmation        |
| **Product Not Received (13.1)**     | Tracking number, delivery confirmation, signature proof, shipping date | Customer communication, refund policy acknowledgment |
| **Product Not as Described (13.3)** | Product description, photos, terms of service, customer communication  | Independent assessment, refund offer evidence        |
| **Duplicate Processing (12.6)**     | Proof of separate transactions, distinct authorization codes           | Customer acknowledgment of multiple purchases        |
| **Canceled Recurring (13.2)**       | Cancellation request date, refund policy, terms of service             | Proof of service delivery up to cancellation date    |
| **Credit Not Processed (13.6)**     | Refund policy, proof refund was issued or not due                      | Customer communication about refund expectations     |

**Evidence Quality Guidelines:**

- Must be legible and in English (or translated with certification).
- Must directly address the specific reason code.
- Must be submitted before the network deadline.
- Screenshots must include URL bar and timestamps where applicable.
- Communication logs must include full headers and timestamps.

---

### Document Upload

Provides a secure mechanism for merchants to upload dispute evidence.

**Supported Document Types:**

| Type              | Formats        | Max Size       |
|-------------------|----------------|----------------|
| **Images**        | JPG, PNG, TIFF | 10 MB per file |
| **PDFs**          | PDF            | 25 MB per file |
| **Text Files**    | TXT, CSV       | 5 MB per file  |
| **Audio/Video**   | MP3, MP4, WAV  | 50 MB per file |
| **Email Exports** | EML, MSG       | 10 MB per file |

**Upload Features:**

- Drag-and-drop interface in merchant dashboard
- API endpoint for programmatic uploads
- Virus/malware scanning on all uploads
- Automatic OCR for text extraction from images and PDFs
- Document categorization (receipt, tracking, communication, policy, etc.)
- Version control (replace previous upload before submission)

**Security:**

- All documents encrypted at rest (AES-256)
- Access restricted to merchant, platform dispute team, and authorized network reviewers
- Audit log of all document access and downloads
- Retention policy: 7 years for regulatory compliance

---

### Representment

Submits the merchant's evidence package to the card network to challenge the chargeback.

**Representment Flow:**

1. **Evidence Review** — Platform team reviews uploaded evidence for completeness and relevance.
2. **Package Assembly** — Compile evidence into network-specific format (Visa VROL, Mastercard MDES, etc.).
3. **Pre-Submission Check** — Validate all required fields, reason code alignment, and deadline compliance.
4. **Submission** — Send representment to acquirer/network via API or file exchange.
5. **Acknowledgment** — Receive submission confirmation and tracking reference.
6. **Status Update** — Transition dispute to `REPRESENTED` state.
7. **Await Decision** — Monitor for issuer response (typically 30–45 days).

**Representment Rules:**

- Must be submitted before the network deadline (typically 7–10 days from chargeback date).
- Partial representment is not supported — must represent for full disputed amount or accept liability.
- Only one representment per chargeback (except for pre-arbitration and arbitration phases).
- Representment fee may apply ($5–$25 depending on network).

---

### Arbitration

Handles escalated disputes where the issuer rejects the merchant's representment and the case proceeds to card network
arbitration.

**Arbitration Flow:**

1. **Pre-Arbitration Notice** — Issuer sends pre-arbitration notice rejecting representment.
2. **Decision Point** — Merchant/platform decides to:
    - **Accept Liability** → Dispute lost, case closed
    - **Respond to Pre-Arbitration** → Submit additional evidence (if new information exists)
    - **Proceed to Arbitration** → Escalate to network for binding decision
3. **Arbitration Filing** — Submit case to card network with full evidence package and filing fee.
4. **Network Review** — Card network reviews both sides (issuer and merchant/acquirer).
5. **Binding Decision** — Network renders final decision (cannot be appealed).

**Arbitration Fees:**

| Network        | Filing Fee            | Potential Liability                  |
|----------------|-----------------------|--------------------------------------|
| **Visa**       | $500                  | Up to $500 + original dispute amount |
| **Mastercard** | $625                  | Up to $625 + original dispute amount |
| **Amex**       | $0 (internal process) | Original dispute amount only         |
| **Discover**   | $350                  | Up to $350 + original dispute amount |

**Arbitration Strategy:**

- Only proceed if evidence is overwhelmingly strong and dispute amount justifies the fee.
- Track arbitration win/loss rates by reason code and merchant category for strategic guidance.
- Automatic recommendation engine suggests accept vs. arbitrate based on historical data.

---

### Merchant Notifications

Ensures merchants are informed at every critical stage of the dispute lifecycle.

**Notification Triggers:**

| Event                             | Channel                     | Urgency         |
|-----------------------------------|-----------------------------|-----------------|
| **Chargeback Received**           | Email + Dashboard + Webhook | Immediate       |
| **Evidence Requested**            | Email + Dashboard           | Within 24 hours |
| **Deadline Approaching (3 days)** | Email + SMS + Dashboard     | High            |
| **Deadline Approaching (1 day)**  | Email + SMS + Dashboard     | Critical        |
| **Representment Submitted**       | Email + Dashboard           | Standard        |
| **Dispute Won**                   | Email + Dashboard + Webhook | Standard        |
| **Dispute Lost**                  | Email + Dashboard + Webhook | Standard        |
| **Arbitration Required**          | Email + Dashboard + Phone   | High            |

**Notification Content:**

- Dispute ID and chargeback reference
- Disputed amount and currency
- Reason code and description
- Response deadline with countdown
- Link to evidence upload portal
- Recommended evidence based on reason code
- Historical win rate for similar disputes
- Contact information for dispute support team

---

### Dispute Status Management

Provides real-time visibility into the current state and history of every dispute.

**Status Query:**

- `GET /disputes/{disputeId}` — Full dispute details, evidence, timeline, and financial impact
- `GET /disputes/{disputeId}/status` — Current state and next required action
- `GET /merchants/{merchantId}/disputes` — All disputes for a merchant with filtering

**Status Timeline:**

| Event                      | Actor             | Description                               |
|----------------------------|-------------------|-------------------------------------------|
| `DISPUTE_CREATED`          | System            | Chargeback received and recorded          |
| `MERCHANT_NOTIFIED`        | System            | Merchant alerted via email/dashboard      |
| `EVIDENCE_REQUESTED`       | System            | Merchant asked to upload evidence         |
| `EVIDENCE_UPLOADED`        | Merchant          | Merchant submitted evidence               |
| `EVIDENCE_REVIEWED`        | Platform          | Dispute team reviewed evidence            |
| `REPRESENTMENT_SUBMITTED`  | Platform          | Evidence package sent to network          |
| `ISSUER_RESPONSE_RECEIVED` | Network           | Issuer accepted or rejected representment |
| `PRE_ARBITRATION_RECEIVED` | Network           | Issuer escalated to pre-arbitration       |
| `ARBITRATION_FILED`        | Platform          | Case escalated to network arbitration     |
| `ARBITRATION_DECISION`     | Network           | Final binding decision rendered           |
| `DISPUTE_WON`              | Network           | Chargeback reversed, funds returned       |
| `DISPUTE_LOST`             | Network           | Chargeback stands, liability confirmed    |
| `DISPUTE_ACCEPTED`         | Merchant/Platform | Liability accepted without fight          |

---

### Refund Coordination

Coordinates refunds with the dispute process to prevent duplicate credits and optimize merchant outcomes.

**Refund Scenarios:**

| Scenario                       | Action                                                                                                       |
|--------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Refund before chargeback**   | If refund was issued before chargeback, use refund evidence in representment                                 |
| **Refund after chargeback**    | If merchant refunds after chargeback received, may still fight dispute with refund as evidence of good faith |
| **Refund instead of fighting** | Merchant accepts liability; platform processes refund and closes dispute                                     |
| **Partial refund**             | Complex — may reduce disputed amount but not eliminate chargeback; consult network rules                     |

**Coordination Rules:**

- If a refund is processed while a dispute is open, the dispute team is automatically notified.
- Refund evidence (receipt, confirmation email) is automatically attached to the dispute record.
- Platform recommends refund vs. fight based on amount, reason code, and historical win rate.
- Duplicate credit prevention: block refund if chargeback already debited merchant, or adjust next settlement
  accordingly.

---

### Deadline Tracking

Monitors and enforces all network-imposed deadlines to prevent automatic losses.

**Key Deadlines:**

| Deadline                     | Network    | Days from Chargeback | Consequence of Miss |
|------------------------------|------------|----------------------|---------------------|
| **Initial Response**         | Visa       | 10 days              | Automatic loss      |
| **Initial Response**         | Mastercard | 7 days               | Automatic loss      |
| **Initial Response**         | Amex       | 7 days               | Automatic loss      |
| **Initial Response**         | Discover   | 10 days              | Automatic loss      |
| **Pre-Arbitration Response** | Visa       | 10 days              | Automatic loss      |
| **Pre-Arbitration Response** | Mastercard | 7 days               | Automatic loss      |
| **Arbitration Filing**       | Visa       | 10 days              | Automatic loss      |
| **Arbitration Filing**       | Mastercard | 7 days               | Automatic loss      |

**Deadline Management:**

| Feature                    | Description                                                    |
|----------------------------|----------------------------------------------------------------|
| **Countdown Display**      | Real-time countdown visible to merchant and operations team    |
| **Automated Reminders**    | Email/SMS at T-7, T-3, T-1 days                                |
| **Escalation Alerts**      | Operations manager alerted at T-2 days if no evidence uploaded |
| **Auto-Extension Request** | Request deadline extension from network (not guaranteed)       |
| **Calendar Integration**   | Export deadlines to operations team calendar                   |
| **SLA Monitoring**         | Track percentage of disputes responded to before deadline      |

**SLA Targets:**

- 100% of disputes with evidence submitted before T-1 day
- 95% of disputes with evidence submitted before T-3 days
- 0% automatic losses due to missed deadlines

---

## Dispute Reasons & Codes

Card networks use specific reason codes to categorize why a customer disputed a transaction.

### Visa Reason Codes

| Code | Description                           | Win Rate | Key Evidence                                      |
|------|---------------------------------------|----------|---------------------------------------------------|
| 10.1 | EMV Liability Shift Counterfeit       | High     | EMV chip read proof, fallback data                |
| 10.4 | Other Fraud — Card-Absent Environment | Medium   | 3DS, AVS, CVV, device fingerprint                 |
| 11.3 | Cardholder Authorization              | Medium   | Signed authorization, terms acceptance            |
| 12.4 | Incorrect Account Number              | High     | Authorization record showing correct PAN          |
| 12.6 | Duplicate Processing                  | High     | Distinct authorization codes, transaction details |
| 13.1 | Merchandise/Services Not Received     | Low      | Tracking, delivery confirmation, signature        |
| 13.3 | Not as Described or Defective         | Low      | Product description, photos, communication        |
| 13.2 | Canceled Recurring                    | Medium   | Cancellation date, terms, refund policy           |
| 13.6 | Credit Not Processed                  | Medium   | Refund policy, proof of credit or non-eligibility |

### Mastercard Reason Codes

| Code | Description                        | Win Rate | Key Evidence                                    |
|------|------------------------------------|----------|-------------------------------------------------|
| 4837 | No Cardholder Authorization        | Medium   | 3DS, signed authorization, communication        |
| 4840 | Fraudulent Processing              | Medium   | Authorization record, merchant documentation    |
| 4841 | Canceled Recurring                 | Medium   | Cancellation proof, terms of service            |
| 4853 | Cardholder Dispute                 | Low      | Varies by sub-reason; generally service-related |
| 4854 | Cardholder Dispute — Not Elsewhere | Low      | Product/service evidence                        |
| 4855 | Goods or Services Not Provided     | Low      | Delivery proof, service logs                    |
| 4860 | Credit Not Processed               | Medium   | Refund evidence, policy documentation           |

### Amex Reason Codes

| Code | Description                         | Win Rate | Key Evidence                       |
|------|-------------------------------------|----------|------------------------------------|
| A01  | Charge Amount Exceeds Authorization | High     | Authorization record               |
| C02  | Credit Not Processed                | Medium   | Refund documentation               |
| C04  | Goods/Services Returned or Refused  | Low      | Return policy, delivery proof      |
| C05  | Goods/Services Canceled             | Medium   | Cancellation proof, terms          |
| C08  | Goods/Services Not Received         | Low      | Tracking, delivery confirmation    |
| C14  | Paid by Other Means                 | High     | Proof of alternative payment       |
| C18  | No Show or CARDeposit Canceled      | Medium   | Cancellation policy, communication |
| F10  | Missing Imprint                     | High     | Imprint or authorization record    |
| F14  | Missing Signature                   | Medium   | Signed receipt or PIN entry proof  |
| F24  | No Cardholder Authorization         | Medium   | 3DS, communication logs            |
| F29  | Card Not Present                    | Medium   | AVS, CVV, 3DS evidence             |
| F30  | EMV Counterfeit                     | High     | EMV data, fallback records         |

---

## Financial Impact

Tracks and records the monetary impact of disputes across the platform.

### Chargeback Financial Flow

```
Step 1: Chargeback Received
  ├─► Debit Merchant Settlement Account:     $100.00 (disputed amount)
  ├─► Debit Merchant Fee Account:               $25.00 (chargeback fee)
  └─► Credit Platform Chargeback Reserve:     $125.00

Step 2: Representment Won
  ├─► Credit Merchant Settlement Account:     $100.00
  ├─► Credit Merchant Fee Account:            $25.00
  └─► Debit Platform Chargeback Reserve:      $125.00

Step 3: Representment Lost (or no response)
  ├─► Debit Platform Chargeback Reserve:      $125.00
  ├─► Credit Platform Revenue — Chargebacks:  $100.00
  └─► Credit Platform Revenue — Fees:           $25.00
```

### Reserve Impact

| Chargeback Rate | Reserve Action                                                         |
|-----------------|------------------------------------------------------------------------|
| < 0.5%          | Standard reserve (e.g., 5% rolling 90 days)                            |
| 0.5% – 0.9%     | Increased reserve (e.g., 10% rolling 120 days)                         |
| 0.9% – 1.8%     | High reserve (e.g., 20% rolling 180 days) + monitoring                 |
| > 1.8%          | Excessive reserve (e.g., 30% rolling 270 days) + potential termination |

### Merchant Impact Dashboard

Merchants can view:

- Total disputes (open, won, lost, pending)
- Dispute rate (% of transactions)
- Financial impact (total debited, total recovered)
- Win rate by reason code
- Average resolution time
- Upcoming deadlines

---

## Owned Resources

The Dispute Service is the authoritative owner of the following data:

| Resource        | Description                                                                                   |
|-----------------|-----------------------------------------------------------------------------------------------|
| **Disputes**    | All dispute records with lifecycle state, reason codes, deadlines, and resolution outcomes.   |
| **Evidence**    | Uploaded documents, their metadata, categorization, and submission history.                   |
| **Chargebacks** | Chargeback-specific data including network references, amounts, fees, and financial postings. |

> **Note:** Transaction data is owned by the Payment Service. Merchant and customer data are owned by the Merchant
> Service and User Service, respectively. Ledger entries for chargeback debits/credits are owned by the Ledger Service.
> The Dispute Service orchestrates the dispute process but does not own the underlying business records.

---

## Domain Events

The Dispute Service publishes the following events for downstream consumers:

| Event                | Trigger                                                                                         |
|----------------------|-------------------------------------------------------------------------------------------------|
| `DisputeCreated`     | A new dispute or chargeback is received and recorded in the system.                             |
| `ChargebackReceived` | A chargeback notification is processed from a card network, acquirer, or gateway.               |
| `DisputeWon`         | The dispute is resolved in the merchant's favor; chargeback is reversed and funds are returned. |
| `DisputeLost`        | The dispute is resolved against the merchant; chargeback stands and liability is confirmed.     |

---

## Integration Notes

- **Payment Service**: Queries original transaction details (amount, date, method, 3DS result, AVS result) for evidence
  assembly and dispute linking.
- **Merchant Service**: Retrieves merchant configuration, settlement account details, chargeback rate history, and
  reserve settings; sends notifications to merchant contacts.
- **User / Customer Service**: Retrieves customer profile and communication history for evidence gathering.
- **Ledger Service**: Posts chargeback debits, representment credits, fee expenses, and reserve adjustments as journal
  entries.
- **Settlement Service**: Coordinates chargeback fund recovery from merchant settlements and reserve releases upon
  dispute resolution.
- **Refund Service**: Coordinates refund processing during open disputes to prevent duplicate credits.
- **Card Networks / Acquirers**: Receives chargeback notifications and sends representment packages via network-specific
  protocols (Visa VROL, Mastercard MDES, etc.).
- **Notification Service**: Sends merchant alerts, deadline reminders, and status updates via email, SMS, dashboard, and
  webhooks.
- **Audit Service**: Subscribes to all dispute events for compliance logging, regulatory reporting, and network audit
  requirements.
- **Analytics / BI Service**: Consumes dispute outcomes for win rate analysis, reason code trending, and merchant risk
  scoring.
