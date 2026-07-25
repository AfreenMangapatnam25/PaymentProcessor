-- ============================================================================
-- tokenization-service local/dev seed data.
--
-- Runs on EVERY startup (spring.sql.init.mode=always), because this service has no
-- Flyway migrations - Hibernate creates the schema via ddl-auto=update and this file
-- populates it. Every statement is therefore written to be IDEMPOTENT with
-- ON CONFLICT DO NOTHING; without it the second boot fails with
--   duplicate key value violates unique constraint "instruments_pkey".
--
-- Card numbers/tokens below are obvious test values, never real PANs.
-- ============================================================================

-- Local/dev seed data for tokenization-service.
-- Loaded via spring.sql.init.mode=always + spring.jpa.defer-datasource-initialization=true,
-- after Hibernate (ddl-auto: update) has created the schema from the @Entity classes.
-- NOT for use in production - see comment above spring.jpa.hibernate.ddl-auto in application.yml.

-- ============ instruments ============
INSERT INTO instruments (id, token, kind, scope_merchant_id, fingerprint, created_at, deleted_at) VALUES
('instr-card-0001', 'tok_9f1c2e3b4a5d6f70', 'CARD', 'merchant-0001', decode('66696e6765727072696e742d30303031', 'hex'), '2026-01-10T09:00:00Z', NULL),
('instr-card-0002', 'tok_1a2b3c4d5e6f7081', 'CARD', 'merchant-0001', decode('66696e6765727072696e742d30303032', 'hex'), '2026-01-11T09:00:00Z', NULL),
('instr-bank-0001', 'tok_bank8877665544', 'BANK_ACCOUNT', 'merchant-0002', decode('66696e6765727072696e742d62616e6b31', 'hex'), '2026-01-12T09:00:00Z', NULL),
('instr-card-0003', 'tok_deadbeefcafe0003', 'CARD', 'merchant-0003', decode('66696e6765727072696e742d30303033', 'hex'), '2026-01-13T09:00:00Z', NULL)
ON CONFLICT DO NOTHING;

-- ============ card_details ============
INSERT INTO card_details (instrument_id, pan_ciphertext, dek_id, nonce, aad, exp_month, exp_year, last4, bin, brand, funding, issuer_country, product_code, cardholder_name_ciphertext) VALUES
('instr-card-0001', decode('66616b652d636970686572746578742d34313131313131313131313131313131', 'hex'), 'dek-0001', decode('6e6f6e63652d31323334353637', 'hex'), decode('696e7374722d636172642d30303031', 'hex'), 12, 2028, '1111', '411111', 'VISA', 'CREDIT', 'US', 'CLASSIC', decode('656e63727970746564206a6f686e20646f65', 'hex')),
('instr-card-0002', decode('66616b652d636970686572746578742d35353535353535353535353435', 'hex'), 'dek-0002', decode('6e6f6e63652d32323334353637', 'hex'), decode('696e7374722d636172642d30303032', 'hex'), 6, 2027, '4444', '555555', 'MASTERCARD', 'DEBIT', 'US', 'STANDARD', decode('656e63727970746564206a616e6520646f65', 'hex')),
('instr-card-0003', decode('66616b652d636970686572746578742d33373030303030303030303030', 'hex'), 'dek-0003', decode('6e6f6e63652d33323334353637', 'hex'), decode('696e7374722d636172642d30303033', 'hex'), 3, 2029, '0005', '370000', 'AMEX', 'CREDIT', 'GB', 'GOLD', decode('656e63727970746564206173682077696c6c69616d73', 'hex'))
ON CONFLICT DO NOTHING;

