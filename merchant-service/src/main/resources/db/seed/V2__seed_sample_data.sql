-- Sample data for local/dev only. Loaded via the "local" Spring profile, which adds
-- classpath:db/seed to spring.flyway.locations (see application.yml). Never applied
-- in an environment that doesn't activate the "local" profile.
--
-- Seeds one realistic, fully-onboarded ACTIVE merchant with a primary business
-- address, a default verified settlement account, and a live production API key.
-- Fixed, deterministic UUIDs so the values in API_TESTING.md are directly testable.

INSERT INTO merchant (
    id, version, created_at, updated_at,
    merchant_reference, legal_business_name, trading_name, registration_number, tax_id,
    mcc, business_type, website_url, support_email, support_phone,
    country, default_currency, owner_user_id,
    status, kyb_status, pricing_plan,
    activated_at, suspended_at, suspension_reason, terminated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111', 0, now(), now(),
    'MER-0001SEED', 'Acme Retail Holdings LLC', 'Acme Retail', 'REG-88213-CA', 'TAX-99231-CA',
    '5732', 'LLC', 'https://www.acmeretail.example', 'support@acmeretail.example', '+14155550100',
    'US', 'USD', '55555555-5555-5555-5555-555555555555',
    'ACTIVE', 'VERIFIED', 'STANDARD',
    now() - interval '30 days', NULL, NULL, NULL
) ON CONFLICT (id) DO NOTHING;

INSERT INTO business_address (
    id, version, created_at, updated_at,
    merchant_id, address_type, line1, line2, city, region, postal_code, country, is_primary
) VALUES (
    '22222222-2222-2222-2222-222222222222', 0, now(), now(),
    '11111111-1111-1111-1111-111111111111', 'REGISTERED', '500 Market Street', 'Suite 200',
    'San Francisco', 'CA', '94105', 'US', TRUE
) ON CONFLICT (id) DO NOTHING;

INSERT INTO settlement_account (
    id, version, created_at, updated_at,
    merchant_id, purpose, account_holder_name, bank_name, bank_code, routing_number, swift,
    iban_last4, account_number_encrypted, account_number_last4, account_class,
    currency, country, is_default, verification_status
) VALUES (
    '44444444-4444-4444-4444-444444444444', 0, now(), now(),
    '11111111-1111-1111-1111-111111111111', 'SETTLEMENT', 'Acme Retail Holdings LLC', 'First National Bank',
    'FNBKUS44', '121000358', 'FNBKUS44XXX',
    NULL, 'seed:AES256:kQ9v2mZ4bXcR7tYuIoPaSdFgHjKlZxCvBnMqWeRtYuI9placeholderEncryptedValue==', '6789', 'CHECKING',
    'USD', 'US', TRUE, 'VERIFIED'
) ON CONFLICT (id) DO NOTHING;

-- key_id/secret_hash are placeholders only: the plaintext secret is never
-- recoverable once generated, so no real API call can authenticate with this
-- seeded row -- it exists so list/get and downstream reads have a row to show.
INSERT INTO api_key (
    id, version, created_at, updated_at,
    merchant_id, key_id, secret_hash, key_type, status, label, ip_allowlist,
    last_used_at, revoked_at, expires_at
) VALUES (
    '33333333-3333-3333-3333-333333333333', 0, now(), now(),
    '11111111-1111-1111-1111-111111111111', 'pk_live_seed00001', '$2a$10$seedHashPlaceholderDoNotUseInProd0000000000000000',
    'PRODUCTION', 'ACTIVE', 'Primary production key', NULL,
    NULL, NULL, NULL
) ON CONFLICT (id) DO NOTHING;
