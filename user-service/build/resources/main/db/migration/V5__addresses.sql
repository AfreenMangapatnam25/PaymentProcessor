-- =============================================================================
-- V5 :: addresses
-- -----------------------------------------------------------------------------
-- Postal addresses for either a user or a customer (polymorphic owner_type /
-- owner_id -- no FK because it points at two tables; integrity enforced in the
-- domain). Address lines are PII and encrypted with the owner's DEK.
-- country_code stays in cleartext: it is not PII on its own and is needed for
-- tax/routing/analytics. Soft delete via deleted_at.
-- =============================================================================
CREATE TABLE addresses (
    id                     VARCHAR(40) NOT NULL,     -- adr_<ULID>
    owner_type             VARCHAR(16) NOT NULL,     -- USER | CUSTOMER
    owner_id               VARCHAR(40) NOT NULL,     -- users.id or customers.id
    address_type           VARCHAR(16) NOT NULL DEFAULT 'SHIPPING',
    line1_encrypted        BYTEA,
    line2_encrypted        BYTEA,
    city_encrypted         BYTEA,
    region_encrypted       BYTEA,
    postal_code_encrypted  BYTEA,
    country_code           CHAR(2),                  -- ISO-3166-1 alpha-2, not PII
    crypto_key_id          VARCHAR(40),              -- -> crypto_keys.id (owner's DEK)
    is_default             BOOLEAN     NOT NULL DEFAULT false,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMPTZ,
    version                BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_addresses PRIMARY KEY (id),
    CONSTRAINT ck_addresses_owner_type CHECK (owner_type IN ('USER', 'CUSTOMER')),
    CONSTRAINT ck_addresses_type CHECK (address_type IN ('BILLING', 'SHIPPING')),
    CONSTRAINT fk_addresses_crypto_key FOREIGN KEY (crypto_key_id) REFERENCES crypto_keys (id)
);

CREATE INDEX ix_addresses_owner
    ON addresses (owner_type, owner_id)
    WHERE deleted_at IS NULL;

-- At most one default address per owner per type (live rows only).
CREATE UNIQUE INDEX uq_addresses_default
    ON addresses (owner_type, owner_id, address_type)
    WHERE is_default = true AND deleted_at IS NULL;

COMMENT ON TABLE addresses IS 'Encrypted postal addresses for a user or customer (polymorphic owner).';