-- ============ bank_details ============
INSERT INTO bank_details (instrument_id, account_ciphertext, routing_ciphertext, dek_id, nonce, last4, bank_name, country) VALUES
('instr-bank-0001', decode('656e632d6163636f756e742d30303031', 'hex'), decode('656e632d726f7574696e672d30303031', 'hex'), 'dek-0004', decode('6e6f6e63652d62616e6b2d30303031', 'hex'), '6789', 'First National Bank', 'US'),
('instr-bank-0002', decode('656e632d6163636f756e742d30303032', 'hex'), decode('656e632d726f7574696e672d30303032', 'hex'), 'dek-0004', decode('6e6f6e63652d62616e6b2d30303032', 'hex'), '4321', 'Second Federal Credit Union', 'US')
ON CONFLICT DO NOTHING;

-- ============ bin_ranges ============
INSERT INTO bin_ranges (id, bin_low, bin_high, brand, funding, issuer, country, product_code, is_prepaid, is_commercial) VALUES
(1, 411111000000, 411111999999, 'VISA', 'CREDIT', 'CHASE', 'US', 'CLASSIC', false, false),
(2, 555555000000, 555555999999, 'MASTERCARD', 'DEBIT', 'WELLS FARGO', 'US', 'STANDARD', false, false),
(3, 370000000000, 370000999999, 'AMEX', 'CREDIT', 'AMERICAN EXPRESS', 'GB', 'GOLD', false, true),
(4, 601100000000, 601100999999, 'DISCOVER', 'CREDIT', 'DISCOVER BANK', 'US', 'CASHBACK', false, false)
ON CONFLICT DO NOTHING;

-- ============ network_tokens ============
INSERT INTO network_tokens (id, instrument_id, network, token_ciphertext, tar, exp_month, exp_year, status, provisioned_at, last_updated_at) VALUES
('ntok-0001', 'instr-card-0001', 'VISA', decode('6e6574776f726b2d746f6b656e2d636970686572746578742d30303031', 'hex'), 'TAR1234567890', 12, 2028, 'ACTIVE', '2026-01-10T09:05:00Z', '2026-01-10T09:05:00Z'),
('ntok-0002', 'instr-card-0002', 'MASTERCARD', decode('6e6574776f726b2d746f6b656e2d636970686572746578742d30303032', 'hex'), 'TAR2233445566', 6, 2027, 'ACTIVE', '2026-01-11T09:05:00Z', '2026-01-11T09:05:00Z'),
('ntok-0003', 'instr-card-0003', 'AMEX', decode('6e6574776f726b2d746f6b656e2d636970686572746578742d30303033', 'hex'), 'TAR9988776655', 3, 2029, 'SUSPENDED', '2026-01-13T09:05:00Z', '2026-02-01T10:00:00Z')
ON CONFLICT DO NOTHING;

-- ============ dek_registry ============
INSERT INTO dek_registry (id, kek_id, wrapped_dek, created_at, rotated_at) VALUES
('dek-0001', 'kek-master-0001', decode('77726170706564646b737465732d30303031', 'hex'), '2026-01-01T00:00:00Z', NULL),
('dek-0002', 'kek-master-0001', decode('77726170706564646b737465732d30303032', 'hex'), '2026-01-01T00:00:00Z', NULL),
('dek-0003', 'kek-master-0002', decode('77726170706564646b737465732d30303033', 'hex'), '2026-02-01T00:00:00Z', '2026-06-01T00:00:00Z'),
('dek-0004', 'kek-master-0002', decode('77726170706564646b737465732d30303034', 'hex'), '2026-02-01T00:00:00Z', NULL)
ON CONFLICT DO NOTHING;

-- ============ instrument_access_log ============
INSERT INTO instrument_access_log (id, instrument_id, actor, purpose, correlation_id, created_at) VALUES
(1, 'instr-card-0001', 'svc-payment-orchestrator', 'AUTHORIZATION', 'corr-0001', '2026-01-10T09:10:00Z'),
(2, 'instr-card-0002', 'svc-payment-orchestrator', 'AUTHORIZATION', 'corr-0002', '2026-01-11T09:10:00Z'),
(3, 'instr-bank-0001', 'svc-fraud-review', 'MANUAL_REVIEW', 'corr-0003', '2026-01-12T10:00:00Z')
ON CONFLICT DO NOTHING;
