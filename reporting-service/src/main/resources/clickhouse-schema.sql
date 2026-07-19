-- =====================================================================
-- analytics — ClickHouse OLAP star schema (owns nothing; fed by
-- Debezium -> Kafka -> ClickHouse from payment/ledger/merchant/
-- settlement/dispute services). Reports are OLAP scans over this replica.
-- =====================================================================

-- ---------- Facts ----------

CREATE TABLE IF NOT EXISTS fact_payments (
  intent_id String, merchant_id LowCardinality(String),
  created_date Date, created_at DateTime64(3),
  amount_minor Int64, currency LowCardinality(FixedString(3)),
  status LowCardinality(String), connector LowCardinality(String),
  card_brand LowCardinality(String), issuer_country LowCardinality(FixedString(2)),
  is_3ds UInt8, risk_score Float32,
  fee_minor Int64, interchange_minor Int64,
  decline_code LowCardinality(String)
) ENGINE = MergeTree
PARTITION BY toYYYYMM(created_date)
ORDER BY (merchant_id, created_date, intent_id);

CREATE TABLE IF NOT EXISTS fact_ledger_entries (
  entry_id String, merchant_id LowCardinality(String),
  posted_date Date, posted_at DateTime64(3),
  account LowCardinality(String), direction Enum8('DEBIT' = 1, 'CREDIT' = 2),
  amount_minor Int64, currency LowCardinality(FixedString(3)),
  reference_type LowCardinality(String), reference_id String
) ENGINE = MergeTree
PARTITION BY toYYYYMM(posted_date)
ORDER BY (merchant_id, posted_date, entry_id);

CREATE TABLE IF NOT EXISTS fact_settlements (
  settlement_id String, merchant_id LowCardinality(String),
  settlement_date Date, settled_at DateTime64(3),
  currency LowCardinality(FixedString(3)),
  gross_minor Int64, fee_minor Int64, refund_minor Int64,
  chargeback_minor Int64, adjustment_minor Int64, net_minor Int64,
  txn_count UInt32, status LowCardinality(String), payout_id String
) ENGINE = MergeTree
PARTITION BY toYYYYMM(settlement_date)
ORDER BY (merchant_id, settlement_date, settlement_id);

CREATE TABLE IF NOT EXISTS fact_disputes (
  dispute_id String, merchant_id LowCardinality(String),
  intent_id String, opened_date Date, opened_at DateTime64(3),
  due_at DateTime64(3), resolved_at Nullable(DateTime64(3)),
  amount_minor Int64, currency LowCardinality(FixedString(3)),
  reason_code LowCardinality(String), category LowCardinality(String),
  status LowCardinality(String), outcome LowCardinality(String),
  card_brand LowCardinality(String)
) ENGINE = MergeTree
PARTITION BY toYYYYMM(opened_date)
ORDER BY (merchant_id, opened_date, dispute_id);

-- ---------- Dimensions ----------

CREATE TABLE IF NOT EXISTS dim_merchant (
  merchant_id LowCardinality(String), legal_name String,
  mcc LowCardinality(String), country LowCardinality(FixedString(2)),
  default_currency LowCardinality(FixedString(3)), status LowCardinality(String),
  updated_at DateTime64(3)
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY merchant_id;

CREATE TABLE IF NOT EXISTS dim_bin (
  bin FixedString(8), card_brand LowCardinality(String),
  card_type LowCardinality(String), issuer_name String,
  issuer_country LowCardinality(FixedString(2)), is_prepaid UInt8
) ENGINE = ReplacingMergeTree
ORDER BY bin;

CREATE TABLE IF NOT EXISTS dim_time (
  date Date, year UInt16, quarter UInt8, month UInt8,
  day UInt8, week UInt8, day_of_week UInt8, is_weekend UInt8
) ENGINE = MergeTree
ORDER BY date;
