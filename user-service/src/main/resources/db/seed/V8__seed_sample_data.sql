-- =============================================================================
-- V8 :: seed_sample_data (LOCAL/DEV ONLY)
-- -----------------------------------------------------------------------------
-- Sample rows for local API testing (see API_TESTING.md). This migration is
-- only ever applied when spring.flyway.locations includes classpath:db/seed
-- for the "local" profile -- baseline application.yml does NOT include the
-- db/seed path, so this file never runs against dev/prod databases.
--
-- IMPORTANT: the *_encrypted BYTEA columns below hold PLACEHOLDER bytes, not
-- real AES-256-GCM ciphertext produced by this service's crypto layer. The
-- API cannot decrypt them at read time (EncryptionService will fail to
-- authenticate the GCM tag) -- they exist only to make FK/NOT NULL/CHECK
-- constraints satisfiable so the row set loads cleanly. To exercise the real
-- read path locally, create data through the API (POST /api/v1/users, etc.)
-- so the service performs the actual encryption. Likewise email_index /
-- phone_index are placeholder hex strings, not real HMAC blind indexes.
-- =============================================================================

-- ---- crypto_keys -------------------------------------------------------
INSERT INTO crypto_keys (id, subject_type, subject_id, wrapped_dek, key_version, algorithm, kms_master_key_id, status, created_at, destroyed_at, version)
VALUES
    ('key_seed_user_0001', 'USER', 'usr_seed_0000000001',
     decode('736565642d77726170706564206465722076616c756520666f7220757365722030303031', 'hex'),
     1, 'AES_256_GCM', 'local-dev-master-key', 'ACTIVE', '2026-07-01T09:00:00Z', NULL, 0),
    ('key_seed_cust_0001', 'CUSTOMER', 'cus_seed_0000000001',
     decode('736565642d77726170706564206465722076616c756520666f7220637573746f6d6572', 'hex'),
     1, 'AES_256_GCM', 'local-dev-master-key', 'ACTIVE', '2026-07-01T09:00:00Z', NULL, 0);

-- ---- users --------------------------------------------------------------
INSERT INTO users (id, identity_id, status, crypto_key_id, created_at, updated_at, erased_at, version)
VALUES
    ('usr_seed_0000000001', 'idn_seed_0000000001', 'ACTIVE', 'key_seed_user_0001',
     '2026-07-01T09:00:00Z', '2026-07-01T09:00:00Z', NULL, 0);

-- ---- user_profiles -------------------------------------------------------
-- email_index / phone_index below are placeholder 64-char hex strings shaped
-- like a real HMAC-SHA256 blind index, not derived from the plaintext email.
INSERT INTO user_profiles (user_id, email_encrypted, email_index, first_name_encrypted, last_name_encrypted, phone_encrypted, phone_index, date_of_birth_encrypted, locale, timezone, updated_at, version)
VALUES
    ('usr_seed_0000000001',
     decode('706c616365686f6c6465722d656d61696c2d63697068657274657874', 'hex'),
     'a1b2c3d4e5f60718293a4b5c6d7e8f90112233445566778899aabbccddeeff01',
     decode('706c616365686f6c6465722d666e2d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d6c6e2d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d70686f6e652d63697068657274657874', 'hex'),
     'f1e2d3c4b5a6978869574635241302f1e0d9c8b7a6958473625140f3e2d1c0b',
     decode('706c616365686f6c6465722d646f622d63697068657274657874', 'hex'),
     'en_US', 'America/New_York', '2026-07-01T09:00:00Z', 0);

-- ---- customers -------------------------------------------------------
INSERT INTO customers (id, merchant_id, user_id, external_ref, email_encrypted, email_index, full_name_encrypted, phone_encrypted, phone_index, default_instrument_token, crypto_key_id, status, metadata, created_at, updated_at, deleted_at, erased_at, version)
VALUES
    ('cus_seed_0000000001', 'mer_seed_0000000001', 'usr_seed_0000000001', 'ext-ref-seed-0001',
     decode('706c616365686f6c6465722d637573742d656d61696c2d63697068657274657874', 'hex'),
     'b2c3d4e5f60718293a4b5c6d7e8f90112233445566778899aabbccddeeff0102',
     decode('706c616365686f6c6465722d637573742d66756c6c6e616d652d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d637573742d70686f6e652d63697068657274657874', 'hex'),
     'e2d3c4b5a6978869574635241302f1e0d9c8b7a6958473625140f3e2d1c0b1a',
     'tok_vault_seed_0000000001',
     'key_seed_cust_0001', 'ACTIVE', '{"segment": "seed-data"}'::jsonb,
     '2026-07-01T09:00:00Z', '2026-07-01T09:00:00Z', NULL, NULL, 0);

-- ---- addresses -------------------------------------------------------
INSERT INTO addresses (id, owner_type, owner_id, address_type, line1_encrypted, line2_encrypted, city_encrypted, region_encrypted, postal_code_encrypted, country_code, crypto_key_id, is_default, created_at, updated_at, deleted_at, version)
VALUES
    ('adr_seed_0000000001', 'USER', 'usr_seed_0000000001', 'SHIPPING',
     decode('706c616365686f6c6465722d6c696e65312d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d6c696e65322d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d636974792d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d726567696f6e2d63697068657274657874', 'hex'),
     decode('706c616365686f6c6465722d706f7374616c2d63697068657274657874', 'hex'),
     'US', 'key_seed_user_0001', true,
     '2026-07-01T09:00:00Z', '2026-07-01T09:00:00Z', NULL, 0);

-- ---- consents -------------------------------------------------------
INSERT INTO consents (id, subject_type, subject_id, consent_kind, granted, source, policy_version, granted_at, revoked_at, created_at, updated_at, version)
VALUES
    ('con_seed_0000000001', 'USER', 'usr_seed_0000000001', 'DATA_PROCESSING', true, 'web', 'v1.0',
     '2026-07-01T09:00:00Z', NULL, '2026-07-01T09:00:00Z', '2026-07-01T09:00:00Z', 0);
