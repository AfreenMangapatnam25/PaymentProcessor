-- ============================================================================
-- Sample data for local/dev testing (NOT run against prod — see
-- application.yml's `local` profile document, which points
-- spring.flyway.locations at classpath:db/seed in addition to db/migration).
--
-- Adds two sample LimitConfiguration rows (a customer-specific override and a
-- merchant-specific soft cap) plus a UsageCounter reflecting some already-
-- reserved usage against the customer config. IDs below are fixed/deterministic
-- so API_TESTING.md can reference them directly in GET-by-id examples.
-- ============================================================================

INSERT INTO limit_configuration
    (id, name, scope, scope_id, dimension, time_window, threshold, currency, enforcement, priority, active, time_zone, record_version, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Sample customer daily amount override', 'CUSTOMER', 'CUST-1001',  'AMOUNT', 'DAILY', 5000.0000, 'USD', 'HARD', 50, TRUE, 'UTC', 0, TIMESTAMPTZ '2026-01-15 10:00:00+00', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('a0000000-0000-0000-0000-000000000002', 'Sample merchant daily transaction count cap', 'MERCHANT', 'MERCH-2001', 'COUNT', 'DAILY', 100.0000, NULL, 'SOFT', 50, TRUE, 'UTC', 0, TIMESTAMPTZ '2026-01-15 10:00:00+00', TIMESTAMPTZ '2026-01-15 10:00:00+00');

-- Usage counter showing $150.00 already reserved today against the sample
-- customer's daily amount override (window_key = "<scopeId>:<yyyy-MM-dd>").
INSERT INTO usage_counter
    (id, limit_config_id, window_key, reserved_amount, committed_amount, reserved_count, committed_count, window_start, record_version, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'CUST-1001:2026-01-15', 150.0000, 0.0000, 1, 0, TIMESTAMPTZ '2026-01-15 00:00:00+00', 0, TIMESTAMPTZ '2026-01-15 10:00:00+00');
