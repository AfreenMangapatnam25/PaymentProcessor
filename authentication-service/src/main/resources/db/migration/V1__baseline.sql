-- ============================================================================
-- Authentication Service baseline schema
-- Flyway is the single source of truth for the schema (Hibernate ddl-auto=none).
-- ============================================================================

CREATE TABLE identities (
    id                  VARCHAR(36)  PRIMARY KEY,
    principal_type      VARCHAR(20)  NOT NULL,
    email               VARCHAR(320),
    phone_e164          VARCHAR(20),
    status              VARCHAR(20)  NOT NULL,
    mfa_required        BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_count  INTEGER      NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    email_verified_at   TIMESTAMPTZ,
    phone_verified_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX ux_identities_email ON identities (LOWER(email)) WHERE email IS NOT NULL;
CREATE INDEX ix_identities_status ON identities (status);

CREATE TABLE credentials (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    kind          VARCHAR(20)  NOT NULL,
    secret_hash   VARCHAR(255) NOT NULL,
    algo_params   VARCHAR(100),
    rotated_at    TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_credentials_identity ON credentials (identity_id, kind);
-- Only one active (non-rotated) password per identity.
CREATE UNIQUE INDEX ux_credentials_active_password
    ON credentials (identity_id, kind) WHERE rotated_at IS NULL;

CREATE TABLE refresh_tokens (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    family_id     VARCHAR(36)  NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    device_id     VARCHAR(36),
    replaced_by   VARCHAR(36),
    issued_at     TIMESTAMPTZ  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked_at    TIMESTAMPTZ
);
CREATE INDEX ix_refresh_identity ON refresh_tokens (identity_id);
CREATE INDEX ix_refresh_family ON refresh_tokens (family_id);

CREATE TABLE api_keys (
    id            VARCHAR(36)  PRIMARY KEY,
    owner_type    VARCHAR(20)  NOT NULL,
    owner_id      VARCHAR(36)  NOT NULL,
    prefix        VARCHAR(16)  NOT NULL,
    secret_hash   VARCHAR(64)  NOT NULL,
    environment   VARCHAR(20)  NOT NULL,
    last_used_at  TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX ux_api_keys_prefix ON api_keys (prefix);
CREATE INDEX ix_api_keys_owner ON api_keys (owner_type, owner_id);

CREATE TABLE api_key_scopes (
    api_key_id    VARCHAR(36)  NOT NULL REFERENCES api_keys (id) ON DELETE CASCADE,
    scope         VARCHAR(64)  NOT NULL,
    PRIMARY KEY (api_key_id, scope)
);

CREATE TABLE mfa_factors (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    kind          VARCHAR(20)  NOT NULL,
    secret_ref    VARCHAR(255),
    label         VARCHAR(120),
    verified_at   TIMESTAMPTZ,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_mfa_identity ON mfa_factors (identity_id, status);

CREATE TABLE mfa_recovery_codes (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    code_hash     VARCHAR(64)  NOT NULL,
    used_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_recovery_identity ON mfa_recovery_codes (identity_id);

CREATE TABLE login_attempts (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identity_id   VARCHAR(36),
    email         VARCHAR(320),
    ip            VARCHAR(45),
    user_agent    VARCHAR(512),
    result        VARCHAR(30)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_login_attempts_identity ON login_attempts (identity_id, created_at);
CREATE INDEX ix_login_attempts_ip ON login_attempts (ip, created_at);

CREATE TABLE devices (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    fingerprint   VARCHAR(128) NOT NULL,
    label         VARCHAR(120),
    trust_level   VARCHAR(20)  NOT NULL,
    last_ip       VARCHAR(45),
    last_seen_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_devices_identity_fp UNIQUE (identity_id, fingerprint)
);

CREATE TABLE password_reset_tokens (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE verification_tokens (
    id            VARCHAR(36)  PRIMARY KEY,
    identity_id   VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,
    channel       VARCHAR(20)  NOT NULL,
    destination   VARCHAR(320) NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL,
    code          VARCHAR(10),
    expires_at    TIMESTAMPTZ  NOT NULL,
    consumed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_verification_identity ON verification_tokens (identity_id, channel);
