# Audit Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `audit-service` has no outbound HTTP calls to any other
service — it only *receives* audit events (via Kafka, or directly through its own
REST API) from other services. You can start and test it entirely on its own.

**Infrastructure:** Postgres (`auditservicedb`). Kafka is only needed if
`audit.kafka.enabled=true` (default) and you want to exercise event-driven
ingestion rather than calling the REST API directly — set `AUDIT_KAFKA_ENABLED=false`
to skip it. Config Server is optional (`optional:configserver:...import`).

Base URL: http://localhost:8095

Auth: All `/api/*` routes require header `X-Api-Key: <key>` (see `audit.security.api-keys`,
default `local-dev-key`). In the `local` Spring profile `audit.security.enabled=false`, so the
filter is registered but disabled and no key is required — send the header anyway if you plan to
run the same requests against a non-local profile. `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`
and `/error` are always exempt.

Seeded sample data (see `src/main/resources/db/seed/V3__seed_sample_data.sql`, local profile only)
provides 5 audit records (`aud_01J9ZQKR000000000000000001` … `...005`) and one sealed batch for
`2026-07-24`, so the GET examples below use those ids directly.

---

## AuditRecordController — `/api/v1/audit-records`

Audit records are append-only: no PUT/DELETE. Create is idempotent on `eventId` — replaying the
same `eventId` returns the existing record with `200 OK` instead of creating a duplicate (which
returns `201 Created`).

### POST /api/v1/audit-records

Auth: header `X-Api-Key`

Request body:

```json
{
  "eventId": "evt_9f2c9b9e-9d3e-4b0a-8a3e-1a2b3c4d5e6f",
  "ts": "2026-07-25T14:32:00Z",
  "actor": {
    "type": "USER",
    "id": "id_5a1c0e8e-2222-4b11-9a10-abcdef012345",
    "ip": "203.0.113.42",
    "ua": "Mozilla/5.0"
  },
  "action": "MERCHANT_UPDATED",
  "resource": {
    "type": "merchant",
    "id": "merch_7788"
  },
  "merchantId": "merch_7788",
  "before": {
    "status": "PENDING_REVIEW"
  },
  "after": {
    "status": "ACTIVE"
  },
  "requestId": "req_bb11cc22",
  "traceId": "trace_44ee55ff"
}
```

Response (`201 Created`, or `200 OK` on idempotent replay):

```json
{
  "id": "aud_01J9ZQKR000000000000000006",
  "seq": 6,
  "ts": "2026-07-25T14:32:00Z",
  "recordedAt": "2026-07-25T14:32:00.512Z",
  "actor": {
    "type": "USER",
    "id": "id_5a1c0e8e-2222-4b11-9a10-abcdef012345",
    "ip": "203.0.113.42",
    "ua": "Mozilla/5.0"
  },
  "action": "MERCHANT_UPDATED",
  "resource": {
    "type": "merchant",
    "id": "merch_7788"
  },
  "merchantId": "merch_7788",
  "before": {
    "status": "PENDING_REVIEW"
  },
  "after": {
    "status": "ACTIVE"
  },
  "requestId": "req_bb11cc22",
  "traceId": "trace_44ee55ff",
  "eventId": "evt_9f2c9b9e-9d3e-4b0a-8a3e-1a2b3c4d5e6f",
  "prevHash": "sha256:5b1e...c9a2",
  "hash": "sha256:7fa0...31de",
  "batchId": null
}
```

curl:

```bash
curl -X POST http://localhost:8095/api/v1/audit-records \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: local-dev-key" \
  -d '{
    "eventId": "evt_9f2c9b9e-9d3e-4b0a-8a3e-1a2b3c4d5e6f",
    "ts": "2026-07-25T14:32:00Z",
    "actor": { "type": "USER", "id": "id_5a1c0e8e-2222-4b11-9a10-abcdef012345", "ip": "203.0.113.42", "ua": "Mozilla/5.0" },
    "action": "MERCHANT_UPDATED",
    "resource": { "type": "merchant", "id": "merch_7788" },
    "merchantId": "merch_7788",
    "before": { "status": "PENDING_REVIEW" },
    "after": { "status": "ACTIVE" },
    "requestId": "req_bb11cc22",
    "traceId": "trace_44ee55ff"
  }'
```

### GET /api/v1/audit-records/{id}

Auth: header `X-Api-Key`

Response (`200 OK`):

