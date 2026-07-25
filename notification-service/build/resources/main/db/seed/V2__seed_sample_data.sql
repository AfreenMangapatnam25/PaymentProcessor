-- Sample data for local/dev only. Loaded via the "local" Spring profile, which adds
-- classpath:db/seed to spring.flyway.locations (see application.yml). Never applied
-- in an environment that doesn't activate the "local" profile.
--
-- Seeds a versioned email template, an active webhook endpoint subscribed to
-- payment events, and one delivered message row referencing that template.
-- Fixed, deterministic ids so the values in API_TESTING.md are directly testable.
--
-- recipient_hash is bytea (SHA-256 of the raw recipient address); we never store
-- the raw email/phone. The value below is sha256('ops@acmeretail.example').

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
