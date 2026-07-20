-- =============================================================================
-- V2 :: users
-- -----------------------------------------------------------------------------
-- A "user" is a platform-level person (README two-level model). Identity/auth
-- is delegated: identity_id is an OPAQUE reference into authentication-service
-- and is never dereferenced here. No PII lives on this table -- name/email/DOB
-- are encrypted on user_profiles (V3). crypto_key_id points at this user's DEK.
-- =============================================================================
CREATE TABLE users (
    id             VARCHAR(40)  NOT NULL,           -- usr_<ULID>
    identity_id    VARCHAR(128) NOT NULL,           -- opaque -> authentication-service
    status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    crypto_key_id  VARCHAR(40),                     -- -> crypto_keys.id (this user's DEK)
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    erased_at      TIMESTAMPTZ,                     -- set when crypto-shredded
    version        BIGINT       NOT NULL DEFAULT 0, -- optimistic lock (rule 8)

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'LOCKED', 'ERASED')),
    CONSTRAINT fk_users_crypto_key FOREIGN KEY (crypto_key_id) REFERENCES crypto_keys (id)
);

-- One platform user per external identity.
CREATE UNIQUE INDEX uq_users_identity_id ON users (identity_id);
CREATE INDEX ix_users_status ON users (status);

COMMENT ON TABLE users IS 'Platform-level person; auth delegated via opaque identity_id.';
COMMENT ON COLUMN users.identity_id IS 'Opaque reference to authentication-service; never dereferenced here.';
