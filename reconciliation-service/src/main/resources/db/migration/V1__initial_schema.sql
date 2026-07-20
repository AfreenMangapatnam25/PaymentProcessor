-- =============================================================================
-- Reconciliation Service - initial schema
-- =============================================================================

CREATE TABLE recon_run (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID           NOT NULL UNIQUE,
    recon_type              VARCHAR(32)    NOT NULL,
    channel                 VARCHAR(128)   NOT NULL,
    account_ref             VARCHAR(128),
    business_date           DATE           NOT NULL,
    currency                VARCHAR(3),
    status                  VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    total_internal          BIGINT         NOT NULL DEFAULT 0,
    total_external          BIGINT         NOT NULL DEFAULT 0,
    matched_count           BIGINT         NOT NULL DEFAULT 0,
    mismatched_count        BIGINT         NOT NULL DEFAULT 0,
    missing_internal_count  BIGINT         NOT NULL DEFAULT 0,
    missing_external_count  BIGINT         NOT NULL DEFAULT 0,
    duplicate_count         BIGINT         NOT NULL DEFAULT 0,
    exception_count         BIGINT         NOT NULL DEFAULT 0,
    matched_amount          NUMERIC(20,4)  DEFAULT 0,
    match_rate              NUMERIC(6,4),
    triggered_by            VARCHAR(128),
    failure_reason          VARCHAR(1024),
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ,
    version                 BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_recon_run_type_date ON recon_run (recon_type, business_date);
CREATE INDEX idx_recon_run_status    ON recon_run (status);

CREATE TABLE recon_record (
    id                   BIGSERIAL PRIMARY KEY,
    recon_run_id         BIGINT         NOT NULL REFERENCES recon_run (id),
    source               VARCHAR(16)    NOT NULL,
    source_system        VARCHAR(64),
    external_reference   VARCHAR(128),
    arn                  VARCHAR(64),
    internal_payment_id  VARCHAR(64),
    amount               NUMERIC(20,4)  NOT NULL,
    fee_amount           NUMERIC(20,4),
    currency             VARCHAR(3)     NOT NULL,
    transaction_date     DATE           NOT NULL,
    value_date           DATE,
    merchant_id          VARCHAR(64),
    counterparty         VARCHAR(128),
    card_bin             VARCHAR(8),
    card_last4           VARCHAR(4),
    transaction_status   VARCHAR(32),
    match_status         VARCHAR(16)    NOT NULL DEFAULT 'UNMATCHED',
    raw_payload          TEXT,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ,
    version              BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_recon_record_run          ON recon_record (recon_run_id);
CREATE INDEX idx_recon_record_run_ref      ON recon_record (recon_run_id, external_reference);
CREATE INDEX idx_recon_record_run_amount   ON recon_record (recon_run_id, amount);
CREATE INDEX idx_recon_record_match_status ON recon_record (recon_run_id, match_status);

CREATE TABLE recon_match (
    id                  BIGSERIAL PRIMARY KEY,
    recon_run_id        BIGINT         NOT NULL REFERENCES recon_run (id),
    internal_record_id  BIGINT         NOT NULL REFERENCES recon_record (id),
    external_record_id  BIGINT         NOT NULL REFERENCES recon_record (id),
    match_type          VARCHAR(16)    NOT NULL,
    match_rule          VARCHAR(64)    NOT NULL,
    confidence          NUMERIC(6,4)   NOT NULL,
    amount_variance     NUMERIC(20,4),
    date_variance_days  INTEGER,
    note                VARCHAR(512),
    manual              BOOLEAN        NOT NULL DEFAULT FALSE,
    matched_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_recon_match_run      ON recon_match (recon_run_id);
CREATE INDEX idx_recon_match_internal ON recon_match (internal_record_id);
CREATE INDEX idx_recon_match_external ON recon_match (external_record_id);

CREATE TABLE recon_exception (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID           NOT NULL UNIQUE,
    recon_run_id        BIGINT         NOT NULL REFERENCES recon_run (id),
    recon_record_id     BIGINT         REFERENCES recon_record (id),
    category            VARCHAR(32)    NOT NULL,
    severity_score      NUMERIC(6,2)   NOT NULL,
    severity_level      VARCHAR(16)    NOT NULL,
    status              VARCHAR(16)    NOT NULL DEFAULT 'OPEN',
    review_queue        VARCHAR(32),
    amount              NUMERIC(20,4),
    currency            VARCHAR(3),
    expected_amount     NUMERIC(20,4),
    actual_amount       NUMERIC(20,4),
    external_reference  VARCHAR(128),
    description         VARCHAR(1024),
    age_days            INTEGER        NOT NULL DEFAULT 0,
    detected_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    sla_due_at          TIMESTAMPTZ,
    assigned_to         VARCHAR(128),
    resolution_type     VARCHAR(24),
    resolution_note     VARCHAR(1024),
    resolved_by         VARCHAR(128),
    resolved_at         TIMESTAMPTZ,
    auto_resolved       BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_recon_exception_run      ON recon_exception (recon_run_id);
CREATE INDEX idx_recon_exception_status   ON recon_exception (status);
CREATE INDEX idx_recon_exception_severity ON recon_exception (severity_level);
CREATE INDEX idx_recon_exception_queue    ON recon_exception (review_queue);

CREATE TABLE recon_adjustment (
    id                      BIGSERIAL PRIMARY KEY,
    exception_id            BIGINT         NOT NULL REFERENCES recon_exception (id),
    adjustment_type         VARCHAR(24)    NOT NULL,
    amount                  NUMERIC(20,4)  NOT NULL,
    currency                VARCHAR(3)     NOT NULL,
    reason                  VARCHAR(1024)  NOT NULL,
    ledger_reference        VARCHAR(64),
    status                  VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    requires_dual_approval  BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by              VARCHAR(128)   NOT NULL,
    approved_by             VARCHAR(128),
    approved_at             TIMESTAMPTZ,
    posted_at               TIMESTAMPTZ,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ,
    version                 BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_recon_adjustment_exception ON recon_adjustment (exception_id);
CREATE INDEX idx_recon_adjustment_status    ON recon_adjustment (status);

CREATE TABLE bank_statement (
    id                  BIGSERIAL PRIMARY KEY,
    statement_reference VARCHAR(128)   NOT NULL UNIQUE,
    bank_name           VARCHAR(128)   NOT NULL,
    account_type        VARCHAR(24)    NOT NULL,
    account_number      VARCHAR(64)    NOT NULL,
    format              VARCHAR(16)    NOT NULL,
    currency            VARCHAR(3)     NOT NULL,
    statement_date      DATE           NOT NULL,
    opening_balance     NUMERIC(20,4),
    closing_balance     NUMERIC(20,4),
    record_count        INTEGER        NOT NULL DEFAULT 0,
    status              VARCHAR(16)    NOT NULL DEFAULT 'INGESTED',
    ingested_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_bank_statement_account_date ON bank_statement (account_number, statement_date);
