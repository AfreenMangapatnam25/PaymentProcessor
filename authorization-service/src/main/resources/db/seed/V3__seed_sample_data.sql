-- ============================================================================
-- Authorization Service - local/dev sample data
-- Only loaded when the "local" Spring profile is active (see application.yml,
-- spring.flyway.locations = classpath:db/migration,classpath:db/seed under the
-- "local" profile block -- spring.profiles.default: local means this is the
-- effective default profile when SPRING_PROFILES_ACTIVE is unset).
-- Fixed/deterministic ids so they can be referenced directly from API_TESTING.md.
-- Role assignments reference roles seeded by V2__seed_rbac.sql by name, since
-- those rows use gen_random_uuid() and have no fixed ids.
-- ============================================================================

INSERT INTO authorization_record (
    id, status, type, merchant_id, customer_id, payment_reference,
    requested_amount, approved_amount, captured_amount, currency,
    card_network, card_bin, card_last4, card_exp_month, card_exp_year,
    authorization_code, network_reference_id, gateway_provider, gateway_authorization_id,
    gateway_response_code, gateway_response_message, response_code, avs_result, cvv_result,
    requires_authentication, authentication_url, original_authorization_id, reauthorization_count,
    idempotency_key, risk_score, gateway_raw_response,
    expires_at, authorized_at, captured_at, reversed_at, version, created_at, updated_at
) VALUES
(
    '11111111-1111-1111-1111-111111111111', 'APPROVED', 'INITIAL', 'merch_7788', 'cust_4455', 'pay_seed_0001',
    250.00, 250.00, NULL, 'USD',
    'VISA', '424242', '4242', 12, 2029,
    'OK7788', '24012345678901234567890', 'stripe', 'pi_seed_0001',
    '00', 'Approved', '00', 'FULL_MATCH', 'MATCH',
    false, NULL, NULL, 0,
    'idem_seed_0001', 0.12, '{"id":"pi_seed_0001","status":"succeeded"}',
    '2026-08-01T09:00:00Z', '2026-07-24T09:00:00Z', NULL, NULL, 0, '2026-07-24T09:00:00Z', '2026-07-24T09:00:00Z'
),
(
    '11111111-1111-1111-1111-111111111112', 'CAPTURED', 'INITIAL', 'merch_7788', 'cust_4455', 'pay_seed_0002',
    100.00, 100.00, 100.00, 'USD',
    'MASTERCARD', '555555', '4444', 6, 2028,
    'OK7789', '24012345678901234567891', 'stripe', 'pi_seed_0002',
    '00', 'Approved', '00', 'FULL_MATCH', 'MATCH',
    false, NULL, NULL, 0,
    'idem_seed_0002', 0.05, '{"id":"pi_seed_0002","status":"succeeded"}',
    '2026-08-01T10:00:00Z', '2026-07-24T10:00:00Z', '2026-07-24T12:00:00Z', NULL, 0, '2026-07-24T10:00:00Z', '2026-07-24T12:00:00Z'
),
(
    '11111111-1111-1111-1111-111111111113', 'DECLINED', 'INITIAL', 'merch_7788', 'cust_9012', 'pay_seed_0003',
    500.00, NULL, NULL, 'USD',
    'VISA', '400000', '0002', 3, 2027,
    NULL, NULL, 'stripe', 'pi_seed_0003',
    '05', 'Do not honor', '05', 'NO_MATCH', 'NO_MATCH',
    false, NULL, NULL, 0,
    'idem_seed_0003', 0.81, '{"id":"pi_seed_0003","status":"failed"}',
    '2026-08-01T11:00:00Z', NULL, NULL, NULL, 0, '2026-07-24T11:00:00Z', '2026-07-24T11:00:00Z'
);

INSERT INTO idempotency_key (key_value, request_hash, resource_type, resource_id, created_at, expires_at) VALUES
('idem_seed_0001', 'a3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5e7c9b1d3f5', 'authorization', '11111111-1111-1111-1111-111111111111', '2026-07-24T09:00:00Z', '2026-07-25T09:00:00Z'),
('idem_seed_0002', 'b4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6f8d0c2e4a6', 'authorization', '11111111-1111-1111-1111-111111111112', '2026-07-24T10:00:00Z', '2026-07-25T10:00:00Z'),
('idem_seed_0003', 'c5d6e7f8091a2b3c4d5e6f7089a0b1c2d3e4f5061728394a5b6c7d8e9f0a1b2', 'authorization', '11111111-1111-1111-1111-111111111113', '2026-07-24T11:00:00Z', '2026-07-25T11:00:00Z');

-- Role assignments, referencing roles seeded by name in V2__seed_rbac.sql.
INSERT INTO role_assignment (id, identity_id, role_id, scope, scope_id, valid_from, valid_until, created_at)
SELECT '66666666-6666-6666-6666-666666666601', '11111111-1111-1111-1111-111111111112', r.id, 'MERCHANT', 'merch_7788', '2026-06-20T00:00:00Z', NULL, '2026-06-20T00:00:00Z'
FROM role r WHERE r.name = 'MERCHANT_ADMIN';

INSERT INTO role_assignment (id, identity_id, role_id, scope, scope_id, valid_from, valid_until, created_at)
SELECT '66666666-6666-6666-6666-666666666602', '11111111-1111-1111-1111-111111111113', r.id, 'MERCHANT', 'merch_7788', '2026-06-25T00:00:00Z', NULL, '2026-06-25T00:00:00Z'
FROM role r WHERE r.name = 'MERCHANT_VIEWER';

INSERT INTO role_assignment (id, identity_id, role_id, scope, scope_id, valid_from, valid_until, created_at)
SELECT '66666666-6666-6666-6666-666666666603', '11111111-1111-1111-1111-111111111113', r.id, 'GLOBAL', NULL, '2026-06-25T00:00:00Z', NULL, '2026-06-25T00:00:00Z'
FROM role r WHERE r.name = 'END_USER';
