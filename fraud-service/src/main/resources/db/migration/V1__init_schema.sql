-- ============================================================================
-- Fraud Service — initial schema
-- ============================================================================

CREATE TABLE devices (
    id            UUID          PRIMARY KEY,
    fingerprint   VARCHAR(200)  NOT NULL,
    merchant_id   VARCHAR(100),
    first_seen    TIMESTAMPTZ,
    last_seen     TIMESTAMPTZ,
    metadata      TEXT
);

CREATE INDEX idx_devices_fingerprint ON devices (fingerprint);
CREATE INDEX idx_devices_merchant    ON devices (merchant_id);

-- ----------------------------------------------------------------------------

CREATE TABLE cases (
    id            UUID          PRIMARY KEY,
    merchant_id   VARCHAR(100),
    intent_id     VARCHAR(100),
    status        VARCHAR(30),
    assignee      VARCHAR(100),
    created_at    TIMESTAMPTZ
);

CREATE INDEX idx_cases_intent   ON cases (intent_id);
CREATE INDEX idx_cases_merchant ON cases (merchant_id);
CREATE INDEX idx_cases_status   ON cases (status);

-- ----------------------------------------------------------------------------

CREATE TABLE lists (
    id            UUID          PRIMARY KEY,
    merchant_id   VARCHAR(100),
    list_kind     VARCHAR(20),
    attribute     VARCHAR(40),
    value         VARCHAR(200),
    reason        TEXT,
    expires_at    TIMESTAMPTZ
);

CREATE INDEX idx_lists_attribute_value ON lists (attribute, value);
CREATE INDEX idx_lists_merchant        ON lists (merchant_id);

-- ----------------------------------------------------------------------------

CREATE TABLE model_registry (
    id            UUID          PRIMARY KEY,
    name          VARCHAR(150)  NOT NULL,
    version       VARCHAR(50),
    status        VARCHAR(20),
    created_at    TIMESTAMPTZ
);

CREATE INDEX idx_model_registry_name   ON model_registry (name);
CREATE INDEX idx_model_registry_status ON model_registry (status);

-- ----------------------------------------------------------------------------

CREATE TABLE risk_assessments (
    id                UUID          PRIMARY KEY,
    intent_id         VARCHAR(100),
    merchant_id       VARCHAR(100),
    score             NUMERIC(5,2),
    decision          VARCHAR(30),
    triggered_rules   TEXT,
    features          TEXT,
    model             VARCHAR(100),
    latency_ms        INTEGER,
    created_at        TIMESTAMPTZ
);

CREATE INDEX idx_risk_assessments_intent   ON risk_assessments (intent_id);
CREATE INDEX idx_risk_assessments_merchant ON risk_assessments (merchant_id);
CREATE INDEX idx_risk_assessments_created  ON risk_assessments (created_at);

-- ----------------------------------------------------------------------------

CREATE TABLE rules (
    id            UUID          PRIMARY KEY,
    scope         VARCHAR(50),
    name          VARCHAR(150),
    expr          TEXT,
    action        VARCHAR(20),
    priority      INTEGER,
    enabled       BOOLEAN,
    version       INTEGER
);

CREATE INDEX idx_rules_name    ON rules (name);
CREATE INDEX idx_rules_enabled ON rules (enabled);
CREATE INDEX idx_rules_scope   ON rules (scope);
