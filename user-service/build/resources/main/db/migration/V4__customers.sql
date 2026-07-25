-- =============================================================================
-- V4 :: customers
-- -----------------------------------------------------------------------------
-- A "customer" is a MERCHANT-SCOPED buyer. Hard isolation rule: two merchants
-- must never see the same customer row. merchant_id references merchant-service
-- (no cross-service FK -- services own their data). A customer MAY be linked to
-- a platform user (user_id, nullable) but need not be. PII is encrypted with
-- the customer's own DEK; email has a per-merchant blind index. Deletion is
-- SOFT (deleted_at, rule 9); erasure is crypto-shred (erased_at, rule 10).
-- default_instrument_token is an OPAQUE vault-service reference, never
-- dereferenced here (README).
-- =============================================================================
CREATE TABLE customers (
    id                       VARCHAR(40)  NOT NULL,   -- cus_<ULID>
    merchant_id              VARCHAR(64)  NOT NULL,   -- -> merchant-service (no FK)
    user_id                  VARCHAR(40),             -- optional link to platform user
    external_ref             VARCHAR(128),            -- merchant's own id for this buyer
    email_encrypted          BYTEA,
    email_index              VARCHAR(64),
    full_name_encrypted      BYTEA,
    phone_encrypted          BYTEA,
    phone_index              VARCHAR(64),
    default_instrument_token VARCHAR(128),            -- opaque -> vault-service
    crypto_key_id            VARCHAR(40),             -- -> crypto_keys.id
    status                   VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    metadata                 JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ,             -- soft delete
    erased_at                TIMESTAMPTZ,             -- crypto-shred
    version                  BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED', 'ERASED')),
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_customers_crypto_key FOREIGN KEY (crypto_key_id) REFERENCES crypto_keys (id)
);

-- Per-merchant dedupe on the merchant's own reference, live rows only.
CREATE UNIQUE INDEX uq_customers_merchant_external_ref
    ON customers (merchant_id, external_ref)
    WHERE external_ref IS NOT NULL AND deleted_at IS NULL;

-- Email uniqueness scoped to the merchant (isolation), live rows only.
CREATE UNIQUE INDEX uq_customers_merchant_email_index
    ON customers (merchant_id, email_index)
    WHERE email_index IS NOT NULL AND deleted_at IS NULL;

-- Primary access path is always merchant-scoped.
CREATE INDEX ix_customers_merchant
    ON customers (merchant_id)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_customers_user ON customers (user_id) WHERE user_id IS NOT NULL;

COMMENT ON TABLE customers IS 'Merchant-scoped buyer; every query must be filtered by merchant_id.';
COMMENT ON COLUMN customers.default_instrument_token IS 'Opaque vault-service token; never dereferenced in this service.';
