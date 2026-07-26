-- ============================================================================
-- Default platform limits. scope_id NULL => applies to every entity in the scope
-- unless a more specific configuration overrides it. Values mirror the README
-- worked examples and the `limit.*` application defaults.
-- ============================================================================

INSERT INTO limit_configuration
    (id, name, scope, scope_id, dimension, time_window, threshold, currency, enforcement, priority, active, time_zone, record_version, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Platform per-transaction amount cap', 'GLOBAL', NULL, 'AMOUNT', 'PER_TRANSACTION', 50000.0000, NULL, 'HARD', 100, TRUE, 'UTC', 0, now(), now()),
    (gen_random_uuid(), 'Customer default daily amount',       'CUSTOMER', NULL, 'AMOUNT', 'DAILY',   10000.0000, NULL, 'HARD', 40, TRUE, 'UTC', 0, now(), now()),
    (gen_random_uuid(), 'Customer default daily count',        'CUSTOMER', NULL, 'COUNT',  'DAILY',      20.0000, NULL, 'HARD', 40, TRUE, 'UTC', 0, now(), now()),
    (gen_random_uuid(), 'Customer default monthly amount',     'CUSTOMER', NULL, 'AMOUNT', 'MONTHLY', 100000.0000, NULL, 'HARD', 40, TRUE, 'UTC', 0, now(), now()),
    (gen_random_uuid(), 'Merchant default daily volume',       'MERCHANT', NULL, 'AMOUNT', 'DAILY',  500000.0000, NULL, 'HARD', 30, TRUE, 'UTC', 0, now(), now());
