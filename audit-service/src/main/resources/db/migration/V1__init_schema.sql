-- ============================================================================
-- Audit Service - initial schema (PostgreSQL)
-- Immutable, hash-chained, Merkle-sealed audit trail.
-- ============================================================================

-- Audit records: the query copy of the append-only hash chain -----------------
CREATE TABLE audit_records (
    id            VARCHAR(64)   PRIMARY KEY,
    seq           BIGINT        NOT NULL,
    ts            TIMESTAMPTZ,
    recorded_at   TIMESTAMPTZ,
    actor_type    VARCHAR(64),
    actor_id      VARCHAR(128),
    actor_ip      VARCHAR(64),
    actor_ua      VARCHAR(512),
    action        VARCHAR(128),
    resource_type VARCHAR(64),
    resource_id   VARCHAR(128),
    merchant_id   VARCHAR(64),
    before        JSONB,
    after         JSONB,
    request_id    VARCHAR(128),
    trace_id      VARCHAR(128),
    event_id      VARCHAR(128),
    prev_hash     VARCHAR(128),
    hash          VARCHAR(128),
    batch_id      VARCHAR(64),
    CONSTRAINT uk_audit_records_seq UNIQUE (seq),
    CONSTRAINT uk_audit_records_event_id UNIQUE (event_id)
);

-- Hot path: appending a record needs the previous record's hash by seq, and
-- range scans over seq drive both verification and daily batching.
CREATE INDEX idx_audit_records_seq ON audit_records (seq);

-- Query access patterns from the data model.
CREATE INDEX idx_audit_records_merchant_ts ON audit_records (merchant_id, ts DESC);
CREATE INDEX idx_audit_records_resource_ts ON audit_records (resource_type, resource_id, ts DESC);
CREATE INDEX idx_audit_records_action_ts ON audit_records (action, ts DESC);

-- Supports daily batch selection by ingest window.
CREATE INDEX idx_audit_records_recorded_at ON audit_records (recorded_at ASC);

-- Audit batches: metadata for sealed daily batches (legal copy lives in S3) ---
CREATE TABLE audit_batches (
    id              VARCHAR(64)   PRIMARY KEY,
    batch_date      DATE          NOT NULL,
    from_seq        BIGINT        NOT NULL DEFAULT 0,
    to_seq          BIGINT        NOT NULL DEFAULT 0,
    record_count    BIGINT        NOT NULL DEFAULT 0,
    root_hash       VARCHAR(128),
    signature       VARCHAR(1024),
    signing_key_id  VARCHAR(128),
    s3_bucket       VARCHAR(255),
    s3_key          VARCHAR(1024),
    s3_version_id   VARCHAR(255),
    retain_until    TIMESTAMPTZ,
    anchor_ref      VARCHAR(512),
    status          VARCHAR(16)   NOT NULL DEFAULT 'SEALING',
    created_at      TIMESTAMPTZ,
    sealed_at       TIMESTAMPTZ,
    CONSTRAINT uk_audit_batches_batch_date UNIQUE (batch_date)
);
CREATE INDEX idx_audit_batches_batch_date ON audit_batches (batch_date);

-- Chain state: singleton row tracking the head of the global hash chain -------
CREATE TABLE audit_chain_state (
    id          VARCHAR(32)   PRIMARY KEY,
    seq         BIGINT        NOT NULL DEFAULT 0,
    head_hash   VARCHAR(128),
    updated_at  TIMESTAMPTZ
);
