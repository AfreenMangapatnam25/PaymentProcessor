-- ==========================================================================
-- Settlement Service — dev-profile seed data
--
-- Spring Boot auto-runs this against whatever datasource is active. It is
-- only meaningful for the 'dev' profile (H2 in-memory, MODE=PostgreSQL,
-- ddl-auto=update, flyway disabled) because that's the only profile with
-- spring.sql.init.mode=always set (see application.yml). The 'prod' profile
-- uses real Postgres + Flyway and never sets spring.sql.init.mode, so this
-- file is a no-op there.
--
-- Uses fixed, deterministic ids so API_TESTING.md examples are directly
-- testable against a freshly-started dev instance.
-- ==========================================================================

INSERT INTO settlement_batches (
    id, merchant_id, currency, schedule_type, period_start, period_end, status,
    gross_minor, refunds_minor, fees_minor, interchange_minor, chargebacks_minor,
    adjustments_minor, reserve_minor, settlement_fee_minor, net_minor,
    ledger_journal_id, approved_by, approved_at, closed_at, failure_reason,
    created_at, updated_at, version
) VALUES (
    'batch-seed-0001', 'MERCH-1001', 'USD', 'DAILY',
    TIMESTAMP '2026-07-24 00:00:00', TIMESTAMP '2026-07-24 23:59:59', 'COMPLETED',
    500000, 10000, 15000, 8000, 0,
    0, 25000, 100, 441900,
    'LJ-SEED-0001', 'ops-analyst-jane', TIMESTAMP '2026-07-25 01:00:00', TIMESTAMP '2026-07-25 02:00:00', NULL,
    TIMESTAMP '2026-07-25 00:30:00', TIMESTAMP '2026-07-25 02:00:00', 0
);

INSERT INTO payouts (
    id, batch_id, merchant_id, payout_account_id, amount_minor, currency, rail, status,
    provider_ref, idempotency_key, ledger_journal_id, attempt_count, next_retry_at,
    failure_code, failure_reason, failure_category, scheduled_at, submitted_at, paid_at,
    created_at, updated_at, version
) VALUES (
    'payout-seed-0001', 'batch-seed-0001', 'MERCH-1001', 'PAYACC-1001', 441900, 'USD', 'ACH', 'COMPLETED',
    'PR-SEED-0001', 'seed-payout-0001', 'LJ-SEED-0002', 1, NULL,
    NULL, NULL, NULL, TIMESTAMP '2026-07-25 01:05:00', TIMESTAMP '2026-07-25 01:10:00', TIMESTAMP '2026-07-25 02:00:00',
    TIMESTAMP '2026-07-25 01:00:00', TIMESTAMP '2026-07-25 02:00:00', 0
);

INSERT INTO reserves (
    id, merchant_id, kind, rate_bps, amount_minor, released_minor, currency,
    source_batch_id, hold_until, status, ledger_hold_id, released_at,
    created_at, updated_at, version
) VALUES (
    'reserve-seed-0001', 'MERCH-1001', 'ROLLING', 500, 25000, 0, 'USD',
    'batch-seed-0001', DATE '2026-10-23', 'HELD', 'LH-SEED-0001', NULL,
    TIMESTAMP '2026-07-25 01:00:00', TIMESTAMP '2026-07-25 01:00:00', 0
);
