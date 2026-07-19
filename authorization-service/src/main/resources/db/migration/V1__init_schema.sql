-- ============================================================================
-- Authorization Service - baseline schema
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Payment authorization
-- ---------------------------------------------------------------------------
CREATE TABLE authorization_record (
    id                       UUID           PRIMARY KEY,
    status                   VARCHAR(32)    NOT NULL,
    type                     VARCHAR(24)    NOT NULL,
    merchant_id              VARCHAR(64)    NOT NULL,
    customer_id              VARCHAR(64),
    payment_reference        VARCHAR(64)    NOT NULL,
    requested_amount         NUMERIC(19,4)  NOT NULL,
    approved_amount          NUMERIC(19,4),
    captured_amount          NUMERIC(19,4),
    currency                 VARCHAR(3)     NOT NULL,
    card_network             VARCHAR(16),
    card_bin                 VARCHAR(8),
    card_last4               VARCHAR(4),
    card_exp_month           INTEGER,
    card_exp_year            INTEGER,
    authorization_code       VARCHAR(32),
    network_reference_id     VARCHAR(64),
    gateway_provider         VARCHAR(32),
    gateway_authorization_id VARCHAR(128),
    gateway_response_code    VARCHAR(32),
    gateway_response_message VARCHAR(512),
    response_code            VARCHAR(32),
    avs_result               VARCHAR(16),
    cvv_result               VARCHAR(16),
    requires_authentication  BOOLEAN        NOT NULL DEFAULT FALSE,
    authentication_url       VARCHAR(1024),
    original_authorization_id UUID,
    reauthorization_count    INTEGER        NOT NULL DEFAULT 0,
    idempotency_key          VARCHAR(128),
    risk_score               DOUBLE PRECISION,
    gateway_raw_response     TEXT,
    expires_at               TIMESTAMPTZ,
    authorized_at            TIMESTAMPTZ,
    captured_at              TIMESTAMPTZ,
    reversed_at              TIMESTAMPTZ,
    version                  BIGINT         NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_merchant     ON authorization_record (merchant_id);
CREATE INDEX idx_auth_payment_ref  ON authorization_record (payment_reference);
CREATE INDEX idx_auth_status       ON authorization_record (status);
CREATE INDEX idx_auth_idempotency  ON authorization_record (idempotency_key);
CREATE INDEX idx_auth_gateway_id   ON authorization_record (gateway_authorization_id);

CREATE TABLE idempotency_key (
    key_value     VARCHAR(128) PRIMARY KEY,
    request_hash  VARCHAR(64)  NOT NULL,
    resource_type VARCHAR(64),
    resource_id   UUID,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ  NOT NULL
);

-- ---------------------------------------------------------------------------
-- Access control (RBAC / ABAC)
-- ---------------------------------------------------------------------------
CREATE TABLE permission (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    resource    VARCHAR(64)  NOT NULL,
    action      VARCHAR(32)  NOT NULL,
    description VARCHAR(256),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_permission_name UNIQUE (name)
);

CREATE TABLE role (
    id             UUID         PRIMARY KEY,
    name           VARCHAR(64)  NOT NULL,
    description    VARCHAR(256),
    category       VARCHAR(16)  NOT NULL,
    parent_role_id UUID         REFERENCES role (id),
    system_role    BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_name UNIQUE (name)
);

CREATE TABLE role_permission (
    role_id       UUID NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE role_assignment (
    id          UUID        PRIMARY KEY,
    identity_id VARCHAR(64) NOT NULL,
    role_id     UUID        NOT NULL REFERENCES role (id),
    scope       VARCHAR(16) NOT NULL,
    scope_id    VARCHAR(64),
    valid_from  TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_assignment UNIQUE (identity_id, role_id, scope, scope_id)
);
CREATE INDEX idx_assignment_identity ON role_assignment (identity_id);

CREATE TABLE policy (
    id             UUID         PRIMARY KEY,
    name           VARCHAR(128) NOT NULL,
    type           VARCHAR(16)  NOT NULL,
    effect         VARCHAR(8)   NOT NULL,
    resource       VARCHAR(64)  NOT NULL,
    action         VARCHAR(32)  NOT NULL,
    condition_json TEXT,
    priority       INTEGER      NOT NULL DEFAULT 100,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_policy_name UNIQUE (name)
);
CREATE INDEX idx_policy_target ON policy (resource, action);
