# notification-service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `notification-service` makes no outbound calls to any
other service — it's an ingestion/fan-out sink called *by* payment-service,
ledger-service, merchant-service, settlement-service, and dispute-service (via
`POST /api/events`), not the other way around. Fully testable standalone.

**Infrastructure:** Postgres (`notificationservicedb`). Kafka for inbound platform
event consumption (optional for direct REST testing). Config Server is optional.

Base URL: `http://localhost:8094` (`server.port` in `application.yml`, override with `SERVER_PORT`)

There is **no authentication enforced** on any endpoint in this service, including the SendGrid
and Twilio webhook receivers. In production those two receivers should verify the provider's
signature (SendGrid Ed25519 signature header, Twilio `X-Twilio-Signature`) before trusting the
payload — the controller code explicitly notes this is not implemented yet.

To exercise the examples below against a local database with realistic data already present, run
the service with the `local` Spring profile (`SPRING_PROFILES_ACTIVE=local`), which loads
`db/seed/V2__seed_sample_data.sql` in addition to the base schema migration. That seed creates:

| Entity                                                             | Fixed id                                |
|--------------------------------------------------------------------|-----------------------------------------|
| Template (`payment.succeeded`, email, en-US, v1)                   | `tmpl_00000000000000000000000000000001` |
| Webhook endpoint (merchant `11111111-1111-1111-1111-111111111111`) | `we_00000000000000000000000000000001`   |
| Message (email, delivered, via the template above)                 | `msg_00000000000000000000000000000001`  |

---

## Events — `EventController` (`/api/events`)

Ingestion endpoint fed by other services (payment-service, ledger-service, merchant-service,
settlement-service, dispute-service). Every ingested event is fanned out to subscribed, active
webhook endpoints as pending deliveries in the same transaction.

### POST /api/events

Request body — `id` is optional; supply it for idempotent retried publishes:

```json
{
  "id": "evt_00000000000000000000000000000001",
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "type": "payment.succeeded",
  "aggregateType": "payment_intent",
  "aggregateId": "pi_11111111111111111111111111111111",
  "apiVersion": "2026-01-01",
  "payload": {
    "amountMinor": 250000,
    "currency": "USD",
    "customerId": "cust_1001"
  }
}
```

Response (201 Created):

```json
{
  "id": "evt_00000000000000000000000000000001",
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "type": "payment.succeeded",
  "aggregateType": "payment_intent",
  "aggregateId": "pi_11111111111111111111111111111111",
  "apiVersion": "2026-01-01",
  "payload": {
    "amountMinor": 250000,
    "currency": "USD",
    "customerId": "cust_1001"
  },
  "sequence": 1,
  "createdAt": "2026-07-25T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8094/api/events \
  -H "Content-Type: application/json" \
  -d '{"id":"evt_00000000000000000000000000000001","merchantId":"11111111-1111-1111-1111-111111111111","type":"payment.succeeded","aggregateType":"payment_intent","aggregateId":"pi_11111111111111111111111111111111","apiVersion":"2026-01-01","payload":{"amountMinor":250000,"currency":"USD","customerId":"cust_1001"}}'
```

### GET /api/events

Query params: `merchantId` (required), `limit` (optional, default 50).

```bash
curl "http://localhost:8094/api/events?merchantId=11111111-1111-1111-1111-111111111111&limit=50"
```

### GET /api/events/{id}

```bash
curl http://localhost:8094/api/events/evt_00000000000000000000000000000001
```

---

## Messages — `MessageController` (`/api/messages`)

### POST /api/messages

Renders the requested template and sends over email or SMS, honoring the suppression list.

Request body:

```json
{
  "channel": "email",
  "recipient": "ops@acmeretail.example",
  "templateKey": "payment.succeeded",
  "locale": "en-US",
  "variables": {
    "customerName": "Jane Doe",
    "amount": "2500.00",
    "currency": "USD",
    "orderId": "ORD-90210"
  }
}
```

Response (201 Created):

```json
{
  "id": "msg_00000000000000000000000000000002",
  "channel": "email",
  "templateId": "tmpl_00000000000000000000000000000001",
  "locale": "en-US",
  "status": "sent",
  "provider": "sendgrid",
  "providerRef": "sg_msg_abc123",
  "sentAt": "2026-07-25T10:00:00Z",
  "createdAt": "2026-07-25T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8094/api/messages \
  -H "Content-Type: application/json" \
  -d '{"channel":"email","recipient":"ops@acmeretail.example","templateKey":"payment.succeeded","locale":"en-US","variables":{"customerName":"Jane Doe","amount":"2500.00","currency":"USD","orderId":"ORD-90210"}}'
```

### GET /api/messages

Query params: `channel` + `recipient` (both required together to filter by recipient), `limit`
(default 50). Without `channel`+`recipient`, returns the most recent messages.

