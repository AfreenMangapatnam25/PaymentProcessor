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

INSERT INTO report_job (
    id, merchant_id, report_type, format, status, parameters, requested_by, notify_email,
    storage_key, row_count, size_bytes, error_message, scheduled_report_id,
    created_at, started_at, completed_at, expires_at, version
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MERCH-1001', 'SETTLEMENT_SUMMARY', 'PDF', 'COMPLETED',
    '{"fromDate":"2026-07-01","toDate":"2026-07-24"}'::jsonb, 'ops-analyst-jane',
    'jane@merchant1001.com', 'reports/MERCH-1001/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.pdf',
    128, 45678, NULL, NULL,
    TIMESTAMPTZ '2026-07-24 08:00:00+00', TIMESTAMPTZ '2026-07-24 08:00:05+00',
    TIMESTAMPTZ '2026-07-24 08:00:20+00', TIMESTAMPTZ '2026-08-23 08:00:20+00', 0
)
ON CONFLICT DO NOTHING;
