-- Dispute Service schema: disputes, dispute_events, evidence, liability,
-- representments, reason_code_catalog.

CREATE TABLE disputes (
    id                    VARCHAR(64)   NOT NULL,
    chargeback_id         VARCHAR(128)  NOT NULL,
    transaction_id        VARCHAR(64),
    payment_id            VARCHAR(64),
    merchant_id           VARCHAR(64)   NOT NULL,
    customer_id           VARCHAR(64),
    network               VARCHAR(20)   NOT NULL,
    type                  VARCHAR(30)   NOT NULL,
    source                VARCHAR(30),
    stage                 VARCHAR(30)   NOT NULL,
    status                VARCHAR(30)   NOT NULL,
    reason_code           VARCHAR(20),
    reason_description    VARCHAR(500),
    amount_minor          BIGINT        NOT NULL,
    currency              VARCHAR(3)    NOT NULL,
    chargeback_fee_minor  BIGINT,
    is_partial            BOOLEAN       NOT NULL DEFAULT FALSE,
    liability_party       VARCHAR(20),
    received_at           TIMESTAMP,
    opened_at             TIMESTAMP,
    deadline_at           TIMESTAMP,
    resolved_at           TIMESTAMP,
    closed_at             TIMESTAMP,
    merchant_notified     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    version               BIGINT,
    CONSTRAINT pk_disputes PRIMARY KEY (id),
    CONSTRAINT uq_disputes_chargeback_id UNIQUE (chargeback_id)
);

CREATE INDEX idx_dispute_merchant ON disputes (merchant_id);
CREATE INDEX idx_dispute_status ON disputes (status);
CREATE INDEX idx_dispute_deadline ON disputes (deadline_at);
CREATE UNIQUE INDEX idx_dispute_chargeback ON disputes (chargeback_id);

CREATE TABLE dispute_events (
    id            BIGSERIAL     NOT NULL,
    dispute_id    VARCHAR(64)   NOT NULL,
    type          VARCHAR(40)   NOT NULL,
    actor         VARCHAR(40),
    description   VARCHAR(1000),
    from_status   VARCHAR(30),
    to_status     VARCHAR(30),
    created_at    TIMESTAMP,
    CONSTRAINT pk_dispute_events PRIMARY KEY (id),
    CONSTRAINT fk_dispute_events_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id)
);

CREATE INDEX idx_event_dispute ON dispute_events (dispute_id);

CREATE TABLE evidence (
    id               VARCHAR(64)   NOT NULL,
    dispute_id       VARCHAR(64)   NOT NULL,
    file_name        VARCHAR(255),
    storage_key      VARCHAR(255),
    type             VARCHAR(20),
    category         VARCHAR(30),
    size_bytes       BIGINT,
    sha256           VARCHAR(64),
    status           VARCHAR(20),
    description      VARCHAR(500),
    malware_scanned  BOOLEAN       NOT NULL DEFAULT FALSE,
    ocr_text         TEXT,
    uploaded_by      VARCHAR(64),
    uploaded_at      TIMESTAMP,
    reviewed_at      TIMESTAMP,
    submitted_at     TIMESTAMP,
    CONSTRAINT pk_evidence PRIMARY KEY (id),
    CONSTRAINT fk_evidence_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id)
);

CREATE INDEX idx_evidence_dispute ON evidence (dispute_id);

CREATE TABLE liability (
    id                     VARCHAR(64)  NOT NULL,
    dispute_id             VARCHAR(64)  NOT NULL,
    party                  VARCHAR(20),
    disputed_amount_minor  BIGINT,
    fee_minor              BIGINT,
    total_minor            BIGINT,
    currency               VARCHAR(3),
    reserve_tier           VARCHAR(20),
    reserve_percentage     INTEGER,
    ledger_hold_id         VARCHAR(64),
    ledger_journal_id      VARCHAR(64),
    reversed               BOOLEAN      NOT NULL DEFAULT FALSE,
    recorded_at            TIMESTAMP,
    reversed_at            TIMESTAMP,
    CONSTRAINT pk_liability PRIMARY KEY (id),
    CONSTRAINT fk_liability_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id)
);

CREATE INDEX idx_liability_dispute ON liability (dispute_id);

CREATE TABLE representments (
    id                 VARCHAR(64)  NOT NULL,
    dispute_id         VARCHAR(64)  NOT NULL,
    stage              VARCHAR(30),
    status             VARCHAR(20),
    network_reference  VARCHAR(128),
    evidence_count     INTEGER      NOT NULL DEFAULT 0,
    fee_minor          BIGINT,
    issuer_response    VARCHAR(20),
    narrative          VARCHAR(2000),
    submitted_by       VARCHAR(64),
    submitted_at       TIMESTAMP,
    decided_at         TIMESTAMP,
    created_at         TIMESTAMP,
    CONSTRAINT pk_representments PRIMARY KEY (id),
    CONSTRAINT fk_representments_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id)
);

CREATE INDEX idx_representment_dispute ON representments (dispute_id);

CREATE TABLE reason_code_catalog (
    network             VARCHAR(20)   NOT NULL,
    code                VARCHAR(20)   NOT NULL,
    category            VARCHAR(60),
    description         VARCHAR(200),
    win_rate            VARCHAR(10),
    required_evidence   VARCHAR(500),
    optional_evidence   VARCHAR(500),
    response_days       INTEGER,
    CONSTRAINT pk_reason_code_catalog PRIMARY KEY (network, code)
);
