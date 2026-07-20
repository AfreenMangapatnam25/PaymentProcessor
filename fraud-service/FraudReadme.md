# Fraud Service

## Overview

The Fraud Service is the platform's real-time risk detection and prevention engine. It evaluates every transaction, login, and sensitive operation against a layered defense system of rules, behavioral models, and machine learning to identify and block suspicious activity before financial loss or reputational damage occurs.

The service operates on a **score-then-decide** model: it ingests signals from across the platform, computes a composite risk score, applies configurable decision policies, and returns a verdict that downstream services enforce. When uncertainty remains, transactions are escalated to a manual review queue rather than auto-declined, balancing security with customer experience.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Defense Layers](#defense-layers)
- [Core Functionalities](#core-functionalities)
  - [Velocity Checks](#velocity-checks)
  - [Device Fingerprinting](#device-fingerprinting)
  - [IP Reputation](#ip-reputation)
  - [Country Risk](#country-risk)
  - [BIN Risk](#bin-risk)
  - [Card Velocity](#card-velocity)
  - [Merchant Risk](#merchant-risk)
  - [User Risk](#user-risk)
  - [Blacklist Management](#blacklist-management)
  - [Whitelist Management](#whitelist-management)
  - [AML Rules](#aml-rules)
  - [Rule Engine](#rule-engine)
  - [ML Scoring](#ml-scoring)
  - [Manual Review Queue](#manual-review-queue)
  - [Risk Score Computation](#risk-score-computation)
  - [Fraud Decision](#fraud-decision)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern | Description |
|---------|-------------|
| **Real-Time Risk Detection** | Evaluate transactions and events within milliseconds to detect anomalies and fraud patterns. |
| **Signal Aggregation** | Ingest and correlate data from devices, networks, users, merchants, cards, and historical behavior. |
| **Decision Automation** | Render approve, decline, or challenge verdicts with minimal human intervention. |
| **Intelligence Learning** | Continuously improve detection accuracy through machine learning and feedback loops from manual reviews. |
| **Regulatory Compliance** | Enforce Anti-Money Laundering (AML) rules and sanctions screening to meet legal obligations. |
| **Operational Escalation** | Route ambiguous or high-value cases to human analysts for manual adjudication. |

---

## Defense Layers

The Fraud Service employs a **multi-layered defense architecture**. Each layer contributes signals to the final risk score.

```
┌─────────────────────────────────────────┐
│  Layer 4: Machine Learning Models       │  ← Behavioral anomaly detection
│  (Unsupervised + Supervised)            │
├─────────────────────────────────────────┤
│  Layer 3: Rule Engine                   │  ← Business logic, thresholds, policies
│  (Velocity, AML, Custom Rules)          │
├─────────────────────────────────────────┤
│  Layer 2: External Intelligence         │  ← IP reputation, country risk, BIN data
│  (Third-party feeds, sanctions lists)   │
├─────────────────────────────────────────┤
│  Layer 1: Device & Network Signals      │  ← Fingerprinting, geolocation, network
│  (Real-time signal ingestion)             │
└─────────────────────────────────────────┘
```

---

## Core Functionalities

### Velocity Checks

Detects abnormal transaction frequency and volume patterns that indicate card testing, account takeover, or bust-out fraud.

**Velocity Dimensions:**

| Dimension | Window | Example Threshold |
|-----------|--------|-------------------|
| **Per Card** | 1 hour | Max 5 transactions |
| **Per User** | 24 hours | Max $10,000 volume |
| **Per Merchant** | 1 hour | Max 100 transactions |
| **Per Device** | 1 hour | Max 3 different cards |
| **Per IP Address** | 15 minutes | Max 10 attempts |
| **Global Platform** | 1 minute | Spike detection (>3σ) |

**Behavior:**
- Breaching a threshold increments the risk score.
- Multiple simultaneous breaches may trigger automatic decline.
- Velocity counters are maintained in a high-performance cache (e.g., Redis) with TTL.

---

### Device Fingerprinting

Identifies and tracks devices used to initiate transactions, detecting device spoofing, emulators, and suspicious device switching.

**Collected Signals:**

| Signal | Purpose |
|--------|---------|
| **Browser Fingerprint** | Canvas, WebGL, fonts, plugins, screen resolution |
| **Device Attributes** | OS, version, model, manufacturer |
| **Network Attributes** | Carrier, connection type (WiFi / cellular) |
| **Behavioral Biometrics** | Typing cadence, mouse movements, touch patterns |
| **Anomaly Flags** | Emulator detection, rooted/jailbroken status, VPN/proxy usage |

**Risk Indicators:**
- New device for an existing user
- Same device associated with multiple high-risk accounts
- Device fingerprint mismatch with historical profile
- Emulator or bot-like behavior detected

---

### IP Reputation

Evaluates the trustworthiness of the IP address originating the request.

**Reputation Sources:**

| Source | Data |
|--------|------|
| **Internal History** | Past fraud rate from this IP |
| **Third-Party Feeds** | Known malicious IPs, botnets, TOR exit nodes, proxies |
| **Geolocation Mismatch** | IP country vs. billing country vs. card-issuing country |
| **Anonymization Detection** | VPN, proxy, TOR, residential proxy services |

**Risk Scoring:**
- Clean IP → No score impact
- Known proxy/VPN → Moderate risk increase
- Known fraud IP / botnet → High risk, likely auto-decline
- IP geolocation mismatch → Risk increase proportional to distance and history

---

### Country Risk

Assesses the risk profile of countries involved in the transaction.

**Evaluated Countries:**
- Card-issuing country (from BIN)
- Billing address country
- IP geolocation country
- Merchant registration country

**Risk Factors:**

| Factor | Impact |
|--------|--------|
| **Sanctions / OFAC** | Immediate block |
| **High-Fraud Jurisdiction** | Elevated risk score (e.g., countries with high chargeback rates) |
| **Cross-Border Mismatch** | Multiple countries in one transaction increases risk |
| **Currency Mismatch** | Transaction currency ≠ card currency ≠ merchant currency |

---

### BIN Risk

Analyzes the Bank Identification Number (first 6 digits of the card) for risk indicators.

**BIN Attributes:**

| Attribute | Risk Relevance |
|-----------|----------------|
| **Issuing Country** | Cross-border transaction risk |
| **Card Type** | Prepaid and virtual cards carry higher fraud rates |
| **Card Level** | Corporate cards vs. consumer cards |
| **Issuer Risk History** | Historical fraud rate from this issuing bank |
| **BIN Velocity** | Unusual transaction volume from this BIN range |

---

### Card Velocity

Tracks the usage patterns of individual card numbers across the platform.

**Monitored Patterns:**

| Pattern | Description |
|---------|-------------|
| **Same Card, Multiple Merchants** | Card used across many merchants in a short window (stolen card testing) |
| **Same Card, Multiple Users** | Card linked to different platform accounts (account takeover or sharing) |
| **Decline Velocity** | Repeated declines followed by a sudden approval (brute-force CVV guessing) |
| **Amount Progression** | Small test transactions followed by large purchases (card verification then exploitation) |

---

### Merchant Risk

Evaluates the risk profile of the merchant receiving the payment.

**Risk Indicators:**

| Indicator | Description |
|-----------|-------------|
| **Chargeback Rate** | Historical chargeback ratio; >1% is high risk |
| **Refund Rate** | Excessive refunds may indicate friendly fraud or poor fulfillment |
| **Transaction Velocity** | Sudden spike in volume vs. historical baseline |
| **Industry Risk** | High-risk MCCs (e.g., gambling, crypto, adult) |
| **Account Age** | Newly onboarded merchants carry higher risk |
| **KYC / KYB Status** | Pending or failed verification elevates risk |

---

### User Risk

Assesses the behavioral and historical risk of the customer initiating the transaction.

**Risk Indicators:**

| Indicator | Description |
|-----------|-------------|
| **Account Age** | New accounts are higher risk |
| **KYC Status** | Unverified or failed KYC elevates risk |
| **Login Anomalies** | Unusual location, time, or device for this user |
| **Transaction History** | First-time purchaser vs. repeat customer with clean history |
| **Dispute History** | Previous chargebacks or disputes filed by this user |
| **Velocity Patterns** | Rapid-fire transactions, unusual amounts, or merchant switching |

---

### Blacklist Management

Maintains lists of entities permanently blocked from transacting.

**Blacklist Types:**

| Type | Scope |
|------|-------|
| **Card Blacklist** | Specific card numbers known to be stolen or fraudulent |
| **User Blacklist** | Platform users permanently banned for fraud |
| **Merchant Blacklist** | Merchants terminated for fraudulent activity |
| **IP Blacklist** | Known malicious IP addresses |
| **Device Blacklist** | Device fingerprints associated with fraud |
| **Email Domain Blacklist** | Disposable email domains or known fraud domains |

**Behavior:**
- Blacklist hits result in **immediate decline** without score evaluation.
- Entries include reason, source, evidence reference, and expiry (if applicable).
- Audit trail for all blacklist additions and removals.

---

### Whitelist Management

Maintains lists of trusted entities that bypass certain risk checks.

**Whitelist Types:**

| Type | Scope |
|------|-------|
| **User Whitelist** | VIP customers, employees, trusted long-term users |
| **Merchant Whitelist** | Strategic partners with pre-negotiated risk terms |
| **IP Whitelist** | Corporate office IPs, known safe locations |
| **Device Whitelist** | User-registered trusted devices |

**Behavior:**
- Whitelist hits may skip specific rules or reduce the risk score floor.
- Whitelist does **not** override AML sanctions or blacklist entries.
- Requires elevated privileges to add or remove entries.

---

### AML Rules

Enforces Anti-Money Laundering regulations to prevent illicit financial flows.

**AML Checks:**

| Check | Description |
|-------|-------------|
| **Sanctions Screening** | Screen names, addresses, and entities against OFAC, UN, EU, and HMT sanctions lists. |
| **PEP Screening** | Identify Politically Exposed Persons and their close associates. |
| **Transaction Monitoring** | Detect structuring (breaking large amounts into small transactions), layering, and rapid movement of funds. |
| **Threshold Reporting** | Flag transactions exceeding regulatory thresholds for Currency Transaction Reports (CTR). |
| **Suspicious Activity Patterns** | Unusual transaction patterns that may indicate money laundering (e.g., round amounts, rapid in/out flows). |

**Behavior:**
- Sanctions hits → **Immediate block**, freeze funds, alert compliance team.
- PEP hits → Elevated risk, enhanced due diligence required.
- Structuring detected → Flag for SAR (Suspicious Activity Report) filing.

---

### Rule Engine

A configurable, high-performance engine that evaluates business-defined fraud rules against incoming transactions.

**Rule Types:**

| Type | Description |
|------|-------------|
| **Static Rules** | Hard thresholds (e.g., "decline if amount > $5,000 and IP is from high-risk country"). |
| **Dynamic Rules** | Rules that adapt based on time, merchant, or user segment. |
| **Composite Rules** | Multi-condition rules with AND/OR logic and weighted scoring. |
| **Velocity Rules** | Time-window-based counters and limits. |
| **Geographic Rules** | Distance, country mismatch, and geofencing logic. |

**Rule Configuration:**
- Rules are versioned and deployed without code changes.
- A/B testing support for rule effectiveness measurement.
- Rule hit rate and false positive tracking.
- Priority ordering: blacklist → AML → static rules → ML model.

---

### ML Scoring

Machine learning models that detect subtle, non-obvious fraud patterns beyond the reach of static rules.

**Model Types:**

| Type | Description |
|------|-------------|
| **Supervised Models** | Trained on labeled historical fraud data (Random Forest, XGBoost, Neural Networks). |
| **Unsupervised Models** | Detect anomalies in real-time without labeled data (Isolation Forest, Autoencoders). |
| **Graph Models** | Analyze entity relationships (user → card → merchant → device) to detect fraud rings and collusion. |
| **Sequence Models** | Analyze transaction sequences to detect behavioral drift (LSTM, Transformers). |

**Feature Engineering:**
- Aggregated historical features (avg amount, time since last transaction, merchant diversity)
- Real-time contextual features (time of day, device trust, IP reputation)
- Cross-entity features (shared cards, shared devices, merchant-user graph distance)

**Model Lifecycle:**
- Training pipeline with automated retraining on new fraud labels.
- Shadow mode deployment before production enablement.
- Feature drift and model performance monitoring.
- Explainability: SHAP values or feature importance for every score.

---

### Manual Review Queue

A human-in-the-loop system for cases where automated scoring is inconclusive or regulatory review is required.

**Queue Routing Logic:**

| Condition | Action |
|-----------|--------|
| Risk score between 40–70 (medium risk) | Route to standard review queue |
| Risk score > 70 but whitelisted user | Route to senior analyst queue |
| High-value transaction (>$10,000) | Mandatory review regardless of score |
| AML alert triggered | Route to compliance team queue |
| First transaction for new merchant | Route to merchant risk queue |

**Review Workflow:**
1. Case created with full context (transaction details, risk signals, user history).
2. Analyst reviews and selects verdict: **Approve**, **Decline**, or **Request More Info**.
3. Verdict is applied; transaction state updated.
4. Analyst decision is fed back to ML models as a training label.
5. SLA tracking: 95% of cases resolved within 15 minutes during business hours.

---

### Risk Score Computation

Aggregates all signals into a single normalized risk score (0–100).

**Scoring Formula (Simplified):**
```
Risk Score = Σ(rule_weights × rule_hits) + ML_model_score + external_intelligence_score
```

**Score Interpretation:**

| Range | Classification | Typical Action |
|-------|----------------|----------------|
| 0–25 | Low Risk | Auto-approve |
| 26–40 | Low-Medium Risk | Approve with monitoring |
| 41–60 | Medium Risk | Approve with 3DS challenge |
| 61–75 | High Risk | Route to manual review queue |
| 76–90 | Very High Risk | Auto-decline |
| 91–100 | Critical Risk | Auto-decline + account review + alert compliance |

**Score Attributes:**
- Real-time computation (< 100ms p99).
- Score breakdown returned with every decision (which rules fired, ML contribution).
- Score is immutable after computation; stored for audit and model training.

---

### Fraud Decision

The final verdict rendered after all layers have been evaluated.

**Decision Types:**

| Verdict | Description |
|---------|-------------|
| **APPROVE** | Transaction cleared; proceed to authorization. |
| **DECLINE** | Transaction blocked; return decline reason to merchant. |
| **CHALLENGE** | Require step-up authentication (3DS, OTP, biometric) before proceeding. |
| **REVIEW** | Hold transaction pending manual analyst review. |
| **ESCALATE** | Immediate compliance and security team alert (sanctions hit, confirmed fraud ring). |

**Decision Payload:**
```json
{
  "decision": "CHALLENGE",
  "riskScore": 58,
  "confidence": 0.82,
  "rulesFired": ["velocity_card_1h", "device_new"],
  "mlContribution": 0.35,
  "reasonCode": "RISK_MEDIUM_DEVICE_ANOMALY",
  "recommendedAction": "INITIATE_3DS",
  "reviewQueueId": null
}
```

---

## Owned Resources

The Fraud Service is the authoritative owner of the following data:

| Resource | Description |
|----------|-------------|
| **Rules** | Fraud detection rules, their conditions, weights, thresholds, and versioning. |
| **Risk Scores** | Computed risk scores for every evaluated transaction, including component breakdowns. |
| **Fraud Decisions** | Final verdicts (approve, decline, challenge, review) with audit trail and reasoning. |

> **Note:** Transaction data is owned by the Payment Service. User and merchant profiles are owned by the User Service and Merchant Service, respectively. Sanctions and PEP lists are sourced from external providers.

---

## Domain Events

The Fraud Service publishes the following events for downstream consumers:

| Event | Trigger |
|-------|---------|
| `FraudDetected` | A transaction or event triggers a fraud rule, exceeds a risk threshold, or matches a known fraud pattern. |
| `ManualReviewRequired` | The risk score or rule configuration routes the case to the human analyst queue. |
| `FraudApproved` | A transaction initially flagged is approved after review (manual or automated secondary check). |
| `FraudRejected` | A transaction is definitively declined due to fraud detection. |

---

## Integration Notes

- **Payment Service**: Receives transaction data for real-time risk evaluation before authorization; enforces the fraud decision (approve, decline, challenge).
- **Authentication Service**: Receives login events for account takeover detection; may request step-up authentication for suspicious logins.
- **User Service**: Queries user history, KYC status, and device registrations for risk signal enrichment.
- **Merchant Service**: Retrieves merchant risk profile, chargeback history, and industry classification.
- **Tokenization Service**: Accesses card metadata (BIN, type, country) without exposing full PAN.
- **External Intelligence Providers**: Consumes IP reputation feeds, device fingerprinting services, sanctions lists, and PEP databases.
- **Notification Service**: Alerts users and merchants of suspicious activity, blocked transactions, and review outcomes.
- **Compliance / Audit Service**: Receives all fraud decisions, AML alerts, and manual review outcomes for regulatory reporting.
- **ML Platform / Data Lake**: Provides training data (features + labels) and consumes model inference results.
