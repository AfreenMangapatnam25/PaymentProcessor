-- ============================================================================
-- Authentication Service - local/dev sample data
-- Only loaded when the "local" Spring profile is active (see application.yml,
-- spring.flyway.locations = classpath:db/migration,classpath:db/seed).
-- Fixed/deterministic ids so they can be referenced directly from API_TESTING.md.
-- NOTE: secret_hash / token_hash / code_hash values are placeholder strings shaped
-- like real Argon2id/SHA-256 output, not hashes of any real usable secret. You
-- cannot log in against these seeded identities out of the box; register a fresh
-- identity via POST /api/v1/admin/identities to exercise the login/MFA flow.
-- ============================================================================

INSERT INTO identities (
    id, principal_type, email, phone_e164, status, mfa_required,
    failed_login_count, locked_until, email_verified_at, phone_verified_at,
    created_at, updated_at
) VALUES
('11111111-1111-1111-1111-111111111111', 'ADMIN', 'admin@paymentprocessor.local', '+14155550001', 'ACTIVE', true,
    0, NULL, '2026-06-01T08:00:00Z', '2026-06-01T08:00:00Z', '2026-06-01T08:00:00Z', '2026-06-01T08:00:00Z'),
('11111111-1111-1111-1111-111111111112', 'USER', 'owner@merchant-demo.com', '+14155559012', 'ACTIVE', true,
    0, NULL, '2026-06-20T08:00:00Z', '2026-06-20T08:00:00Z', '2026-06-20T08:00:00Z', '2026-07-20T08:00:00Z'),
('11111111-1111-1111-1111-111111111113', 'USER', 'alice@example.com', '+14155551234', 'ACTIVE', false,
    0, NULL, '2026-06-25T09:00:00Z', NULL, '2026-06-25T09:00:00Z', '2026-06-25T09:00:00Z'),
('11111111-1111-1111-1111-111111111114', 'USER', 'locked.user@example.com', NULL, 'LOCKED', false,
    5, '2026-07-26T00:00:00Z', '2026-05-10T09:00:00Z', NULL, '2026-05-10T09:00:00Z', '2026-07-24T18:00:00Z'),
('11111111-1111-1111-1111-111111111115', 'USER', 'pending.user@example.com', NULL, 'PENDING', false,
    0, NULL, NULL, NULL, '2026-07-24T12:00:00Z', '2026-07-24T12:00:00Z');

INSERT INTO credentials (id, identity_id, kind, secret_hash, algo_params, rotated_at, expires_at, created_at) VALUES
('21111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'PASSWORD',
    '$argon2id$v=19$m=16384,t=3,p=1$c2VlZGFkbWluc2FsdA$c2VlZGFkbWluaGFzaHBsYWNlaG9sZGVy', 'argon2id', NULL, NULL, '2026-06-01T08:00:00Z'),
('21111111-1111-1111-1111-111111111112', '11111111-1111-1111-1111-111111111112', 'PASSWORD',
    '$argon2id$v=19$m=16384,t=3,p=1$c2VlZG93bmVyc2FsdA$c2VlZG93bmVyaGFzaHBsYWNlaG9sZGVy', 'argon2id', NULL, NULL, '2026-06-20T08:00:00Z'),
('21111111-1111-1111-1111-111111111113', '11111111-1111-1111-1111-111111111113', 'PASSWORD',
    '$argon2id$v=19$m=16384,t=3,p=1$c2VlZGFsaWNlc2FsdA$c2VlZGFsaWNlaGFzaHBsYWNlaG9sZGVy', 'argon2id', NULL, NULL, '2026-06-25T09:00:00Z'),
('21111111-1111-1111-1111-111111111114', '11111111-1111-1111-1111-111111111114', 'PASSWORD',
    '$argon2id$v=19$m=16384,t=3,p=1$c2VlZGxvY2tlZHNhbHQ$c2VlZGxvY2tlZGhhc2hwbGFjZWhvbGRlcg', 'argon2id', NULL, NULL, '2026-05-10T09:00:00Z'),