```json
{
  "id": "aud_01J9ZQKR000000000000000001",
  "seq": 1,
  "ts": "2026-07-24T09:00:00Z",
  "recordedAt": "2026-07-24T09:00:00.100Z",
  "actor": {
    "type": "USER",
    "id": "id_5a1c0e8e-2222-4b11-9a10-abcdef012345",
    "ip": "203.0.113.10",
    "ua": "curl/8.4.0"
  },
  "action": "LOGIN_SUCCEEDED",
  "resource": {
    "type": "identity",
    "id": "id_5a1c0e8e-2222-4b11-9a10-abcdef012345"
  },
  "merchantId": "merch_7788",
  "before": null,
  "after": null,
  "requestId": "req_seed_0001",
  "traceId": "trace_seed_0001",
  "eventId": "evt_seed_0001",
  "prevHash": "sha256:0000000000000000000000000000000000000000000000000000000000000000",
  "hash": "sha256:aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111",
  "batchId": "batch_2026-07-24"
}
```

curl:

```bash
curl http://localhost:8095/api/v1/audit-records/aud_01J9ZQKR000000000000000001 \
  -H "X-Api-Key: local-dev-key"
```

### GET /api/v1/audit-records

Auth: header `X-Api-Key`

Query params (exactly one selector group is required):

- `merchantId` — search by merchant
- `resourceType` + `resourceId` — search by resource (both required together)
- `action` — search by action name
- `limit` — max results, default `50`

curl:

```bash
curl "http://localhost:8095/api/v1/audit-records?merchantId=merch_7788&limit=20" \
  -H "X-Api-Key: local-dev-key"
```

Response (`200 OK`): array of `AuditRecordResponse` objects, same shape as the GET-by-id example.

---

## BatchController — `/api/v1/audit/batches`

Daily legal-copy manifests. Sealing normally runs on a schedule (`audit.batch.cron`); the manual
seal endpoint is for backfills/recovery and is idempotent.

### GET /api/v1/audit/batches/{date}

Auth: header `X-Api-Key`
Path param: `date` — ISO date, e.g. `2026-07-24`.

Response (`200 OK`):

```json
{
  "id": "batch_2026-07-24",
  "batchDate": "2026-07-24",
  "fromSeq": 1,
  "toSeq": 5,
  "recordCount": 5,
  "rootHash": "sha256:dead beef00000000000000000000000000000000000000000000000000",
  "signature": "MEUCIQDx...==",
  "signingKeyId": "audit-batch-signer-v1",
  "s3Bucket": "payment-processor-audit-legal",
  "s3Key": "audit-batches/2026/07/24/batch_2026-07-24.json",
  "s3VersionId": "3sL4kqtJlcpXroDTDmJ+rmSpXd3dIbrHY+MTRCxf3vjVBH40Nrjfkd",
  "retainUntil": "2036-07-24T00:30:00Z",
  "anchorRef": "log:2026-07-24T00:30:05Z",
  "status": "SEALED",
  "createdAt": "2026-07-24T00:30:00Z",
  "sealedAt": "2026-07-24T00:30:05Z"
}
```

curl:

```bash
curl http://localhost:8095/api/v1/audit/batches/2026-07-24 \
  -H "X-Api-Key: local-dev-key"
```

### POST /api/v1/audit/batches/{date}/seal

Auth: header `X-Api-Key`
Path param: `date` — ISO date, e.g. `2026-07-25`.

Response (`200 OK`): `BatchResponse`, same shape as the GET example above (with `status: "SEALED"`).

curl:

```bash
curl -X POST http://localhost:8095/api/v1/audit/batches/2026-07-25/seal \
  -H "X-Api-Key: local-dev-key"
```

---

## VerificationController — `/api/v1/audit`

Recomputes the hash chain to prove the trail hasn't been tampered with.

### GET /api/v1/audit/verify

Auth: header `X-Api-Key`

Query params (both optional): `fromSeq` (default `1`), `toSeq` (default current head).

Response (`200 OK`):

```json
{
  "valid": true,
  "fromSeq": 1,
  "toSeq": 5,
  "recordsChecked": 5,
  "firstBrokenSeq": null,
  "detail": "chain verified"
}
```

curl:

```bash
curl "http://localhost:8095/api/v1/audit/verify?fromSeq=1&toSeq=5" \
  -H "X-Api-Key: local-dev-key"
```

### GET /api/v1/audit/head

Auth: header `X-Api-Key`

Response (`200 OK`):

```json
{
  "headSeq": 5
}
```

curl:

```bash
curl http://localhost:8095/api/v1/audit/head \
  -H "X-Api-Key: local-dev-key"
```

---

## Error format

All endpoints return this envelope on failure (see `api/dto/ApiError.java`):

```json
{
  "timestamp": "2026-07-25T14:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No audit record with id aud_missing",
  "path": "/api/v1/audit-records/aud_missing"
}
```
