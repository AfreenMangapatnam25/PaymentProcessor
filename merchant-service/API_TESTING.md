# merchant-service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `merchant-service` makes no outbound calls to any other
service — it's called *by* `settlement-service` for merchant/settlement-account
lookups, not the other way around. Fully testable standalone.

**Infrastructure:** Postgres (`merchantservicedb`). Kafka for outbox events
(optional for REST testing). Config Server is optional.

Base URL: `http://localhost:8083` (`server.port` in `application.yml`, override with `SERVER_PORT`)

All endpoints are served under `/api/v1/merchants...`. There is **no enforced authentication** on
this service today — some operations are commented `(admin)` in the controller but nothing actually
checks a credential or role, so any client can call them. Treat this as a gap to close before
exposing the service beyond trusted internal networks.

To exercise the examples below against a local database with realistic data already present, run
the service with the `local` Spring profile (`SPRING_PROFILES_ACTIVE=local`), which loads
`db/seed/V2__seed_sample_data.sql` in addition to the base schema migration. That seed creates:

| Entity                                             | Fixed id                               |
|----------------------------------------------------|----------------------------------------|
| Merchant (`Acme Retail Holdings LLC`, ACTIVE)      | `11111111-1111-1111-1111-111111111111` |
| Business address (REGISTERED, primary)             | `22222222-2222-2222-2222-222222222222` |
| API key (PRODUCTION, ACTIVE)                       | `33333333-3333-3333-3333-333333333333` |
| Settlement account (SETTLEMENT, default, VERIFIED) | `44444444-4444-4444-4444-444444444444` |

The examples below use that merchant id (`11111111-1111-1111-1111-111111111111`) wherever a
GET/list/update against an existing merchant is shown.

---

## Merchants — `MerchantController` (`/api/v1/merchants`)

### POST /api/v1/merchants

Onboard a new merchant (admin).

Request body:

```json
{
  "legalBusinessName": "Acme Retail Holdings LLC",
  "tradingName": "Acme Retail",
  "registrationNumber": "REG-88213-CA",
  "taxId": "TAX-99231-CA",
  "mcc": "5732",
  "businessType": "LLC",
  "websiteUrl": "https://www.acmeretail.example",
  "supportEmail": "support@acmeretail.example",
  "supportPhone": "+14155550100",
  "country": "US",
  "defaultCurrency": "USD",
  "ownerUserId": "55555555-5555-5555-5555-555555555555"
}
```

Response (201 Created, `Location: /api/v1/merchants/{id}`):

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "merchantReference": "MER-0001SEED",
  "legalBusinessName": "Acme Retail Holdings LLC",
  "tradingName": "Acme Retail",
  "registrationNumber": "REG-88213-CA",
  "taxId": "TAX-99231-CA",
  "mcc": "5732",
  "businessType": "LLC",
  "websiteUrl": "https://www.acmeretail.example",
  "supportEmail": "support@acmeretail.example",
  "supportPhone": "+14155550100",
  "country": "US",
  "defaultCurrency": "USD",
  "ownerUserId": "55555555-5555-5555-5555-555555555555",
  "status": "PENDING",
  "kybStatus": "PENDING",
  "pricingPlan": "STANDARD",
  "activatedAt": null,
  "suspendedAt": null,
  "suspensionReason": null,
  "createdAt": "2026-07-25T10:00:00Z",
  "updatedAt": "2026-07-25T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants \
  -H "Content-Type: application/json" \
  -d '{"legalBusinessName":"Acme Retail Holdings LLC","tradingName":"Acme Retail","registrationNumber":"REG-88213-CA","taxId":"TAX-99231-CA","mcc":"5732","businessType":"LLC","websiteUrl":"https://www.acmeretail.example","supportEmail":"support@acmeretail.example","supportPhone":"+14155550100","country":"US","defaultCurrency":"USD","ownerUserId":"55555555-5555-5555-5555-555555555555"}'
