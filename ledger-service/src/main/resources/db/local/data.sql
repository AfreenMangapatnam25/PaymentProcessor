-- Local (H2) seed. Schema is created by Hibernate (ddl-auto=create-drop);
-- this file loads the same reference data as the Flyway prod seed.

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

INSERT INTO accounts (id, account_code, name, type_code, currency, status, created_at) VALUES
    ('1101', '1101', 'Cash - Settlement Account',        'ASSET',     'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('1102', '1102', 'Cash - Operating Account',         'ASSET',     'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('1103', '1103', 'Accounts Receivable - Merchants',  'ASSET',     'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('1104', '1104', 'Reserve Assets',                   'ASSET',     'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('2101', '2101', 'Customer Deposits Payable',        'LIABILITY', 'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('2102', '2102', 'Merchant Settlement Payable',      'LIABILITY', 'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('2103', '2103', 'Tax Payable',                      'LIABILITY', 'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('3101', '3101', 'Share Capital',                    'EQUITY',    'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('3201', '3201', 'Retained Earnings',                'EQUITY',    'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('4101', '4101', 'Merchant Transaction Fees',        'REVENUE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('4102', '4102', 'Interchange Revenue',              'REVENUE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('4103', '4103', 'Subscription Revenue',             'REVENUE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('5101', '5101', 'Payment Processing Costs',         'EXPENSE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('5102', '5102', 'Chargeback Losses',                'EXPENSE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('5103', '5103', 'Refund Costs',                     'EXPENSE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP),
    ('5104', '5104', 'Fraud Losses',                     'EXPENSE',   'USD', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO account_balances
    (account_id, currency, posted_minor, pending_minor, held_minor, available_minor, entry_high_water, version, updated_at)
SELECT id, currency, 0, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP FROM accounts;

INSERT INTO accounting_periods (id, code, period_type, start_date, end_date, state, created_at) VALUES
    ('PERIOD-2026', 'FY2026', 'YEARLY', DATE '2026-01-01', DATE '2026-12-31', 'OPEN', CURRENT_TIMESTAMP);