('21111111-1111-1111-1111-111111111115', '11111111-1111-1111-1111-111111111115', 'PASSWORD',
    '$argon2id$v=19$m=16384,t=3,p=1$c2VlZHBlbmRpbmdzYWx0$c2VlZHBlbmRpbmdoYXNocGxhY2Vob2xkZXI', 'argon2id', NULL, NULL, '2026-07-24T12:00:00Z');

INSERT INTO devices (id, identity_id, fingerprint, label, trust_level, last_ip, last_seen_at, created_at) VALUES
('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111112', 'fp_c8b1e2a3d4f5', 'Alice''s MacBook Pro', 'TRUSTED', '203.0.113.11', '2026-07-25T09:00:00Z', '2026-06-01T09:00:00Z'),
('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111112', 'fp_9a8b7c6d5e4f', 'Merchant Owner iPhone', 'RECOGNIZED', '203.0.113.12', '2026-07-24T20:00:00Z', '2026-06-15T09:00:00Z'),
('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111113', 'fp_1a2b3c4d5e6f', 'Alice Home Desktop', 'UNKNOWN', '203.0.113.13', '2026-07-20T09:00:00Z', '2026-06-25T09:10:00Z');

INSERT INTO mfa_factors (id, identity_id, kind, secret_ref, label, verified_at, status, created_at) VALUES
('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111112', 'TOTP', 'kms://mfa-secrets/33333333-3333-3333-3333-333333333331', 'Merchant Owner iPhone', '2026-06-20T09:00:00Z', 'ACTIVE', '2026-06-20T08:30:00Z'),
('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111111', 'TOTP', 'kms://mfa-secrets/33333333-3333-3333-3333-333333333332', 'Admin YubiKey Authenticator', '2026-06-01T08:30:00Z', 'ACTIVE', '2026-06-01T08:15:00Z');

INSERT INTO mfa_recovery_codes (id, identity_id, code_hash, used_at, created_at) VALUES
('34444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111112', 'a3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5', NULL, '2026-06-20T09:00:00Z'),
('34444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111112', 'b4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6', NULL, '2026-06-20T09:00:00Z');

INSERT INTO api_keys (id, owner_type, owner_id, prefix, secret_hash, environment, last_used_at, expires_at, revoked_at, created_at) VALUES
('44444444-4444-4444-4444-444444444401', 'SERVICE', 'fraud-service', 'pk_live_4444a4', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85', 'production', '2026-07-25T10:00:00Z', '2027-07-25T00:00:00Z', NULL, '2026-06-01T08:00:00Z'),
('44444444-4444-4444-4444-444444444402', 'SERVICE', 'authorization-service', 'pk_live_4444a5', 'f1a2b3c4d5e6f7089a0b1c2d3e4f50617283940516273849506172839405162', 'production', '2026-07-25T09:55:00Z', '2027-07-25T00:00:00Z', NULL, '2026-06-01T08:00:00Z'),
('44444444-4444-4444-4444-444444444403', 'MERCHANT', 'merch_7788', 'pk_test_4444a6', 'c5d6e7f8091a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2', 'sandbox', NULL, NULL, '2026-07-01T00:00:00Z', '2026-06-10T08:00:00Z');

INSERT INTO api_key_scopes (api_key_id, scope) VALUES
('44444444-4444-4444-4444-444444444401', 'fraud:read'),
('44444444-4444-4444-4444-444444444401', 'fraud:score'),
('44444444-4444-4444-4444-444444444402', 'authorization:read'),
('44444444-4444-4444-4444-444444444402', 'authorization:create'),
('44444444-4444-4444-4444-444444444403', 'merchant:read');

INSERT INTO login_attempts (identity_id, email, ip, user_agent, result, created_at) VALUES
('11111111-1111-1111-1111-111111111113', 'alice@example.com', '203.0.113.13', 'Mozilla/5.0', 'SUCCESS', '2026-07-25T09:00:00Z'),
('11111111-1111-1111-1111-111111111114', 'locked.user@example.com', '203.0.113.14', 'Mozilla/5.0', 'BAD_CREDENTIALS', '2026-07-24T17:55:00Z'),
('11111111-1111-1111-1111-111111111114', 'locked.user@example.com', '203.0.113.14', 'Mozilla/5.0', 'LOCKED', '2026-07-24T18:00:00Z');