```

### GET /api/v1/merchants

List merchants (admin). Query params: `status` (optional, `MerchantStatus` enum: `PENDING`,
`UNDER_REVIEW`, `ACTIVE`, `SUSPENDED`, `RESTRICTED`, `TERMINATED`, `BLACKLISTED`), plus standard
Spring `Pageable` params (`page`, `size`, `sort`).

Response (200):

```json
{
  "content": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "merchantReference": "MER-0001SEED",
      "legalBusinessName": "Acme Retail Holdings LLC",
      "status": "ACTIVE",
      "kybStatus": "VERIFIED",
      "pricingPlan": "STANDARD"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

curl:

```bash
curl "http://localhost:8083/api/v1/merchants?status=ACTIVE&page=0&size=20"
```

### GET /api/v1/merchants/{merchantId}

Response (200): same shape as the onboarding response above, `status: "ACTIVE"`.

curl:

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111
```

### PUT /api/v1/merchants/{merchantId}

Replace merchant profile (full replacement of mutable profile fields).

Request body:

```json
{
  "legalBusinessName": "Acme Retail Holdings LLC",
  "tradingName": "Acme Retail Co",
  "registrationNumber": "REG-88213-CA",
  "taxId": "TAX-99231-CA",
  "mcc": "5732",
  "businessType": "LLC",
  "websiteUrl": "https://www.acmeretail.example",
  "supportEmail": "help@acmeretail.example",
  "supportPhone": "+14155550101"
}
```

curl:

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111 \
  -H "Content-Type: application/json" \
  -d '{"legalBusinessName":"Acme Retail Holdings LLC","tradingName":"Acme Retail Co","registrationNumber":"REG-88213-CA","taxId":"TAX-99231-CA","mcc":"5732","businessType":"LLC","websiteUrl":"https://www.acmeretail.example","supportEmail":"help@acmeretail.example","supportPhone":"+14155550101"}'
```

### PATCH /api/v1/merchants/{merchantId}

Partial update; only non-null fields are applied.

Request body:

```json
{
  "supportPhone": "+14155559999"
}
```

curl:

```bash
curl -X PATCH http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111 \
  -H "Content-Type: application/json" \
  -d '{"supportPhone":"+14155559999"}'
```

### POST /api/v1/merchants/{merchantId}/status

Change merchant lifecycle status (admin).

Request body:

```json
{
  "targetStatus": "SUSPENDED",
  "reason": "Elevated chargeback ratio under review"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/status \
  -H "Content-Type: application/json" \
  -d '{"targetStatus":"SUSPENDED","reason":"Elevated chargeback ratio under review"}'
```

### PUT /api/v1/merchants/{merchantId}/pricing-plan

Assign a pricing plan (admin).

Request body:

```json
{
  "pricingPlan": "VOLUME_TIERED"
}
```

curl:

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/pricing-plan \
  -H "Content-Type: application/json" \
  -d '{"pricingPlan":"VOLUME_TIERED"}'
```

---

## Business Addresses — `AddressController` (`/api/v1/merchants/{merchantId}/addresses`)

### GET /addresses

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/addresses
```

Response (200):

```json
[
  {
    "id": "22222222-2222-2222-2222-222222222222",
    "merchantId": "11111111-1111-1111-1111-111111111111",
    "addressType": "REGISTERED",
    "line1": "500 Market Street",
    "line2": "Suite 200",
    "city": "San Francisco",
    "region": "CA",
    "postalCode": "94105",
    "country": "US",
    "primary": true
  }
]
```

### POST /addresses

Request body (`AddressType`: `REGISTERED`, `OPERATING`, `BILLING`, `MAILING`):

```json
{
  "addressType": "BILLING",
  "line1": "500 Market Street",
  "line2": "Suite 200",
  "city": "San Francisco",
  "region": "CA",
  "postalCode": "94105",
  "country": "US",
  "primary": false
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/addresses \
  -H "Content-Type: application/json" \
  -d '{"addressType":"BILLING","line1":"500 Market Street","line2":"Suite 200","city":"San Francisco","region":"CA","postalCode":"94105","country":"US","primary":false}'
```

### PUT /addresses/{addressId}

Same body shape as POST. Example: update the seeded address.

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/addresses/22222222-2222-2222-2222-222222222222 \
  -H "Content-Type: application/json" \
  -d '{"addressType":"REGISTERED","line1":"501 Market Street","city":"San Francisco","region":"CA","postalCode":"94105","country":"US","primary":true}'
```

### POST /addresses/{addressId}/primary

Marks the given address primary (unsets others).

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/addresses/22222222-2222-2222-2222-222222222222/primary
```

### DELETE /addresses/{addressId}

```bash
curl -X DELETE http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/addresses/22222222-2222-2222-2222-222222222222
```

Response: `204 No Content`.

---

## API Keys — `ApiKeyController` (`/api/v1/merchants/{merchantId}/api-keys`)

### GET /api-keys

Lists active keys.

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/api-keys
```

Response (200):

```json
[
  {
    "id": "33333333-3333-3333-3333-333333333333",
    "merchantId": "11111111-1111-1111-1111-111111111111",
    "keyId": "pk_live_seed00001",
    "keyType": "PRODUCTION",
    "status": "ACTIVE",
    "label": "Primary production key",
    "ipAllowlist": null,
    "lastUsedAt": null,
    "expiresAt": null,
    "createdAt": "2026-07-25T10:00:00Z"
  }
]
```

### POST /api-keys

Generates a new key; the plaintext `secret` is returned once only.

Request body (`ApiKeyType`: `PRODUCTION`, `SANDBOX`, `READ_ONLY`):

```json
{
  "keyType": "SANDBOX",
  "label": "CI test key",
  "ipAllowlist": "203.0.113.0/24",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

Response (201):

```json
{
  "apiKey": {
    "id": "66666666-6666-6666-6666-666666666666",
    "merchantId": "11111111-1111-1111-1111-111111111111",
    "keyId": "sk_sandbox_9f2a1c",
    "keyType": "SANDBOX",
    "status": "ACTIVE",
    "label": "CI test key",
    "ipAllowlist": "203.0.113.0/24",
    "lastUsedAt": null,
    "expiresAt": "2027-01-01T00:00:00Z",
    "createdAt": "2026-07-25T10:00:00Z"
  },
  "secret": "sk_live_raw_secret_shown_once_only_1234567890"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/api-keys \
  -H "Content-Type: application/json" \
  -d '{"keyType":"SANDBOX","label":"CI test key","ipAllowlist":"203.0.113.0/24","expiresAt":"2027-01-01T00:00:00Z"}'
```

### POST /api-keys/{keyId}/rotate

Issues a new secret for the same key id record (or a replacement), old one revoked.

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/api-keys/33333333-3333-3333-3333-333333333333/rotate
```

### PUT /api-keys/{keyId}/ip-allowlist

Query param `ipAllowlist` (optional, max 1024 chars, comma-separated CIDRs typically).

```bash
curl -X PUT "http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/api-keys/33333333-3333-3333-3333-333333333333/ip-allowlist?ipAllowlist=203.0.113.0/24,198.51.100.0/24"
```

### DELETE /api-keys/{keyId}

```bash
curl -X DELETE http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/api-keys/33333333-3333-3333-3333-333333333333
```

Response: `204 No Content`.

---

## Beneficial Owners — `BeneficialOwnerController` (`/api/v1/merchants/{merchantId}/beneficial-owners`)

### GET /beneficial-owners

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/beneficial-owners
```

### POST /beneficial-owners

Request body (`BeneficialOwnerRole`: `BENEFICIAL_OWNER`, `DIRECTOR`, `AUTHORIZED_SIGNATORY`, `OFFICER`):

```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "1985-03-14",
  "email": "jane.doe@acmeretail.example",
  "role": "DIRECTOR",
  "ownershipPercentage": 51.5,
  "nationality": "US"
}
```

Response (201):

```json
{
  "id": "77777777-7777-7777-7777-777777777777",
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "1985-03-14",
  "email": "jane.doe@acmeretail.example",
  "role": "DIRECTOR",
  "ownershipPercentage": 51.5,
  "nationality": "US",
  "kycStatus": "PENDING"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/beneficial-owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","dateOfBirth":"1985-03-14","email":"jane.doe@acmeretail.example","role":"DIRECTOR","ownershipPercentage":51.5,"nationality":"US"}'
```

### PUT /beneficial-owners/{ownerId}

Same body as POST.

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/beneficial-owners/77777777-7777-7777-7777-777777777777 \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","dateOfBirth":"1985-03-14","email":"jane.doe@acmeretail.example","role":"DIRECTOR","ownershipPercentage":60.0,"nationality":"US"}'
```

### PUT /beneficial-owners/{ownerId}/kyc-status

Query param `status` (`KycStatus`: `PENDING`, `VERIFIED`, `FAILED`, `EXPIRED`).

```bash
curl -X PUT "http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/beneficial-owners/77777777-7777-7777-7777-777777777777/kyc-status?status=VERIFIED"
```

### DELETE /beneficial-owners/{ownerId}

```bash
curl -X DELETE http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/beneficial-owners/77777777-7777-7777-7777-777777777777
```

---

## Branding — `BrandingController` (`/api/v1/merchants/{merchantId}/branding`)

### GET /branding

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/branding
```

Response (200):

```json
{
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "logoUrl": null,
  "primaryColor": null,
  "secondaryColor": null,
  "statementDescriptor": null,
  "customDomain": null,
  "emailTemplateRef": null
}
```

### PUT /branding

Request body (colors must be `#RRGGBB` or `#RRGGBBAA`):

```json
{
  "logoUrl": "https://cdn.acmeretail.example/logo.png",
  "primaryColor": "#1A73E8",
  "secondaryColor": "#FF6D00",
  "statementDescriptor": "ACME RETAIL",
  "customDomain": "pay.acmeretail.example",
  "emailTemplateRef": "acme-default-v2"
}
```

curl:

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/branding \
  -H "Content-Type: application/json" \
  -d '{"logoUrl":"https://cdn.acmeretail.example/logo.png","primaryColor":"#1A73E8","secondaryColor":"#FF6D00","statementDescriptor":"ACME RETAIL","customDomain":"pay.acmeretail.example","emailTemplateRef":"acme-default-v2"}'
```

---

## Fees & Pricing — `FeeController` (`/api/v1/merchants/{merchantId}/fees`)

### GET /fees

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/fees
```

### PUT /fees

Request body (`PricingPlan`: `STANDARD`, `VOLUME_TIERED`, `ENTERPRISE`, `STARTER`):

```json
{
  "pricingPlan": "STANDARD",
  "transactionFeePercent": 2.9,
  "transactionFeeFixed": 0.30,
  "interchangePassThrough": false,
  "monthlyPlatformFee": 0,
  "chargebackFee": 15.00,
  "refundFee": 0,
  "payoutFee": 0,
  "currency": "USD"
}
```

Response (200):

```json
{
  "id": "88888888-8888-8888-8888-888888888888",
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "pricingPlan": "STANDARD",
  "transactionFeePercent": 2.9,
  "transactionFeeFixed": 0.30,
  "interchangePassThrough": false,
  "monthlyPlatformFee": 0,
  "chargebackFee": 15.00,
  "refundFee": 0,
  "payoutFee": 0,
  "currency": "USD",
  "active": true
}
```

curl:

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/fees \
  -H "Content-Type: application/json" \
  -d '{"pricingPlan":"STANDARD","transactionFeePercent":2.9,"transactionFeeFixed":0.30,"interchangePassThrough":false,"monthlyPlatformFee":0,"chargebackFee":15.00,"refundFee":0,"payoutFee":0,"currency":"USD"}'
```

### POST /fees/preview

Request body:

```json
{
  "amount": 100.00,
  "currency": "USD"
}
```

Response (200):

```json
{
  "amount": 100.00,
  "currency": "USD",
  "percentComponent": 2.90,
  "fixedComponent": 0.30,
  "totalFee": 3.20,
  "netToMerchant": 96.80
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/fees/preview \
  -H "Content-Type: application/json" \
  -d '{"amount":100.00,"currency":"USD"}'
```

---

## KYB Verification — `KybController` (`/api/v1/merchants/{merchantId}/kyb`)

### GET /kyb/cases

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/cases
```

### GET /kyb/cases/latest

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/cases/latest
```

Response (200):

```json
{
  "id": "99999999-9999-9999-9999-999999999999",
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "status": "APPROVED",
  "externalReference": "COMPLIANCE-CASE-4471",
  "riskScore": 12,
  "sanctionsScreened": true,
  "pepScreened": true,
  "decisionReason": "All checks passed",
  "submittedAt": "2026-06-25T09:00:00Z",
  "reviewedAt": "2026-06-26T14:30:00Z"
}
```

### POST /kyb/cases

Opens a new case for the merchant (no body).

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/cases
```

### POST /kyb/cases/{caseId}/decision

Callback from the Compliance/KYB service recording a decision.

Request body (`KybCaseStatus`: `OPEN`, `IN_REVIEW`, `APPROVED`, `REJECTED`):

```json
{
  "decision": "APPROVED",
  "riskScore": 12,
  "sanctionsScreened": true,
  "pepScreened": true,
  "decisionReason": "All checks passed",
  "externalReference": "COMPLIANCE-CASE-4471"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/cases/99999999-9999-9999-9999-999999999999/decision \
  -H "Content-Type: application/json" \
  -d '{"decision":"APPROVED","riskScore":12,"sanctionsScreened":true,"pepScreened":true,"decisionReason":"All checks passed","externalReference":"COMPLIANCE-CASE-4471"}'
```

### GET /kyb/documents

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/documents
```

### POST /kyb/documents

Request body (`KybDocumentType`: `BUSINESS_LICENSE`, `TAX_ID`, `BANK_STATEMENT`, `IDENTITY_PROOF`,
`PROOF_OF_ADDRESS`, `ARTICLES_OF_INCORPORATION`, `OTHER`):

```json
{
  "documentType": "BUSINESS_LICENSE",
  "fileName": "business-license.pdf",
  "contentType": "application/pdf",
  "storageReference": "s3://merchant-docs/11111111-1111-1111-1111-111111111111/business-license.pdf",
  "expiresOn": "2028-01-01"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/documents \
  -H "Content-Type: application/json" \
  -d '{"documentType":"BUSINESS_LICENSE","fileName":"business-license.pdf","contentType":"application/pdf","storageReference":"s3://merchant-docs/11111111-1111-1111-1111-111111111111/business-license.pdf","expiresOn":"2028-01-01"}'
```

### DELETE /kyb/documents/{documentId}

```bash
curl -X DELETE http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/kyb/documents/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
```

---

## Merchant Configuration — `MerchantConfigurationController` (`/api/v1/merchants/{merchantId}/configuration`)

### GET /configuration

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/configuration
```

### PATCH /configuration

Null fields fall back to existing values.

Request body:

```json
{
  "captureMode": "AUTOMATIC",
  "enforce3ds": true,
  "velocityLimitPerDay": 5000,
  "payoutSchedule": "DAILY",
  "minimumPayoutThreshold": 50.00,
  "instantPayoutEligible": false,
  "fraudScreeningLevel": "MEDIUM",
  "chargebackAlertThreshold": 10,
  "reservePercentage": 5.0,
  "notifyOnPayout": true,
  "notifyOnChargeback": true,
  "notifyOnStatusChange": true,
  "kycRefreshIntervalDays": 365
}
```

Response (200):

```json
{
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "captureMode": "AUTOMATIC",
  "enforce3ds": true,
  "velocityLimitPerDay": 5000,
  "payoutSchedule": "DAILY",
  "minimumPayoutThreshold": 50.00,
  "instantPayoutEligible": false,
  "fraudScreeningLevel": "MEDIUM",
  "chargebackAlertThreshold": 10,
  "reservePercentage": 5.0,
  "notifyOnPayout": true,
  "notifyOnChargeback": true,
  "notifyOnStatusChange": true,
  "kycRefreshIntervalDays": 365
}
```

curl:

```bash
curl -X PATCH http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/configuration \
  -H "Content-Type: application/json" \
  -d '{"captureMode":"AUTOMATIC","enforce3ds":true,"velocityLimitPerDay":5000,"payoutSchedule":"DAILY","minimumPayoutThreshold":50.00,"instantPayoutEligible":false,"fraudScreeningLevel":"MEDIUM","chargebackAlertThreshold":10,"reservePercentage":5.0,"notifyOnPayout":true,"notifyOnChargeback":true,"notifyOnStatusChange":true,"kycRefreshIntervalDays":365}'
```

---

## Payment Methods — `PaymentMethodController` (`/api/v1/merchants/{merchantId}/payment-methods`)

### GET /payment-methods

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/payment-methods
```

### PUT /payment-methods

Upsert one method's config. Request body (`PaymentMethodType`: `CARD`, `BANK_TRANSFER`,
`DIGITAL_WALLET`, `BNPL`, `CRYPTO`, `LOCAL`):

```json
{
  "methodType": "CARD",
  "enabled": true,
  "settingsJson": "{\"networks\":[\"VISA\",\"MASTERCARD\"]}"
}
```

curl:

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/payment-methods \
  -H "Content-Type: application/json" \
  -d '{"methodType":"CARD","enabled":true,"settingsJson":"{\"networks\":[\"VISA\",\"MASTERCARD\"]}"}'
```

### POST /payment-methods/{methodType}/enable

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/payment-methods/CARD/enable
```

### POST /payment-methods/{methodType}/disable

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/payment-methods/CARD/disable
```

---

## Settlement Accounts — `SettlementAccountController` (`/api/v1/merchants/{merchantId}/settlement-accounts`)

### GET /settlement-accounts

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/settlement-accounts
```

Response (200):

```json
[
  {
    "id": "44444444-4444-4444-4444-444444444444",
    "merchantId": "11111111-1111-1111-1111-111111111111",
    "purpose": "SETTLEMENT",
    "accountHolderName": "Acme Retail Holdings LLC",
    "bankName": "First National Bank",
    "bankCode": "FNBKUS44",
    "routingNumber": "121000358",
    "swift": "FNBKUS44XXX",
    "accountNumberLast4": "6789",
    "accountClass": "CHECKING",
    "currency": "USD",
    "country": "US",
    "defaultAccount": true,
    "verificationStatus": "VERIFIED"
  }
]
```

### POST /settlement-accounts

Request body — raw account number is accepted and encrypted at rest; never returned.

```json
{
  "purpose": "SETTLEMENT",
  "accountHolderName": "Acme Retail Holdings LLC",
  "bankName": "First National Bank",
  "bankCode": "FNBKUS44",
  "routingNumber": "121000358",
  "swift": "FNBKUS44XXX",
  "accountNumber": "000123456789",
  "accountClass": "CHECKING",
  "currency": "USD",
  "country": "US",
  "defaultAccount": false
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/settlement-accounts \
  -H "Content-Type: application/json" \
  -d '{"purpose":"SETTLEMENT","accountHolderName":"Acme Retail Holdings LLC","bankName":"First National Bank","bankCode":"FNBKUS44","routingNumber":"121000358","swift":"FNBKUS44XXX","accountNumber":"000123456789","accountClass":"CHECKING","currency":"USD","country":"US","defaultAccount":false}'
```

### PUT /settlement-accounts/{accountId}

Same body shape as POST.

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/settlement-accounts/44444444-4444-4444-4444-444444444444 \
  -H "Content-Type: application/json" \
  -d '{"purpose":"SETTLEMENT","accountHolderName":"Acme Retail Holdings LLC","bankName":"First National Bank","bankCode":"FNBKUS44","routingNumber":"121000358","swift":"FNBKUS44XXX","accountNumber":"000123456789","accountClass":"CHECKING","currency":"USD","country":"US","defaultAccount":true}'
```

### POST /settlement-accounts/{accountId}/default

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/settlement-accounts/44444444-4444-4444-4444-444444444444/default
```

### POST /settlement-accounts/{accountId}/verify

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/settlement-accounts/44444444-4444-4444-4444-444444444444/verify
```

### DELETE /settlement-accounts/{accountId}

```bash
curl -X DELETE http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/settlement-accounts/44444444-4444-4444-4444-444444444444
```

---

## Webhooks — `WebhookController` (`/api/v1/merchants/{merchantId}/webhooks`)

### GET /webhooks

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/webhooks
```

### POST /webhooks

`endpointUrl` must be `https://...`. Request body (`WebhookEventType`: `PAYMENT_SUCCEEDED`,
`PAYMENT_FAILED`, `REFUND_PROCESSED`, `CHARGEBACK_RECEIVED`, `PAYOUT_COMPLETED`,
`MERCHANT_STATUS_CHANGED`):

```json
{
  "endpointUrl": "https://webhooks.acmeretail.example/payments",
  "events": [
    "PAYMENT_SUCCEEDED",
    "PAYMENT_FAILED",
    "REFUND_PROCESSED"
  ],
  "maxRetries": 5,
  "timeoutSeconds": 10,
  "active": true
}
```

Response (201) — `signingSecret` is returned once only:

```json
{
  "webhook": {
    "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "merchantId": "11111111-1111-1111-1111-111111111111",
    "endpointUrl": "https://webhooks.acmeretail.example/payments",
    "events": [
      "PAYMENT_SUCCEEDED",
      "PAYMENT_FAILED",
      "REFUND_PROCESSED"
    ],
    "active": true,
    "maxRetries": 5,
    "timeoutSeconds": 10
  },
  "signingSecret": "whsec_raw_secret_shown_once_only_abcdef123456"
}
```

curl:

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/webhooks \
  -H "Content-Type: application/json" \
  -d '{"endpointUrl":"https://webhooks.acmeretail.example/payments","events":["PAYMENT_SUCCEEDED","PAYMENT_FAILED","REFUND_PROCESSED"],"maxRetries":5,"timeoutSeconds":10,"active":true}'
```

### PUT /webhooks/{webhookId}

Same body shape as POST (no secret returned).

```bash
curl -X PUT http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/webhooks/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb \
  -H "Content-Type: application/json" \
  -d '{"endpointUrl":"https://webhooks.acmeretail.example/payments-v2","events":["PAYMENT_SUCCEEDED"],"maxRetries":3,"timeoutSeconds":15,"active":true}'
```

### POST /webhooks/{webhookId}/test

Triggers a test delivery. Response: `202 Accepted`.

```bash
curl -X POST http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/webhooks/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/test
```

### GET /webhooks/{webhookId}/deliveries

```bash
curl http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/webhooks/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/deliveries
```

### DELETE /webhooks/{webhookId}

```bash
curl -X DELETE http://localhost:8083/api/v1/merchants/11111111-1111-1111-1111-111111111111/webhooks/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
```
