-- =============================================================================
-- V1 :: crypto_keys
-- -----------------------------------------------------------------------------
-- Envelope-encryption registry that makes GDPR erasure a crypto-shred instead
-- of a physical DELETE (rule 10). Each PII-bearing subject (a user or a
-- customer) gets one ACTIVE Data Encryption Key (DEK). The DEK is stored only
-- in WRAPPED form (encrypted under a KMS/HSM master key that never leaves the
-- KMS). To "erase" a subject we destroy its DEK: null the wrapped bytes and
-- flip status to DESTROYED. The ciphertext in users/customers/addresses then
-- becomes mathematically unrecoverable, while all foreign keys and ledger
-- references stay intact.
-- =============================================================================
CREATE TABLE crypto_keys (
    id                VARCHAR(40)  NOT NULL,
    subject_type      VARCHAR(16)  NOT NULL,
    subject_id        VARCHAR(40)  NOT NULL,
    -- DEK encrypted under the KMS master key; NULL once the key is destroyed.
    wrapped_dek       BYTEA,
    key_version       INTEGER      NOT NULL DEFAULT 1,
    algorithm         VARCHAR(32)  NOT NULL DEFAULT 'AES_256_GCM',
    -- Which KMS master key (KEK) wrapped this DEK, for rotation/audit.
    kms_master_key_id VARCHAR(256) NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    destroyed_at      TIMESTAMPTZ,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_crypto_keys PRIMARY KEY (id),
    CONSTRAINT ck_crypto_keys_subject_type CHECK (subject_type IN ('USER', 'CUSTOMER')),
    CONSTRAINT ck_crypto_keys_status CHECK (status IN ('ACTIVE', 'DESTROYED')),
    -- Enforce the invariant: a destroyed key has no bytes and a destruction
    -- timestamp; an active key must still hold its wrapped DEK.
    CONSTRAINT ck_crypto_keys_state CHECK (
        (status = 'DESTROYED' AND wrapped_dek IS NULL AND destroyed_at IS NOT NULL)
        OR
        (status = 'ACTIVE' AND wrapped_dek IS NOT NULL AND destroyed_at IS NULL)
    )
);

-- At most one ACTIVE key per subject; destroyed rows are retained for audit.
CREATE UNIQUE INDEX uq_crypto_keys_active_subject
    ON crypto_keys (subject_type, subject_id)
    WHERE status = 'ACTIVE';

-- Reverse lookup for the GDPR erasure job (find a subject's key(s)).
CREATE INDEX ix_crypto_keys_subject
    ON crypto_keys (subject_type, subject_id);

COMMENT ON TABLE crypto_keys IS 'Per-subject wrapped DEKs; destroying a row is the GDPR crypto-shred.';
COMMENT ON COLUMN crypto_keys.wrapped_dek IS 'DEK encrypted under KMS master key; NULL after crypto-shred.';
