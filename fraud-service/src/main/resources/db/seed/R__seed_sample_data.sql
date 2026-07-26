-- ============================================================================
-- REPEATABLE seed migration (Flyway "R__" prefix).
--
-- Why repeatable rather than versioned:
--   * Repeatable migrations always run AFTER every versioned migration, so this
--     file can never be "out of order". A versioned seed applied only under the
--     local profile broke as soon as the service had already been started without
--     it - Flyway then refused with
--     "Detected resolved migration not applied to database: <n>".
--   * Flyway re-applies a repeatable migration whenever its checksum changes, so
--     editing the sample data below just works on the next start; no manual
--     flyway_schema_history surgery needed.
--
-- Every statement is therefore written to be IDEMPOTENT (ON CONFLICT DO NOTHING),
-- because this file runs again on every checksum change and must never fail with
-- a duplicate-key error.
--
-- Only loaded when the "local" Spring profile is active (see application.yml:
-- spring.flyway.locations = classpath:db/migration,classpath:db/seed).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- devices
-- ----------------------------------------------------------------------------
INSERT INTO devices (id, fingerprint, merchant_id, first_seen, last_seen, metadata) VALUES
('11111111-1111-1111-1111-111111111111', 'fp_a1b2c3d4e5f6', 'mrc_9',  '2026-06-01T10:15:00Z', '2026-07-20T09:00:00Z', '{"os":"iOS 17","browser":"Safari"}'),
('11111111-1111-1111-1111-111111111112', 'fp_b2c3d4e5f6a1', 'mrc_9',  '2026-07-24T22:10:00Z', '2026-07-24T22:10:00Z', '{"os":"Android 14","browser":"Chrome"}'),
('11111111-1111-1111-1111-111111111113', 'fp_c3d4e5f6a1b2', 'mrc_12', '2026-05-10T08:00:00Z', '2026-07-19T14:32:00Z', '{"os":"Windows 11","browser":"Edge"}'),
('11111111-1111-1111-1111-111111111114', 'fp_d4e5f6a1b2c3', 'mrc_12', '2026-07-01T03:45:00Z', '2026-07-23T11:05:00Z', NULL)
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- cases
-- ----------------------------------------------------------------------------
INSERT INTO cases (id, merchant_id, intent_id, status, assignee, created_at) VALUES
('22222222-2222-2222-2222-222222222221', 'mrc_9',  'pi_1001', 'OPEN',        'analyst.jane',  '2026-07-20T09:05:00Z'),
('22222222-2222-2222-2222-222222222222', 'mrc_9',  'pi_1002', 'IN_REVIEW',   'analyst.mike',  '2026-07-21T13:22:00Z'),
('22222222-2222-2222-2222-222222222223', 'mrc_12', 'pi_1003', 'ESCALATED',   'analyst.priya', '2026-07-22T16:40:00Z'),
('22222222-2222-2222-2222-222222222224', 'mrc_12', 'pi_1004', 'CLOSED',      'analyst.jane',  '2026-07-18T08:12:00Z')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- lists
-- ----------------------------------------------------------------------------
INSERT INTO lists (id, merchant_id, list_kind, attribute, value, reason, expires_at) VALUES
('33333333-3333-3333-3333-333333333331', 'mrc_9',  'BLACKLIST', 'CARD',   '411111', 'Repeated chargebacks',        '2027-01-01T00:00:00Z'),
('33333333-3333-3333-3333-333333333332', 'mrc_9',  'WHITELIST', 'USER',   'usr_500', 'VIP long-standing customer',  NULL),
('33333333-3333-3333-3333-333333333333', 'mrc_12', 'BLACKLIST', 'IP',     '203.0.113.7', 'Known proxy/fraud ring',   '2026-12-31T00:00:00Z'),
('33333333-3333-3333-3333-333333333334', 'mrc_12', 'BLACKLIST', 'EMAIL_DOMAIN', 'tempmail.example', 'Disposable email domain', NULL)
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- model_registry
-- ----------------------------------------------------------------------------
INSERT INTO model_registry (id, name, version, status, created_at) VALUES
('44444444-4444-4444-4444-444444444441', 'fraud-xgb-core',      '2.3.0', 'ACTIVE',     '2026-05-01T00:00:00Z'),
('44444444-4444-4444-4444-444444444442', 'fraud-xgb-core',      '2.2.0', 'RETIRED',    '2026-02-01T00:00:00Z'),
('44444444-4444-4444-4444-444444444443', 'fraud-lightgbm-beta', '0.9.1', 'SHADOW',     '2026-07-10T00:00:00Z')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- risk_assessments
-- ----------------------------------------------------------------------------
INSERT INTO risk_assessments (id, intent_id, merchant_id, score, decision, triggered_rules, features, model, latency_ms, created_at) VALUES
('55555555-5555-5555-5555-555555555551', 'pi_1001', 'mrc_9',  82.50, 'REVIEW',  '["velocity_card_1h","device_new"]', '{"amount":5400.00,"ipCountry":"NG"}', 'fraud-xgb-core', 45, '2026-07-20T09:00:12Z'),
('55555555-5555-5555-5555-555555555552', 'pi_1002', 'mrc_9',  15.00, 'APPROVE', '[]',                                 '{"amount":42.10,"ipCountry":"US"}',   'fraud-xgb-core', 21, '2026-07-21T13:20:03Z'),
('55555555-5555-5555-5555-555555555553', 'pi_1003', 'mrc_12', 96.00, 'ESCALATE','["sanctions_hit","blacklist_ip"]',  '{"amount":12000.00,"ipCountry":"IR"}','fraud-xgb-core', 60, '2026-07-22T16:39:50Z'),
('55555555-5555-5555-5555-555555555554', 'pi_1004', 'mrc_12', 34.00, 'CHALLENGE','["device_new"]',                   '{"amount":250.00,"ipCountry":"GB"}',  'fraud-xgb-core', 30, '2026-07-18T08:10:44Z')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- rules
-- ----------------------------------------------------------------------------
INSERT INTO rules (id, scope, name, expr, action, priority, enabled, version) VALUES
('66666666-6666-6666-6666-666666666661', 'GLOBAL', 'high_amount_review',   'amount >= 5000',                      'REVIEW',   10, true,  1),
('66666666-6666-6666-6666-666666666662', 'GLOBAL', 'sanctioned_country',   'sanctionedCountry == true',           'ESCALATE', 5,  true,  1),
('66666666-6666-6666-6666-666666666663', 'MERCHANT','device_new_challenge','deviceNew == true',                   'CHALLENGE',20, true,  2),
('66666666-6666-6666-6666-666666666664', 'GLOBAL', 'disposable_email',     'disposableEmail == true',             'SCORE',    30, false, 1)
ON CONFLICT DO NOTHING;