```bash
curl "http://localhost:8094/api/messages?limit=50"
curl "http://localhost:8094/api/messages?channel=email&recipient=ops@acmeretail.example&limit=50"
```

### GET /api/messages/{id}

```bash
curl http://localhost:8094/api/messages/msg_00000000000000000000000000000001
```

Response (200):

```json
{
  "id": "msg_00000000000000000000000000000001",
  "channel": "email",
  "templateId": "tmpl_00000000000000000000000000000001",
  "locale": "en-US",
  "status": "delivered",
  "provider": "sendgrid",
  "providerRef": "sg_msg_seed_0001",
  "sentAt": "2026-07-25T10:00:00Z",
  "createdAt": "2026-07-25T10:00:00Z"
}
```

---

## Suppressions — `SuppressionController` (`/api/suppressions`)

### GET /api/suppressions

```bash
curl http://localhost:8094/api/suppressions
```

### POST /api/suppressions

Request body (`reason`: `bounce`, `complaint`, `unsubscribe`):

```json
{
  "channel": "email",
  "recipient": "bounced@example.com",
  "reason": "bounce"
}
```

Response (201):

```json
{
  "channel": "email",
  "reason": "bounce",
  "createdAt": "2026-07-25T10:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8094/api/suppressions \
  -H "Content-Type: application/json" \
  -d '{"channel":"email","recipient":"bounced@example.com","reason":"bounce"}'
```

### GET /api/suppressions/check

Query params: `channel`, `recipient`.

```bash
curl "http://localhost:8094/api/suppressions/check?channel=email&recipient=bounced@example.com"
```

Response (200):

```json
{
  "suppressed": true
}
```

### POST /api/suppressions/webhooks/sendgrid

Unauthenticated SendGrid Event Webhook receiver. **No signature verification implemented** — do
not expose this publicly without adding it. Body is a JSON array of SendGrid event objects.

```json
[
  {
    "email": "bounced@example.com",
    "event": "bounce",
    "reason": "mailbox full"
  }
]
```

curl:

```bash
curl -X POST http://localhost:8094/api/suppressions/webhooks/sendgrid \
  -H "Content-Type: application/json" \
  -d '[{"email":"bounced@example.com","event":"bounce","reason":"mailbox full"}]'
```

### POST /api/suppressions/webhooks/twilio

Unauthenticated Twilio status callback receiver, `application/x-www-form-urlencoded`. **No
signature verification implemented.**
curl:

```bash
curl -X POST http://localhost:8094/api/suppressions/webhooks/twilio \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "To=%2B14155550123&MessageStatus=failed"
```

---

## Templates — `TemplateController` (`/api/templates`)

Templates are append-only versioned rows keyed by `(key, channel, locale)`; POST always creates a
new version rather than mutating an existing one.

### GET /api/templates

```bash
curl http://localhost:8094/api/templates
```

Response (200):

```json
[
  {
    "id": "tmpl_00000000000000000000000000000001",
    "key": "payment.succeeded",
    "channel": "email",
    "locale": "en-US",
    "subject": "Your payment of {{amount}} {{currency}} was successful",
    "body": "Hi {{customerName}}, we have received your payment of {{amount}} {{currency}} for order {{orderId}}. Thank you for your business.",
    "version": 1,
    "createdAt": "2026-07-25T10:00:00Z"
  }
]
```

### POST /api/templates

Request body:

```json
{
  "key": "payment.succeeded",
  "channel": "email",
  "locale": "en-US",
  "subject": "Payment received - {{amount}} {{currency}}",
  "body": "Hi {{customerName}}, your payment for order {{orderId}} was received."
}
```

Response (201) — creates version 2 of this `(key, channel, locale)`:

```json
{
  "id": "tmpl_00000000000000000000000000000002",
  "key": "payment.succeeded",
  "channel": "email",
  "locale": "en-US",
  "subject": "Payment received - {{amount}} {{currency}}",
  "body": "Hi {{customerName}}, your payment for order {{orderId}} was received.",
  "version": 2,
  "createdAt": "2026-07-25T10:05:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8094/api/templates \
  -H "Content-Type: application/json" \
  -d '{"key":"payment.succeeded","channel":"email","locale":"en-US","subject":"Payment received - {{amount}} {{currency}}","body":"Hi {{customerName}}, your payment for order {{orderId}} was received."}'
```

### GET /api/templates/{key}/{channel}

All versions for a key+channel across locales.

```bash
curl http://localhost:8094/api/templates/payment.succeeded/email
```

### GET /api/templates/{key}/{channel}/{locale}

Latest version for key+channel+locale.

```bash
curl http://localhost:8094/api/templates/payment.succeeded/email/en-US
```

### GET /api/templates/{key}/{channel}/{locale}/{version}

