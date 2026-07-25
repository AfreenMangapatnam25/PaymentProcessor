-- =============================================================================
-- Analytics (reporting) Service - sample seed data (local profile only)
-- Applied only when spring.profiles.active=local (see application.yml, which
-- adds classpath:db/seed to spring.flyway.locations for that profile).
-- Uses a fixed, deterministic UUID so API_TESTING.md examples are directly
-- testable against a freshly-seeded local database.
-- =============================================================================

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
);
