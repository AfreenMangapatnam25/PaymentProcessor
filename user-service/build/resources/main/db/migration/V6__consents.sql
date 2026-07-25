-- =============================================================================
-- V6 :: consents
-- -----------------------------------------------------------------------------
-- GDPR consent state for a user or customer, one current row per (subject,
-- kind). granted + granted_at/revoked_at capture the current lawful basis;
-- policy_version records which privacy policy the subject agreed to. No PII
-- here, so no encryption needed.
--
-- Trade-off: this table holds CURRENT state only. If a full immutable audit
-- trail of every grant/revoke is required (often the case for regulators), add
-- an append-only consent_events history table in a later migration; the domain
-- is structured so that change is additive.
-- =============================================================================
CREATE TABLE consents (
    id             VARCHAR(40) NOT NULL,             -- con_<ULID>
    subject_type   VARCHAR(16) NOT NULL,             -- USER | CUSTOMER
    subject_id     VARCHAR(40) NOT NULL,
    consent_kind   VARCHAR(32) NOT NULL,
    granted        BOOLEAN     NOT NULL,
    source         VARCHAR(64),                      -- capture channel (web, api, import)
    policy_version VARCHAR(32),
    granted_at     TIMESTAMPTZ,
    revoked_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_consents PRIMARY KEY (id),
    CONSTRAINT ck_consents_subject_type CHECK (subject_type IN ('USER', 'CUSTOMER')),
    CONSTRAINT ck_consents_kind CHECK (
        consent_kind IN ('MARKETING', 'DATA_PROCESSING', 'THIRD_PARTY_SHARING', 'PROFILING')
    )
);

-- One current consent record per subject per kind.
CREATE UNIQUE INDEX uq_consents_subject_kind
    ON consents (subject_type, subject_id, consent_kind);

CREATE INDEX ix_consents_subject
    ON consents (subject_type, subject_id);

COMMENT ON TABLE consents IS 'Current GDPR consent state per subject and kind (no PII).';
