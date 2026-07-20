-- Merchant Service — initial schema.
-- Column names/types mirror the JPA entities (spring.jpa.hibernate.ddl-auto=validate).

CREATE TABLE merchant (
    id                  UUID PRIMARY KEY,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_reference  VARCHAR(40)  NOT NULL,
    legal_business_name VARCHAR(255) NOT NULL,
    trading_name        VARCHAR(255),
    registration_number VARCHAR(100),
    tax_id              VARCHAR(100),
    mcc                 VARCHAR(4),
    business_type       VARCHAR(40),
    website_url         VARCHAR(512),
    support_email       VARCHAR(255),
    support_phone       VARCHAR(40),
    country             VARCHAR(2),
    default_currency    VARCHAR(3),
    owner_user_id       UUID,
    status              VARCHAR(20) NOT NULL,
    kyb_status          VARCHAR(20) NOT NULL,
    pricing_plan        VARCHAR(20) NOT NULL,
    activated_at        TIMESTAMP(6) WITH TIME ZONE,
    suspended_at        TIMESTAMP(6) WITH TIME ZONE,
    suspension_reason   VARCHAR(512),
    terminated_at       TIMESTAMP(6) WITH TIME ZONE
);
CREATE UNIQUE INDEX ux_merchant_reference ON merchant (merchant_reference);
CREATE INDEX ix_merchant_status ON merchant (status);
CREATE INDEX ix_merchant_owner_user ON merchant (owner_user_id);

