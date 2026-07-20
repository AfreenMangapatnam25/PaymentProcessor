-- ============================================================================
-- Limit Service — initial schema
-- ============================================================================

CREATE TABLE limit_configuration (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    scope           VARCHAR(20)     NOT NULL,
    scope_id        VARCHAR(100),
    dimension       VARCHAR(20)     NOT NULL,
    time_window     VARCHAR(20)     NOT NULL,
    threshold       NUMERIC(19,4)   NOT NULL,
    currency        VARCHAR(3),
    enforcement     VARCHAR(10)     NOT NULL,
    priority        INTEGER         NOT NULL DEFAULT 0,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    time_zone       VARCHAR(60)     NOT NULL DEFAULT 'UTC',
    record_version  BIGINT,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_limit_cfg_lookup   ON limit_configuration (scope, scope_id, active);
CREATE INDEX idx_limit_cfg_currency ON limit_configuration (currency);

-- ----------------------------------------------------------------------------

CREATE TABLE usage_counter (
    id                UUID          PRIMARY KEY,
    limit_config_id   UUID          NOT NULL,
    window_key        VARCHAR(40)   NOT NULL,
    reserved_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    committed_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    reserved_count    BIGINT        NOT NULL DEFAULT 0,
    committed_count   BIGINT        NOT NULL DEFAULT 0,
    window_start      TIMESTAMPTZ   NOT NULL,
    record_version    BIGINT,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_usage_counter UNIQUE (limit_config_id, window_key),
    CONSTRAINT fk_usage_counter_config FOREIGN KEY (limit_config_id)
        REFERENCES limit_configuration (id)
);

CREATE INDEX idx_usage_counter_config ON usage_counter (limit_config_id);

-- ----------------------------------------------------------------------------

CREATE TABLE limit_reservation (
    id                UUID          PRIMARY KEY,
    transaction_id    VARCHAR(100)  NOT NULL,
    idempotency_key   VARCHAR(100)  UNIQUE,
    customer_id       VARCHAR(100),
    merchant_id       VARCHAR(100),
    currency          VARCHAR(3),
    reserved_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    committed_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    status            VARCHAR(15)   NOT NULL,
    expires_at        TIMESTAMPTZ   NOT NULL,
    record_version    BIGINT,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_reservation_txn            ON limit_reservation (transaction_id);
CREATE INDEX idx_reservation_status_expiry  ON limit_reservation (status, expires_at);

-- ----------------------------------------------------------------------------

CREATE TABLE reservation_line (
    id                UUID          PRIMARY KEY,
    reservation_id    UUID          NOT NULL,
    limit_config_id   UUID          NOT NULL,
    usage_counter_id  UUID          NOT NULL,
    window_key        VARCHAR(40)   NOT NULL,
    held_amount       NUMERIC(19,4) NOT NULL DEFAULT 0,
    held_count        BIGINT        NOT NULL DEFAULT 0,
    committed_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    CONSTRAINT fk_res_line_reservation FOREIGN KEY (reservation_id)
        REFERENCES limit_reservation (id) ON DELETE CASCADE
);

CREATE INDEX idx_res_line_reservation ON reservation_line (reservation_id);

-- ----------------------------------------------------------------------------

CREATE TABLE limit_audit_log (
    id                UUID          PRIMARY KEY,
    action            VARCHAR(30)   NOT NULL,
    entity_reference  VARCHAR(150),
    transaction_id    VARCHAR(100),
    actor             VARCHAR(100),
    detail            TEXT,
    created_at        TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_audit_entity  ON limit_audit_log (entity_reference);
CREATE INDEX idx_audit_txn     ON limit_audit_log (transaction_id);
CREATE INDEX idx_audit_created ON limit_audit_log (created_at);
