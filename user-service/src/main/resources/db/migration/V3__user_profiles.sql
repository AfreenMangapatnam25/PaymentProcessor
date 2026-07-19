-- =============================================================================
-- V3 :: user_profiles
-- -----------------------------------------------------------------------------
-- All user PII, encrypted at rest with the user's DEK. Ciphertext lives in the
-- *_encrypted BYTEA columns (AES-256-GCM: nonce || ciphertext || tag). Because
-- ciphertext is not searchable, each field we must look up or uniquely
-- constrain also gets a deterministic HMAC "blind index" (email_index /
-- phone_index): same plaintext -> same HMAC, but the HMAC is irreversible
-- without the index key. locale/timezone are not PII and stay in cleartext.
-- Shares its primary key with users (strict 1:1) and cascades on delete.
-- =============================================================================
CREATE TABLE user_profiles (
    user_id                  VARCHAR(40) NOT NULL,   -- = users.id (shared PK, 1:1)
    email_encrypted          BYTEA,
    email_index              VARCHAR(64),            -- HMAC-SHA256 hex blind index
    first_name_encrypted     BYTEA,
    last_name_encrypted      BYTEA,
    phone_encrypted          BYTEA,
    phone_index              VARCHAR(64),
    date_of_birth_encrypted  BYTEA,
    locale                   VARCHAR(16),            -- not PII (e.g. en_GB)
    timezone                 VARCHAR(64),            -- not PII (e.g. Europe/London)
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_user_profiles PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Global email uniqueness across platform users (blind index, so no plaintext).
CREATE UNIQUE INDEX uq_user_profiles_email_index
    ON user_profiles (email_index)
    WHERE email_index IS NOT NULL;

CREATE INDEX ix_user_profiles_phone_index
    ON user_profiles (phone_index)
    WHERE phone_index IS NOT NULL;

COMMENT ON TABLE user_profiles IS 'Encrypted user PII; blind-index columns enable lookup without plaintext.';
COMMENT ON COLUMN user_profiles.email_index IS 'Deterministic HMAC of normalized email; irreversible, used for uniqueness/lookup.';