```bash
curl http://localhost:8094/api/templates/payment.succeeded/email/en-US/1
```

### POST /api/templates/preview

Renders the latest version of a template against sample variables without sending or persisting
anything.

Request body:

```json
{
  "key": "payment.succeeded",
  "channel": "email",
  "locale": "en-US",
  "variables": {
    "customerName": "Jane Doe",
    "amount": "2500.00",
    "currency": "USD",
    "orderId": "ORD-90210"
  }
}
```

Response (200):

```json
{
  "subject": "Your payment of 2500.00 USD was successful",
  "body": "Hi Jane Doe, we have received your payment of 2500.00 USD for order ORD-90210. Thank you for your business."
}
```

curl:

```bash
curl -X POST http://localhost:8094/api/templates/preview \
  -H "Content-Type: application/json" \
  -d '{"key":"payment.succeeded","channel":"email","locale":"en-US","variables":{"customerName":"Jane Doe","amount":"2500.00","currency":"USD","orderId":"ORD-90210"}}'
```

---

## Webhook Deliveries — `WebhookDeliveryController` (`/api/webhook-deliveries`)

Read-only view over delivery attempts plus an operator-triggered retry.

### GET /api/webhook-deliveries

Query param: `endpointId` (optional).

```bash
curl "http://localhost:8094/api/webhook-deliveries?endpointId=we_00000000000000000000000000000001"
```

Response (200):

```json
[
  {
    "id": 1,
    "endpointId": "we_00000000000000000000000000000001",
    "eventId": "evt_00000000000000000000000000000001",
    "attempt": 1,
    "status": "delivered",
    "nextRetryAt": null,
    "responseCode": 200,
    "responseMs": 145,
    "error": null,
    "createdAt": "2026-07-25T10:00:05Z"
  }
]
```

### GET /api/webhook-deliveries/{id}

`id` is a numeric (`Long`) delivery id.

```bash
curl http://localhost:8094/api/webhook-deliveries/1
```

### POST /api/webhook-deliveries/{id}/retry

Operator-triggered resend of a failed/dead delivery.

```bash
curl -X POST http://localhost:8094/api/webhook-deliveries/1/retry
```

---

## Webhook Endpoints — `WebhookEndpointController` (`/api/webhook-endpoints`)

### GET /api/webhook-endpoints

Query param: `merchantId` (optional).

```bash
curl "http://localhost:8094/api/webhook-endpoints?merchantId=11111111-1111-1111-1111-111111111111"
```

Response (200):

```json
[
  {
    "id": "we_00000000000000000000000000000001",
    "merchantId": "11111111-1111-1111-1111-111111111111",
    "url": "https://webhooks.acmeretail.example/notifications",
    "secretRef": "env:NOTIFICATION_WEBHOOK_SECRET_ACME",
    "subscribedTypes": [
      "payment.succeeded",
      "payment.failed",
      "refund.processed"
    ],
    "apiVersion": "2026-01-01",
    "status": "active",
    "consecutiveFailures": 0,
    "createdAt": "2026-07-25T10:00:00Z"
  }
]
```

### POST /api/webhook-endpoints

Request body:

```json
{
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "url": "https://webhooks.acmeretail.example/notifications-v2",
  "subscribedTypes": [
    "payment.succeeded",
    "payment.failed"
  ],
  "apiVersion": "2026-01-01"
}
```

curl:

```bash
curl -X POST http://localhost:8094/api/webhook-endpoints \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"11111111-1111-1111-1111-111111111111","url":"https://webhooks.acmeretail.example/notifications-v2","subscribedTypes":["payment.succeeded","payment.failed"],"apiVersion":"2026-01-01"}'
```

### GET /api/webhook-endpoints/{id}

```bash
curl http://localhost:8094/api/webhook-endpoints/we_00000000000000000000000000000001
```

### PUT /api/webhook-endpoints/{id}

Same body shape as POST.

```bash
curl -X PUT http://localhost:8094/api/webhook-endpoints/we_00000000000000000000000000000001 \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"11111111-1111-1111-1111-111111111111","url":"https://webhooks.acmeretail.example/notifications","subscribedTypes":["payment.succeeded","payment.failed","refund.processed","chargeback.received"],"apiVersion":"2026-01-01"}'
```

### POST /api/webhook-endpoints/{id}/disable

```bash
curl -X POST http://localhost:8094/api/webhook-endpoints/we_00000000000000000000000000000001/disable
```

### POST /api/webhook-endpoints/{id}/enable

```bash
curl -X POST http://localhost:8094/api/webhook-endpoints/we_00000000000000000000000000000001/enable
```

### DELETE /api/webhook-endpoints/{id}

```bash
curl -X DELETE http://localhost:8094/api/webhook-endpoints/we_00000000000000000000000000000001
```

Response: `204 No Content`.
