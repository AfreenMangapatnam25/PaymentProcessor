-- ============================================================================
-- Sample data for local/dev testing (NOT run against prod — see
-- application.yml's `local` profile document, which points
-- spring.flyway.locations at classpath:db/seed in addition to db/migration).
--
-- Adds two sample chart-of-accounts entries (a customer wallet and a merchant
-- payable) plus a posted, balanced journal moving funds from the platform's
-- existing Cash - Settlement Account (1101, seeded in V2) into the sample
-- customer wallet. IDs below are fixed/deterministic so API_TESTING.md can
-- reference them directly in GET-by-id examples.
-- ============================================================================

-- Sample accounts --------------------------------------------------------------
INSERT INTO accounts (id, account_code, name, owner_type, owner_id, type_code, currency, status, created_at) VALUES
    ('ACC-CUST-0001', 'ACC-CUST-0001', 'Customer Wallet - Jane Doe',      'CUSTOMER', 'CUST-1001',  'LIABILITY', 'USD', 'ACTIVE', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('ACC-MERCH-0001', 'ACC-MERCH-0001', 'Merchant Payable - Acme Co',    'MERCHANT', 'MERCH-2001', 'LIABILITY', 'USD', 'ACTIVE', TIMESTAMPTZ '2026-01-15 10:00:00+00');

-- Zeroed balance rows for the new accounts (mirrors the pattern in V2) --------
INSERT INTO account_balances (account_id, currency, updated_at)
VALUES
    ('ACC-CUST-0001', 'USD', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('ACC-MERCH-0001', 'USD', TIMESTAMPTZ '2026-01-15 10:00:00+00');

-- Sample posted journal: $100.00 moved from platform cash into the sample
-- customer's wallet (debit Cash - Settlement Account 1101, credit the wallet).
INSERT INTO journals (id, event_type, external_ref, idempotency_key, description, status, period_id, effective_at, posted_at, created_by) VALUES
    ('JRNL-SEED-0001', 'SEED_DEPOSIT', 'SEED-REF-0001', 'seed-idempotency-0001', 'Seed sample customer deposit for local testing', 'POSTED', 'PERIOD-2026', TIMESTAMPTZ '2026-01-15 10:00:00+00', TIMESTAMPTZ '2026-01-15 10:00:00+00', 'seed-script');

INSERT INTO entries (journal_id, line_number, account_id, direction, amount_minor, currency, description, effective_at) VALUES
    ('JRNL-SEED-0001', 1, '1101',           'DEBIT',  10000, 'USD', 'Seed deposit source (platform cash)', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('JRNL-SEED-0001', 2, 'ACC-CUST-0001',  'CREDIT', 10000, 'USD', 'Seed deposit into customer wallet',   TIMESTAMPTZ '2026-01-15 10:00:00+00');

-- Reflect the posted journal in the denormalised real-time balances ----------
UPDATE account_balances
   SET posted_minor = posted_minor + 10000,
       available_minor = available_minor + 10000,
       entry_high_water = entry_high_water + 1,
       updated_at = TIMESTAMPTZ '2026-01-15 10:00:00+00'
 WHERE account_id = '1101';

UPDATE account_balances
   SET posted_minor = posted_minor + 10000,
       available_minor = available_minor + 10000,
       entry_high_water = entry_high_water + 1,
       updated_at = TIMESTAMPTZ '2026-01-15 10:00:00+00'
 WHERE account_id = 'ACC-CUST-0001';
