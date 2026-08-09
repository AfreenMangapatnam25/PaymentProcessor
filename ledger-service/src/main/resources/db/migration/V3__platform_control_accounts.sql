-- Platform control accounts used by settlement-service and dispute-service.
-- Idempotent: ON CONFLICT DO NOTHING so re-runs are safe.

INSERT INTO accounts (id, account_code, name, type_code, currency, owner_type, owner_id, parent_account_id, status, created_at)
VALUES
    ('platform:cash', 'platform.cash', 'Platform Cash Control', 'ASSET', 'USD', 'PLATFORM', 'platform', '1101', 'ACTIVE', now()),
    ('platform:fee_revenue', 'platform.fee_revenue', 'Platform Fee Revenue Control', 'REVENUE', 'USD', 'PLATFORM', 'platform', '4101', 'ACTIVE', now()),
    ('platform:payout_payable', 'platform.payout_payable', 'Platform Payout Payable Control', 'LIABILITY', 'USD', 'PLATFORM', 'platform', '2102', 'ACTIVE', now()),
    ('platform:adjustment_expense', 'platform.adjustment_expense', 'Platform Adjustment Expense Control', 'EXPENSE', 'USD', 'PLATFORM', 'platform', '5101', 'ACTIVE', now()),
    ('platform:chargeback_clearing', 'platform.chargeback_clearing', 'Platform Chargeback Clearing Control', 'LIABILITY', 'USD', 'PLATFORM', 'platform', '2102', 'ACTIVE', now()),
    ('platform:fee_expense', 'platform.fee_expense', 'Platform Fee Expense Control', 'EXPENSE', 'USD', 'PLATFORM', 'platform', '5101', 'ACTIVE', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO account_balances (account_id, currency, updated_at)
SELECT id, currency, now() FROM accounts
WHERE id IN (
    'platform:cash', 'platform:fee_revenue', 'platform:payout_payable',
    'platform:adjustment_expense', 'platform:chargeback_clearing', 'platform:fee_expense'
)
ON CONFLICT (account_id) DO NOTHING;

-- Dev/sample merchant ledger accounts (idempotent) for cross-service local testing.
INSERT INTO accounts (id, account_code, name, type_code, currency, owner_type, owner_id, parent_account_id, status, created_at)
VALUES
    ('merchant:MERCH-1001:settlement_liability', 'merchant.MERCH-1001.settlement_liability', 'Merchant MERCH-1001 settlement liability', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-1001', '2102', 'ACTIVE', now()),
    ('merchant:MERCH-1001:reserve', 'merchant.MERCH-1001.reserve', 'Merchant MERCH-1001 reserve', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-1001', '1104', 'ACTIVE', now()),
    ('merchant:MERCH-SEED-01:settlement_liability', 'merchant.MERCH-SEED-01.settlement_liability', 'Merchant MERCH-SEED-01 settlement liability', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-SEED-01', '2102', 'ACTIVE', now()),
    ('merchant:MERCH-SEED-01:reserve', 'merchant.MERCH-SEED-01.reserve', 'Merchant MERCH-SEED-01 reserve', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-SEED-01', '1104', 'ACTIVE', now()),
    ('merchant:MERCH-SEED-02:settlement_liability', 'merchant.MERCH-SEED-02.settlement_liability', 'Merchant MERCH-SEED-02 settlement liability', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-SEED-02', '2102', 'ACTIVE', now()),
    ('merchant:MERCH-SEED-02:reserve', 'merchant.MERCH-SEED-02.reserve', 'Merchant MERCH-SEED-02 reserve', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-SEED-02', '1104', 'ACTIVE', now()),
    ('merchant:MERCH-SEED-03:settlement_liability', 'merchant.MERCH-SEED-03.settlement_liability', 'Merchant MERCH-SEED-03 settlement liability', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-SEED-03', '2102', 'ACTIVE', now()),
    ('merchant:MERCH-SEED-03:reserve', 'merchant.MERCH-SEED-03.reserve', 'Merchant MERCH-SEED-03 reserve', 'LIABILITY', 'USD', 'MERCHANT', 'MERCH-SEED-03', '1104', 'ACTIVE', now()),
    ('merchant:MER-0001SEED:settlement_liability', 'merchant.MER-0001SEED.settlement_liability', 'Merchant MER-0001SEED settlement liability', 'LIABILITY', 'USD', 'MERCHANT', 'MER-0001SEED', '2102', 'ACTIVE', now()),
    ('merchant:MER-0001SEED:reserve', 'merchant.MER-0001SEED.reserve', 'Merchant MER-0001SEED reserve', 'LIABILITY', 'USD', 'MERCHANT', 'MER-0001SEED', '1104', 'ACTIVE', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO account_balances (account_id, currency, updated_at)
SELECT id, currency, now() FROM accounts
WHERE id LIKE 'merchant:%'
ON CONFLICT (account_id) DO NOTHING;
