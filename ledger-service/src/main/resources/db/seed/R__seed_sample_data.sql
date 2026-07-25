-- ============================================================================
-- REPEATABLE seed migration (Flyway "R__" prefix).
--
-- Why repeatable rather than versioned:
--   * Repeatable migrations always run AFTER every versioned migration, so this
--     file can never be "out of order". A versioned seed applied only under the
--     local profile broke as soon as the service had already been started without
--     it - Flyway then refused with
--     "Detected resolved migration not applied to database: <n>".
--   * Flyway re-applies a repeatable migration whenever its checksum changes, so
--     editing the sample data below just works on the next start; no manual
--     flyway_schema_history surgery needed.
--
-- Every statement is therefore written to be IDEMPOTENT (ON CONFLICT DO NOTHING),
-- because this file runs again on every checksum change and must never fail with
-- a duplicate-key error.
--
-- Only loaded when the "local" Spring profile is active (see application.yml:
-- spring.flyway.locations = classpath:db/migration,classpath:db/seed).
-- ============================================================================

-- Sample accounts --------------------------------------------------------------
INSERT INTO accounts (id, account_code, name, owner_type, owner_id, type_code, currency, status, created_at) VALUES
    ('ACC-CUST-0001', 'ACC-CUST-0001', 'Customer Wallet - Jane Doe',      'CUSTOMER', 'CUST-1001',  'LIABILITY', 'USD', 'ACTIVE', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('ACC-MERCH-0001', 'ACC-MERCH-0001', 'Merchant Payable - Acme Co',    'MERCHANT', 'MERCH-2001', 'LIABILITY', 'USD', 'ACTIVE', TIMESTAMPTZ '2026-01-15 10:00:00+00')
ON CONFLICT DO NOTHING;

-- Zeroed balance rows for the new accounts (mirrors the pattern in V2) --------
INSERT INTO account_balances (account_id, currency, updated_at)
VALUES
    ('ACC-CUST-0001', 'USD', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('ACC-MERCH-0001', 'USD', TIMESTAMPTZ '2026-01-15 10:00:00+00')
ON CONFLICT DO NOTHING;

-- Sample posted journal: $100.00 moved from platform cash into the sample
-- customer's wallet (debit Cash - Settlement Account 1101, credit the wallet).
INSERT INTO journals (id, event_type, external_ref, idempotency_key, description, status, period_id, effective_at, posted_at, created_by) VALUES
    ('JRNL-SEED-0001', 'SEED_DEPOSIT', 'SEED-REF-0001', 'seed-idempotency-0001', 'Seed sample customer deposit for local testing', 'POSTED', 'PERIOD-2026', TIMESTAMPTZ '2026-01-15 10:00:00+00', TIMESTAMPTZ '2026-01-15 10:00:00+00', 'seed-script')
ON CONFLICT DO NOTHING;

INSERT INTO entries (journal_id, line_number, account_id, direction, amount_minor, currency, description, effective_at) VALUES
    ('JRNL-SEED-0001', 1, '1101',           'DEBIT',  10000, 'USD', 'Seed deposit source (platform cash)', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('JRNL-SEED-0001', 2, 'ACC-CUST-0001',  'CREDIT', 10000, 'USD', 'Seed deposit into customer wallet',   TIMESTAMPTZ '2026-01-15 10:00:00+00')
ON CONFLICT DO NOTHING;

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
