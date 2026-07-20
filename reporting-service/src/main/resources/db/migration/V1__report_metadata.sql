-- Operational job-metadata store for the analytics reporting service (Postgres).
-- Note: this is the ONLY state the analytics service owns; all report *content*
-- is read from the ClickHouse OLAP replica.

CREATE TABLE scheduled_report (
    id                  UUID PRIMARY KEY,
    merchant_id         VARCHAR(64)  NOT NULL,
    report_type         VARCHAR(40)  NOT NULL,
    format              VARCHAR(10)  NOT NULL,
    cadence             VARCHAR(20)  NOT NULL,
    cron                VARCHAR(120),
    timezone            VARCHAR(60)  NOT NULL DEFAULT 'UTC',
    parameters          JSONB,
    notify_email        VARCHAR(320),
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    next_run_at         TIMESTAMPTZ,
    last_run_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_sched_merchant     ON scheduled_report (merchant_id);
CREATE INDEX idx_sched_enabled_next ON scheduled_report (enabled, next_run_at);

CREATE TABLE report_job (
    id                  UUID PRIMARY KEY,
    merchant_id         VARCHAR(64)  NOT NULL,
    report_type         VARCHAR(40)  NOT NULL,
    format              VARCHAR(10)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    parameters          JSONB,
    requested_by        VARCHAR(120),
    notify_email        VARCHAR(320),
    storage_key         VARCHAR(512),
    row_count           BIGINT,
    size_bytes          BIGINT,
    error_message       VARCHAR(2000),
    scheduled_report_id UUID REFERENCES scheduled_report (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_report_job_merchant ON report_job (merchant_id);
CREATE INDEX idx_report_job_status   ON report_job (status);
CREATE INDEX idx_report_job_created  ON report_job (created_at);
