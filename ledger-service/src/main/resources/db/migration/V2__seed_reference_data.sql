-- ============================================================================
-- Seed reference data: currencies, account types, chart of accounts, period.
-- ============================================================================

INSERT INTO currencies (code, exponent, name) VALUES
    ('USD', 2, 'US Dollar'),
    ('EUR', 2, 'Euro'),
    ('GBP', 2, 'Pound Sterling');

INSERT INTO account_types (code, classification, normal_balance, description) VALUES
    ('ASSET',     'ASSET',     'DEBIT',  'Resources owned by the platform'),
    ('LIABILITY', 'LIABILITY', 'CREDIT', 'Obligations owed by the platform'),
    ('EQUITY',    'EQUITY',    'CREDIT', 'Residual interest in the assets'),
    ('REVENUE',   'REVENUE',   'CREDIT', 'Income earned'),
    ('EXPENSE',   'EXPENSE',   'DEBIT',  'Costs incurred');

-- Chart of accounts (README §Chart of Accounts). id == account_code for GL accounts.
INSERT INTO accounts (id, account_code, name, type_code, currency, status, created_at) VALUES
    ('1101', '1101', 'Cash - Settlement Account',        'ASSET',     'USD', 'ACTIVE', now()),
    ('1102', '1102', 'Cash - Operating Account',         'ASSET',     'USD', 'ACTIVE', now()),
    ('1103', '1103', 'Accounts Receivable - Merchants',  'ASSET',     'USD', 'ACTIVE', now()),
    ('1104', '1104', 'Reserve Assets',                   'ASSET',     'USD', 'ACTIVE', now()),
    ('2101', '2101', 'Customer Deposits Payable',        'LIABILITY', 'USD', 'ACTIVE', now()),
    ('2102', '2102', 'Merchant Settlement Payable',      'LIABILITY', 'USD', 'ACTIVE', now()),
    ('2103', '2103', 'Tax Payable',                      'LIABILITY', 'USD', 'ACTIVE', now()),
    ('3101', '3101', 'Share Capital',                    'EQUITY',    'USD', 'ACTIVE', now()),
    ('3201', '3201', 'Retained Earnings',                'EQUITY',    'USD', 'ACTIVE', now()),
    ('4101', '4101', 'Merchant Transaction Fees',        'REVENUE',   'USD', 'ACTIVE', now()),
    ('4102', '4102', 'Interchange Revenue',              'REVENUE',   'USD', 'ACTIVE', now()),
    ('4103', '4103', 'Subscription Revenue',             'REVENUE',   'USD', 'ACTIVE', now()),
    ('5101', '5101', 'Payment Processing Costs',         'EXPENSE',   'USD', 'ACTIVE', now()),
    ('5102', '5102', 'Chargeback Losses',                'EXPENSE',   'USD', 'ACTIVE', now()),
    ('5103', '5103', 'Refund Costs',                     'EXPENSE',   'USD', 'ACTIVE', now()),
    ('5104', '5104', 'Fraud Losses',                     'EXPENSE',   'USD', 'ACTIVE', now());

-- Initialise zeroed balances for each seeded account.
INSERT INTO account_balances (account_id, currency, updated_at)
SELECT id, currency, now() FROM accounts;

-- A genesis accounting period (open). Operators create subsequent periods via the API.
INSERT INTO accounting_periods (id, code, period_type, start_date, end_date, state, created_at) VALUES
    ('PERIOD-2026', 'FY2026', 'YEARLY', DATE '2026-01-01', DATE '2026-12-31', 'OPEN', now());
