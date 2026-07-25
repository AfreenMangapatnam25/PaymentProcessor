-- =============================================================================
-- Reconciliation Service - sample seed data (local profile only)
-- Applied only when spring.profiles.active=local (see application.yml, which
-- adds classpath:db/seed to spring.flyway.locations for that profile).
-- Uses fixed, deterministic ids so API_TESTING.md examples are directly
-- testable against a freshly-seeded local database.
-- =============================================================================

INSERT INTO recon_run (
    id, uuid, recon_type, channel, account_ref, business_date, currency, status,
    started_at, completed_at, total_internal, total_external, matched_count,
    mismatched_count, missing_internal_count, missing_external_count, duplicate_count,
    exception_count, matched_amount, match_rate, triggered_by, created_at, updated_at, version
) VALUES (
    1, '11111111-1111-1111-1111-111111111111', 'ACQUIRER', 'VISA_ACQUIRING', 'ACC-100200',
    DATE '2026-07-24', 'USD', 'COMPLETED',
    TIMESTAMPTZ '2026-07-24 02:00:00+00', TIMESTAMPTZ '2026-07-24 02:03:00+00',
    1, 1, 1, 0, 0, 0, 0, 0, 250.0000, 1.0000, 'seed-script',
    TIMESTAMPTZ '2026-07-24 02:00:00+00', TIMESTAMPTZ '2026-07-24 02:03:00+00', 0
);

SELECT setval(pg_get_serial_sequence('recon_run', 'id'), (SELECT MAX(id) FROM recon_run));

INSERT INTO recon_record (
    id, recon_run_id, source, source_system, external_reference, arn, internal_payment_id,
    amount, fee_amount, currency, transaction_date, value_date, merchant_id, counterparty,
    card_bin, card_last4, transaction_status, match_status, raw_payload, created_at, updated_at, version
) VALUES
(
    1, 1, 'INTERNAL', 'payment-service', 'TXN-1001', NULL, 'PAY-1001',
    250.0000, 5.0000, 'USD', DATE '2026-07-24', DATE '2026-07-24', 'MERCH-1001', 'Acme Corp',
    '411111', '1111', 'CAPTURED', 'MATCHED', NULL,
    TIMESTAMPTZ '2026-07-24 02:00:00+00', TIMESTAMPTZ '2026-07-24 02:03:00+00', 0
),
(
    2, 1, 'EXTERNAL', 'acquirer-visa', 'TXN-1001', 'ARN-99001122', NULL,
    250.0000, 5.0000, 'USD', DATE '2026-07-24', DATE '2026-07-24', 'MERCH-1001', 'Acme Corp',
    '411111', '1111', 'SETTLED', 'MATCHED', NULL,
    TIMESTAMPTZ '2026-07-24 02:00:00+00', TIMESTAMPTZ '2026-07-24 02:03:00+00', 0
);

SELECT setval(pg_get_serial_sequence('recon_record', 'id'), (SELECT MAX(id) FROM recon_record));

INSERT INTO recon_match (
    id, recon_run_id, internal_record_id, external_record_id, match_type, match_rule,
    confidence, amount_variance, date_variance_days, note, manual, matched_at,
    created_at, updated_at, version
) VALUES (
    1, 1, 1, 2, 'EXACT', 'AmountAndReferenceRule', 1.0000, 0.0000, 0, 'Seed sample match', FALSE,
    TIMESTAMPTZ '2026-07-24 02:03:00+00', TIMESTAMPTZ '2026-07-24 02:03:00+00',
    TIMESTAMPTZ '2026-07-24 02:03:00+00', 0
);

SELECT setval(pg_get_serial_sequence('recon_match', 'id'), (SELECT MAX(id) FROM recon_match));
