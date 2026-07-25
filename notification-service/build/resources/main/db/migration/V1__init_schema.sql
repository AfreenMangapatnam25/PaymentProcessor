-- notification-service schema
--
-- Deviations from the source spec, and why:
--
-- 1. `events` and `webhook_deliveries` are RANGE-partitioned by created_at.
--    Postgres requires that a partitioned table's PRIMARY KEY and every
--    UNIQUE constraint include the partition key column. The spec's
--    `events` table had `PRIMARY KEY (id)` and `UNIQUE (merchant_id,
--    sequence)`, neither of which would actually create on a partitioned
--    table. Both now include created_at, mirroring the pattern the spec
--    already used for webhook_deliveries
--    (`UNIQUE (endpoint_id, event_id, created_at)`).
--    Practical effect: uniqueness of (merchant_id, sequence) and of (id) is
--    enforced within a created_at value, which is fine here because both
--    are assigned at insert time in the same transaction as created_at.
--
-- 2. No physical FOREIGN KEY from webhook_deliveries.event_id -> events.id,
--    or from messages to events. Postgres cannot enforce a FK against a
--    partitioned table unless the referenced unique constraint contains the
--    partition key, which the referencing column doesn't have. This is a
--    standard trade-off for partitioned event/log tables: referential
--    integrity to `events` is enforced at the application layer
--    (EventIngestionService), not by the database. FKs that don't cross a
--    partition boundary (webhook_deliveries.endpoint_id -> webhook_endpoints,
--    messages.template_id -> templates) are still enforced by the DB.

CREATE TABLE events (
  id text NOT NULL,                        -- evt_...
  merchant_id text NOT NULL,
  type text NOT NULL,                       -- payment.succeeded | dispute.opened | ...
  aggregate_type text NOT NULL,
  aggregate_id text NOT NULL,
  api_version text NOT NULL,
  payload jsonb NOT NULL,
  sequence bigint NOT NULL,                 -- monotonic PER MERCHANT => ordered delivery
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (id, created_at),
  UNIQUE (merchant_id, sequence, created_at)
) PARTITION BY RANGE (created_at);

-- Fast "what's the next sequence for this merchant" / ordering-guard lookups.
CREATE INDEX idx_events_merchant_sequence ON events (merchant_id, sequence DESC);
CREATE INDEX idx_events_aggregate ON events (aggregate_type, aggregate_id);
CREATE INDEX idx_events_type ON events (merchant_id, type);

-- Catch-all so inserts never fail while a monthly partition is missing;
-- RetentionService keeps real monthly partitions ahead of the write path.
CREATE TABLE events_default PARTITION OF events DEFAULT;

-- Because events.id can't be a standalone unique/PK column on a partitioned
-- table (see note 1 above), event-id idempotency for ingestion (upstream
-- services may retry a publish with the same event id) is enforced here:
-- a small, unpartitioned table with a real UNIQUE(event_id). One extra
-- `INSERT ... ON CONFLICT DO NOTHING` before the real event insert tells
-- EventIngestionService whether this id has been seen before.
CREATE TABLE event_idempotency (
  event_id text PRIMARY KEY,
  event_created_at timestamptz NOT NULL
);

CREATE TABLE webhook_endpoints (
  id text PRIMARY KEY,
  merchant_id text NOT NULL,
  url text NOT NULL,
  secret_ref text NOT NULL,                 -- HMAC signing key reference in KMS
  subscribed_types text[] NOT NULL,
  api_version text NOT NULL,
  status text NOT NULL DEFAULT 'active',    -- active|disabled|auto_disabled
  consecutive_failures int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_endpoints_merchant ON webhook_endpoints (merchant_id);
CREATE INDEX idx_webhook_endpoints_status ON webhook_endpoints (status);
CREATE INDEX idx_webhook_endpoints_subscribed_types ON webhook_endpoints USING gin (subscribed_types);

CREATE TABLE webhook_deliveries (
  id bigserial NOT NULL,
  endpoint_id text NOT NULL REFERENCES webhook_endpoints (id),
  event_id text NOT NULL,
  attempt int NOT NULL DEFAULT 0,
  status text NOT NULL,                     -- pending|delivering|delivered|failed|dead
  next_retry_at timestamptz,
  response_code int,
  response_ms int,
  error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (id, created_at),
  UNIQUE (endpoint_id, event_id, created_at)    -- exactly one delivery row per event
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_webhook_deliveries_pending ON webhook_deliveries (next_retry_at) WHERE status = 'pending';
-- Ordering guard: "does this endpoint have an earlier, still-unresolved delivery?"
CREATE INDEX idx_webhook_deliveries_endpoint_event ON webhook_deliveries (endpoint_id, event_id);
CREATE INDEX idx_webhook_deliveries_endpoint_status ON webhook_deliveries (endpoint_id, status);

CREATE TABLE webhook_deliveries_default PARTITION OF webhook_deliveries DEFAULT;

CREATE TABLE templates (
  id text PRIMARY KEY,
  key text NOT NULL,
  channel text NOT NULL,
  locale text NOT NULL,
  subject text,
  body text NOT NULL,
  version int NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (key, channel, locale, version)
);

CREATE INDEX idx_templates_lookup ON templates (key, channel, locale, version DESC);

CREATE TABLE messages (
  id text PRIMARY KEY,
  channel text NOT NULL,                    -- email|sms|push
  template_id text REFERENCES templates (id),
  recipient_hash bytea NOT NULL,            -- hashed: don't duplicate PII here
  locale text,
  status text NOT NULL,
  provider text,
  provider_ref text,
  sent_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_recipient ON messages (channel, recipient_hash);
CREATE INDEX idx_messages_status ON messages (status, created_at);

CREATE TABLE suppressions (
  channel text NOT NULL,
  recipient_hash bytea NOT NULL,
  reason text NOT NULL,                     -- bounce|complaint|unsubscribe
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (channel, recipient_hash)
);

-- ---------------------------------------------------------------------------
-- Bootstrap partitions: current month +/- a small window so the service is
-- immediately usable. RetentionService (scheduled) creates future partitions
-- and drops partitions older than the 90-day retention window on an ongoing
-- basis; this migration just seeds enough to start.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  month_start date;
  month_end date;
  i int;
  suffix text;
BEGIN
  FOR i IN -1..2 LOOP
    month_start := date_trunc('month', now())::date + (i || ' month')::interval;
    month_end := month_start + interval '1 month';
    suffix := to_char(month_start, 'YYYY_MM');

    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS events_%s PARTITION OF events FOR VALUES FROM (%L) TO (%L)',
      suffix, month_start, month_end
    );
    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS webhook_deliveries_%s PARTITION OF webhook_deliveries FOR VALUES FROM (%L) TO (%L)',
      suffix, month_start, month_end
    );
  END LOOP;
END $$;