CREATE TABLE business_address (
    id           UUID PRIMARY KEY,
    version      BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id  UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    address_type VARCHAR(20) NOT NULL,
    line1        VARCHAR(255) NOT NULL,
    line2        VARCHAR(255),
    city         VARCHAR(120) NOT NULL,
    region       VARCHAR(120),
    postal_code  VARCHAR(20),
    country      VARCHAR(2) NOT NULL,
    is_primary   BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_address_merchant ON business_address (merchant_id);

CREATE TABLE settlement_account (
    id                       UUID PRIMARY KEY,
    version                  BIGINT NOT NULL DEFAULT 0,
    created_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id              UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    purpose                  VARCHAR(20) NOT NULL,
    account_holder_name      VARCHAR(255) NOT NULL,
    bank_name                VARCHAR(255),
    bank_code                VARCHAR(40),
    routing_number           VARCHAR(40),
    swift                    VARCHAR(20),
    iban_last4               VARCHAR(4),
    account_number_encrypted VARCHAR(1024) NOT NULL,
    account_number_last4     VARCHAR(4) NOT NULL,
    account_class            VARCHAR(20),
    currency                 VARCHAR(3) NOT NULL,
    country                  VARCHAR(2) NOT NULL,
    is_default               BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status      VARCHAR(20) NOT NULL
);
CREATE INDEX ix_settlement_merchant ON settlement_account (merchant_id);

CREATE TABLE beneficial_owner (
    id                   UUID PRIMARY KEY,
    version              BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id          UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    first_name           VARCHAR(120) NOT NULL,
    last_name            VARCHAR(120) NOT NULL,
    date_of_birth        DATE,
    email                VARCHAR(255),
    role                 VARCHAR(30) NOT NULL,
    ownership_percentage NUMERIC(5,2),
    nationality          VARCHAR(2),
    kyc_status           VARCHAR(20) NOT NULL
);
CREATE INDEX ix_owner_merchant ON beneficial_owner (merchant_id);

CREATE TABLE kyb_case (
    id                 UUID PRIMARY KEY,
    version            BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id        UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    status             VARCHAR(20) NOT NULL,
    external_reference VARCHAR(100),
    risk_score         INTEGER,
    sanctions_screened BOOLEAN NOT NULL DEFAULT FALSE,
    pep_screened       BOOLEAN NOT NULL DEFAULT FALSE,
    decision_reason    VARCHAR(1024),
    submitted_at       TIMESTAMP(6) WITH TIME ZONE,
    reviewed_at        TIMESTAMP(6) WITH TIME ZONE
);
CREATE INDEX ix_kybcase_merchant ON kyb_case (merchant_id);
CREATE INDEX ix_kybcase_status ON kyb_case (status);

CREATE TABLE kyb_document (
    id                UUID PRIMARY KEY,
    version           BIGINT NOT NULL DEFAULT 0,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id       UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    kyb_case_id       UUID REFERENCES kyb_case (id) ON DELETE SET NULL,
    document_type     VARCHAR(40) NOT NULL,
    file_name         VARCHAR(255),
    content_type      VARCHAR(120),
    storage_reference VARCHAR(512) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    expires_on        DATE
);
CREATE INDEX ix_kybdoc_merchant ON kyb_document (merchant_id);
CREATE INDEX ix_kybdoc_case ON kyb_document (kyb_case_id);

CREATE TABLE fee_configuration (
    id                       UUID PRIMARY KEY,
    version                  BIGINT NOT NULL DEFAULT 0,
    created_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id              UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    pricing_plan             VARCHAR(20) NOT NULL,
    transaction_fee_percent  NUMERIC(6,3),
    transaction_fee_fixed    NUMERIC(12,2),
    interchange_pass_through BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_platform_fee     NUMERIC(12,2),
    chargeback_fee           NUMERIC(12,2),
    refund_fee               NUMERIC(12,2),
    payout_fee               NUMERIC(12,2),
    currency                 VARCHAR(3) NOT NULL,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_fee_merchant ON fee_configuration (merchant_id);

CREATE TABLE payment_method_config (
    id            UUID PRIMARY KEY,
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id   UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    method_type   VARCHAR(20) NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    settings_json TEXT,
    CONSTRAINT ux_pmconfig_merchant_method UNIQUE (merchant_id, method_type)
);
CREATE INDEX ix_pmconfig_merchant ON payment_method_config (merchant_id);

CREATE TABLE webhook (
    id                UUID PRIMARY KEY,
    version           BIGINT NOT NULL DEFAULT 0,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id       UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    endpoint_url      VARCHAR(1024) NOT NULL,
    subscribed_events VARCHAR(1024) NOT NULL,
    secret_hash       VARCHAR(200) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries       INTEGER NOT NULL DEFAULT 5,
    timeout_seconds   INTEGER NOT NULL DEFAULT 10
);
CREATE INDEX ix_webhook_merchant ON webhook (merchant_id);

CREATE TABLE webhook_delivery_log (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    webhook_id      UUID NOT NULL REFERENCES webhook (id) ON DELETE CASCADE,
    merchant_id     UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    event_type      VARCHAR(60) NOT NULL,
    payload         TEXT,
    status          VARCHAR(20) NOT NULL,
    attempts        INTEGER NOT NULL DEFAULT 0,
    response_code   INTEGER,
    last_attempt_at TIMESTAMP(6) WITH TIME ZONE
);
CREATE INDEX ix_wdl_webhook ON webhook_delivery_log (webhook_id);
CREATE INDEX ix_wdl_status ON webhook_delivery_log (status);

CREATE TABLE api_key (
    id           UUID PRIMARY KEY,
    version      BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id  UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    key_id       VARCHAR(60) NOT NULL,
    secret_hash  VARCHAR(200) NOT NULL,
    key_type     VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    label        VARCHAR(120),
    ip_allowlist VARCHAR(1024),
    last_used_at TIMESTAMP(6) WITH TIME ZONE,
    revoked_at   TIMESTAMP(6) WITH TIME ZONE,
    expires_at   TIMESTAMP(6) WITH TIME ZONE
);
CREATE UNIQUE INDEX ux_apikey_key_id ON api_key (key_id);
CREATE INDEX ix_apikey_merchant ON api_key (merchant_id);

CREATE TABLE merchant_configuration (
    id                        UUID PRIMARY KEY,
    version                   BIGINT NOT NULL DEFAULT 0,
    created_at                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id               UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    capture_mode              VARCHAR(20) NOT NULL,
    enforce_3ds               BOOLEAN NOT NULL DEFAULT FALSE,
    velocity_limit_per_day    INTEGER,
    payout_schedule           VARCHAR(20) NOT NULL,
    minimum_payout_threshold  NUMERIC(12,2),
    instant_payout_eligible   BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_screening_level     VARCHAR(10) NOT NULL,
    chargeback_alert_threshold INTEGER,
    reserve_percentage        NUMERIC(5,2),
    notify_on_payout          BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_chargeback      BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_status_change   BOOLEAN NOT NULL DEFAULT TRUE,
    kyc_refresh_interval_days INTEGER
);
CREATE UNIQUE INDEX ux_config_merchant ON merchant_configuration (merchant_id);

CREATE TABLE merchant_branding (
    id                   UUID PRIMARY KEY,
    version              BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    merchant_id          UUID NOT NULL REFERENCES merchant (id) ON DELETE CASCADE,
    logo_url             VARCHAR(1024),
    primary_color        VARCHAR(9),
    secondary_color      VARCHAR(9),
    statement_descriptor VARCHAR(22),
    custom_domain        VARCHAR(255),
    email_template_ref   VARCHAR(255)
);
CREATE UNIQUE INDEX ux_branding_merchant ON merchant_branding (merchant_id);

CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(80) NOT NULL,
    topic          VARCHAR(200) NOT NULL,
    message_key    VARCHAR(100) NOT NULL,
    payload        TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    attempts       INTEGER NOT NULL DEFAULT 0,
    published_at   TIMESTAMP(6) WITH TIME ZONE,
    last_error     VARCHAR(2048)
);
CREATE INDEX ix_outbox_status_created ON outbox_event (status, created_at);
CREATE INDEX ix_outbox_aggregate ON outbox_event (aggregate_type, aggregate_id);
