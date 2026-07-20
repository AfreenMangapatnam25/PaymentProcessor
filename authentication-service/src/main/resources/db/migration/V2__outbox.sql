-- ============================================================================
-- Transactional Outbox: domain events are written here in the SAME transaction
-- as the business change, then relayed to Kafka asynchronously (at-least-once).
-- ============================================================================

CREATE TABLE outbox_events (
    id             VARCHAR(36)  PRIMARY KEY,
    aggregate_type VARCHAR(40)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(60)  NOT NULL,
    topic          VARCHAR(120) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ
);

-- The relay scans PENDING rows oldest-first.
CREATE INDEX ix_outbox_pending ON outbox_events (status, created_at);
