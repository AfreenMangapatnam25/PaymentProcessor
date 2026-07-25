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

INSERT INTO limit_configuration
    (id, name, scope, scope_id, dimension, time_window, threshold, currency, enforcement, priority, active, time_zone, record_version, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Sample customer daily amount override', 'CUSTOMER', 'CUST-1001',  'AMOUNT', 'DAILY', 5000.0000, 'USD', 'HARD', 50, TRUE, 'UTC', 0, TIMESTAMPTZ '2026-01-15 10:00:00+00', TIMESTAMPTZ '2026-01-15 10:00:00+00'),
    ('a0000000-0000-0000-0000-000000000002', 'Sample merchant daily transaction count cap', 'MERCHANT', 'MERCH-2001', 'COUNT', 'DAILY', 100.0000, NULL, 'SOFT', 50, TRUE, 'UTC', 0, TIMESTAMPTZ '2026-01-15 10:00:00+00', TIMESTAMPTZ '2026-01-15 10:00:00+00')
ON CONFLICT DO NOTHING;

-- Usage counter showing $150.00 already reserved today against the sample
-- customer's daily amount override (window_key = "<scopeId>:<yyyy-MM-dd>").
INSERT INTO usage_counter
    (id, limit_config_id, window_key, reserved_amount, committed_amount, reserved_count, committed_count, window_start, record_version, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'CUST-1001:2026-01-15', 150.0000, 0.0000, 1, 0, TIMESTAMPTZ '2026-01-15 00:00:00+00', 0, TIMESTAMPTZ '2026-01-15 10:00:00+00')
ON CONFLICT DO NOTHING;
