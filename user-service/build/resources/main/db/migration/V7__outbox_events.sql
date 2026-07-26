-- =============================================================================
-- V7 :: outbox_events
-- -----------------------------------------------------------------------------
-- Transactional outbox (rule 11). Domain changes and the events they emit are
-- written in the SAME database transaction, so an event can never be lost or
-- published for a rolled-back change. A relay polls PENDING rows and publishes
-- them to Kafka, then marks them PUBLISHED. Multiple relay replicas coordinate
-- with `SELECT ... FOR UPDATE SKIP LOCKED` on ix_outbox_due, so the poller
-- scales horizontally (rule 14) without double-publishing.
--
-- PRIVACY: payloads carry ids and non-PII attributes only. Raw PII (name,
-- email, DOB, address) must NOT be placed in an event payload (rules 12-13);
-- downstream services resolve PII through their own authorized paths.
-- =============================================================================
CREATE TABLE outbox_events (
    id              VARCHAR(40)  NOT NULL,           -- out_<ULID>
    aggregate_type  VARCHAR(64)  NOT NULL,           -- User | Customer | Address | Consent
    aggregate_id    VARCHAR(40)  NOT NULL,
    event_type      VARCHAR(96)  NOT NULL,           -- e.g. user.created.v1
    payload         JSONB        NOT NULL,
    headers         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    topic           VARCHAR(128) NOT NULL,
    partition_key   VARCHAR(128),                    -- usually aggregate_id (ordering)
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    last_error      TEXT,
    trace_id        VARCHAR(64),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

-- Relay hot path: due, unpublished rows in insertion (ULID) order.
CREATE INDEX ix_outbox_due
    ON outbox_events (next_attempt_at, id)
    WHERE status IN ('PENDING', 'FAILED');

-- Trace an aggregate's emitted events.
CREATE INDEX ix_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

COMMENT ON TABLE outbox_events IS 'Transactional outbox; relay publishes to Kafka with FOR UPDATE SKIP LOCKED.';
COMMENT ON COLUMN outbox_events.payload IS 'Event body: ids and non-PII only; never raw PII.';
