-- ============================================================================
-- Audit Service - local/dev sample data
-- Only loaded when the "local" Spring profile is active (see application.yml,
-- spring.flyway.locations = classpath:db/migration,classpath:db/seed).
-- Fixed/deterministic ids so they can be referenced directly from API_TESTING.md.
-- NOTE: hash / prevHash values below are illustrative placeholders, not real
-- SHA-256 digests, so GET /api/v1/audit/verify will report the seeded range as
-- invalid. This is expected for hand-authored seed rows; append new records via
-- the API to exercise the real hash chain.
-- ============================================================================

INSERT INTO audit_records (
    id, seq, ts, recorded_at, actor_type, actor_id, actor_ip, actor_ua,
    action, resource_type, resource_id, merchant_id, before, after,
    request_id, trace_id, event_id, prev_hash, hash, batch_id
) VALUES
(
    'aud_01J9ZQKR000000000000000001', 1,
    '2026-07-24T09:00:00Z', '2026-07-24T09:00:00.100Z',
    'USER', 'id_5a1c0e8e-2222-4b11-9a10-abcdef012345', '203.0.113.10', 'curl/8.4.0',
    'LOGIN_SUCCEEDED', 'identity', 'id_5a1c0e8e-2222-4b11-9a10-abcdef012345', 'merch_7788',
    NULL, NULL,
    'req_seed_0001', 'trace_seed_0001', 'evt_seed_0001',
    'sha256:0000000000000000000000000000000000000000000000000000000000000000',
    'sha256:aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111',
    'batch_2026-07-24'
),
(
    'aud_01J9ZQKR000000000000000002', 2,
    '2026-07-24T09:05:00Z', '2026-07-24T09:05:00.100Z',
    'USER', 'id_5a1c0e8e-2222-4b11-9a10-abcdef012345', '203.0.113.10', 'curl/8.4.0',
    'MERCHANT_CREATED', 'merchant', 'merch_7788', 'merch_7788',
    NULL, '{"status": "PENDING_REVIEW"}'::jsonb,
    'req_seed_0002', 'trace_seed_0002', 'evt_seed_0002',
    'sha256:aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111',
    'sha256:bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222',
    'batch_2026-07-24'
),
(
    'aud_01J9ZQKR000000000000000003', 3,
    '2026-07-24T10:15:00Z', '2026-07-24T10:15:00.100Z',
    'SERVICE', 'svc_payment-service', '10.0.4.21', 'payment-service/1.0',
    'AUTHORIZATION_APPROVED', 'authorization', '11111111-1111-1111-1111-111111111111', 'merch_7788',
    NULL, '{"status": "APPROVED", "amount": 250.00}'::jsonb,
    'req_seed_0003', 'trace_seed_0003', 'evt_seed_0003',
    'sha256:bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222',
    'sha256:cccc3333cccc3333cccc3333cccc3333cccc3333cccc3333cccc3333cccc3333',
    'batch_2026-07-24'
),
(
    'aud_01J9ZQKR000000000000000004', 4,
    '2026-07-24T11:30:00Z', '2026-07-24T11:30:00.100Z',
    'USER', 'id_5a1c0e8e-2222-4b11-9a10-abcdef012345', '203.0.113.10', 'curl/8.4.0',
    'MERCHANT_UPDATED', 'merchant', 'merch_7788', 'merch_7788',
    '{"status": "PENDING_REVIEW"}'::jsonb, '{"status": "ACTIVE"}'::jsonb,
    'req_seed_0004', 'trace_seed_0004', 'evt_seed_0004',
    'sha256:cccc3333cccc3333cccc3333cccc3333cccc3333cccc3333cccc3333cccc3333',
    'sha256:dddd4444dddd4444dddd4444dddd4444dddd4444dddd4444dddd4444dddd4444',
    'batch_2026-07-24'
),
(
    'aud_01J9ZQKR000000000000000005', 5,
    '2026-07-24T18:00:00Z', '2026-07-24T18:00:00.100Z',
    'ADMIN', 'id_admin-0000-0000-0000-000000000001', '198.51.100.5', 'Mozilla/5.0',
    'IDENTITY_LOCKED', 'identity', '11111111-1111-1111-1111-111111111114', NULL,
    '{"status": "ACTIVE"}'::jsonb, '{"status": "LOCKED"}'::jsonb,
    'req_seed_0005', 'trace_seed_0005', 'evt_seed_0005',
    'sha256:dddd4444dddd4444dddd4444dddd4444dddd4444dddd4444dddd4444dddd4444',
    'sha256:eeee5555eeee5555eeee5555eeee5555eeee5555eeee5555eeee5555eeee5555',
    'batch_2026-07-24'
);

-- Sealed daily batch covering seq 1-5, all dated 2026-07-24 (UTC).
INSERT INTO audit_batches (
    id, batch_date, from_seq, to_seq, record_count, root_hash, signature,
    signing_key_id, s3_bucket, s3_key, s3_version_id, retain_until, anchor_ref,
    status, created_at, sealed_at
) VALUES (
    'batch_2026-07-24', '2026-07-24', 1, 5, 5,
    'sha256:deadbeef00000000000000000000000000000000000000000000000000000',
    'MEUCIQDx0000000000000000000000000000000000000000000000000000000AiEA00==',
    'audit-batch-signer-v1',
    'payment-processor-audit-legal',
    'audit-batches/2026/07/24/batch_2026-07-24.json',
    '3sL4kqtJlcpXroDTDmJ+rmSpXd3dIbrHY+MTRCxf3vjVBH40Nrjfkd',
    '2036-07-24T00:30:00Z', 'log:2026-07-24T00:30:05Z',
    'SEALED', '2026-07-24T00:30:00Z', '2026-07-24T00:30:05Z'
);

-- Chain head reflects the last seeded record so subsequent API-appended records
-- chain on cleanly (though their hash will not match the illustrative seed hashes above).
INSERT INTO audit_chain_state (id, seq, head_hash, updated_at)
VALUES (
    'GLOBAL', 5,
    'sha256:eeee5555eeee5555eeee5555eeee5555eeee5555eeee5555eeee5555eeee5555',
    '2026-07-24T18:00:00.100Z'
);
