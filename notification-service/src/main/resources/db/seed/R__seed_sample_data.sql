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

INSERT INTO templates (id, key, channel, locale, subject, body, version, created_at)
VALUES (
    'tmpl_00000000000000000000000000000001',
    'payment.succeeded',
    'email',
    'en-US',
    'Your payment of {{amount}} {{currency}} was successful',
    'Hi {{customerName}}, we have received your payment of {{amount}} {{currency}} for order {{orderId}}. Thank you for your business.',
    1,
    now()
)
ON CONFLICT (key, channel, locale, version) DO NOTHING;

INSERT INTO webhook_endpoints (
    id, merchant_id, url, secret_ref, subscribed_types, api_version, status, consecutive_failures, created_at
) VALUES (
    'we_00000000000000000000000000000001',
    '11111111-1111-1111-1111-111111111111',
    'https://webhooks.acmeretail.example/notifications',
    'env:NOTIFICATION_WEBHOOK_SECRET_ACME',
    ARRAY['payment.succeeded', 'payment.failed', 'refund.processed'],
    '2026-01-01',
    'active',
    0,
    now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO messages (
    id, channel, template_id, recipient_hash, locale, status, provider, provider_ref, sent_at, created_at
) VALUES (
    'msg_00000000000000000000000000000001',
    'email',
    'tmpl_00000000000000000000000000000001',
    decode('9dbf555aa7a3c698345f38de01a5b4113e6ec379c4d08ee3d956724abfb54c3a', 'hex'),
    'en-US',
    'delivered',
    'sendgrid',
    'sg_msg_seed_0001',
    now(),
    now()
)
ON CONFLICT (id) DO NOTHING;
