# Payment Processor Platform - Service Architecture

## Core Services

### 1. **Gateway Service** (Port 8080)

- API Gateway, request routing, rate limiting, authentication middleware
- Entry point for all client requests

### 2. **Authentication Service** (Port 8081)

- JWT token generation and validation
- OAuth2/SAML integration
- Multi-factor authentication

### 3. **User Service** (Port 8082)

- User account management
- Customer KYC/AML verification
- Profile and preference management

### 4. **Merchant Service** (Port 8083)

- Merchant onboarding and management
- Merchant KYC verification
- Risk scoring and tier management
- Chargeback tracking

## Payment Processing Services

### 5. **Payment Service** (Port 8080)

- Core payment processing orchestration
- Transaction creation and status management
- Payment method switching

### 6. **Tokenization Service** (Port 8084) ⭐ NEW

- PAN tokenization and token mapping
- Vault operations for secure storage
- Detokenization with PCI compliance
- End-to-end encryption and audit trails

### 7. **Limit Service** (Port 8085) ⭐ NEW

- Transaction limits management (daily, monthly, per-transaction)
- Velocity checks and fraud detection
- Spending controls by category and merchant
- Real-time monitoring using Redis

### 8. **Authorization Service** (Port 8086) ⭐ NEW

- Real-time transaction authorization (approve/decline)
- Funds hold management
- AVS (Address Verification System) validation
- Card validation (Luhn, BIN, expiry)
- 3D Secure/MFA support

### 9. **Connector Service** (Port 8089)

- Integration with payment networks (Visa, Mastercard, etc.)
- Network protocol handling
- Transaction submission to acquiring banks

### 10. **Vault Service**

- Secure credential storage
- Encryption key management
- PCI DSS compliance

## Post-Transaction Services

### 11. **Dispute Service** (Port 8090)

- Chargeback management
- Full/partial refunds and reversals
- Dispute-linked refunds
- Evidence management
- Liability tracking
- Representment handling
- Network reason code management

### 12. **Fraud Service**

- Real-time fraud detection
- Behavioral analytics
- Machine learning models for fraud scoring
- Blacklist/whitelist management

### 13. **Ledger Service**

- Double-entry accounting
- Transaction journal entries
- Financial reconciliation
- Real-time balance tracking

### 14. **Settlement Service**

- Batch settlement processing
- Fund settlement scheduling
- Settlement statement generation

### 15. **Clearing Service**

- Clearing house integration
- Transaction clearing and batch processing
- Network messaging

### 16. **Reconciliation Service**

- Automated reconciliation between systems
- Bank statement matching
- Discrepancy detection and resolution

## Support Services

### 17. **Pricing Service**

- Transaction fee calculation
- Rate management
- Dispute fee handling

### 18. **Notification Service**

- Email, SMS, and webhook notifications
- Event-driven notification delivery
- Template management

### 19. **Audit Service**

- Compliance logging
- Regulatory audit trails
- User activity tracking

### 20. **Reporting Service**

- Transaction reporting
- Financial reporting
- Analytics and dashboards

### 21. **Analytics Service**

- Business intelligence
- Data warehouse integration
- Real-time metrics

---

## Service Dependencies & Data Flow

```
Client Request
    ↓
[Gateway Service] 
    ↓
[Authentication Service]
    ↓
[User/Merchant Services]
    ↓
[Payment Service] (Orchestrator)
    ├─→ [Tokenization Service] (Secure card data)
    ├─→ [Limit Service] (Check transaction limits)
    ├─→ [Authorization Service] (Real-time decision)
    └─→ [Connector Service] (Network submission)
         ├─→ [Vault Service] (Credential retrieval)
         └─→ [Fraud Service] (Risk assessment)
             ↓
        [Clearing Service]
             ↓
        [Dispute Service] (if chargeback)
             ├─→ [Ledger Service] (Accounting)
             ├─→ [Pricing Service] (Fee calculation)
             └─→ [Notification Service]
                  ↓
        [Settlement Service]
             ├─→ [Reconciliation Service]
             ├─→ [Ledger Service] (Final settlement)
             └─→ [Reporting/Audit Services]
```

## Technology Stack

- **Framework:** Spring Boot 3.3.2
- **Service Discovery:** Eureka
- **Message Queue:** Kafka (event streaming)
- **Database:** PostgreSQL (primary), Redis (caching/velocity)
- **Security:** Spring Security, JWT, TLS
- **Monitoring:** Spring Boot Actuator, Prometheus metrics
- **Logging:** ELK Stack compatible

## Database Schema (Per Service)

### Tokenization Service

- `token_vault` - Encrypted PAN tokens
- `pan_token_mapping` - Token-to-PAN mappings
- `tokenization_audit` - Compliance audit log

### Limit Service

- `customer_limits` - Transaction limits per customer
- `merchant_limits` - Merchant-specific rules
- `velocity_metrics` - Real-time velocity tracking
- `spending_controls` - Category/merchant restrictions

### Authorization Service

- `authorizations` - Transaction auth records
- `fund_holds` - Active funds holds
- `authorization_rules` - Decision rules engine
- `card_validation_logs` - Validation audit trail

### Dispute Service

- `disputes` - Chargeback records
- `dispute_events` - State transitions
- `evidence` - Dispute evidence files (S3)
- `representments` - Dispute representments
- `liability` - Liability tracking

---

## Configuration Ports

| Service        | Port | Database          | Cache |
|----------------|------|-------------------|-------|
| Gateway        | 8080 | -                 | -     |
| Authentication | 8081 | PostgreSQL        | Redis |
| User           | 8082 | PostgreSQL        | Redis |
| Merchant       | 8083 | PostgreSQL        | Redis |
| Tokenization   | 8084 | PostgreSQL        | -     |
| Limit          | 8085 | PostgreSQL        | Redis |
| Authorization  | 8086 | PostgreSQL        | -     |
| Connector      | 8089 | PostgreSQL        | -     |
| Dispute        | 8090 | PostgreSQL (+ S3) | -     |

---

## PCI DSS Compliance

- **Tokenization Service**: AES-256 encryption, key rotation
- **Vault Service**: HSM integration, secure key storage
- **Audit Service**: Complete audit trails for all PAN operations
- All services: TLS 1.2+, secure communication channels

