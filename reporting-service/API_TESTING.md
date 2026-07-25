# Reporting Service (analytics) — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `reporting-service` makes no outbound calls to any other
microservice in this repo — it reads its own Postgres job-metadata store and a
(separate, external) ClickHouse OLAP replica. Fully testable standalone.

**Infrastructure:** Postgres (`reportingservicedb`). ClickHouse is only needed for
`/api/v1/query` dataset queries — not part of this repo's services, point
`CLICKHOUSE_URL` at one if you have it, otherwise expect that endpoint to fail.
SMTP (`SMTP_HOST`) is only needed for email report delivery. Config Server is
optional.

Base URL: `http://localhost:8096` (`server.port` in `application.yml`, overridable via `SERVER_PORT`)

Note: `spring.application.name` is `analytics` — the service is registered/discovered as
`analytics`, and its Java base package is `com.paymentprocessor.analytics`, even though the
module directory is `reporting-service`.

No authentication is required by any endpoint below.

Run with the `local` profile (`SPRING_PROFILES_ACTIVE=local`) to have Flyway also apply
`db/seed/V2__seed_sample_data.sql`, which seeds one `report_job` row:

- id `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`, merchant `MERCH-1001`, type `SETTLEMENT_SUMMARY`,
  format `PDF`, status `COMPLETED`

The GET examples below use this seeded id directly.

---

## AnalyticsController (`/api/v1/analytics`)

### GET /api/v1/analytics/status

Service metadata and capabilities.

Response (200):

```json
{
  "service": "analytics",
  "store": "clickhouse",
  "role": "read-only CDC replica",
  "reportTypes": [
    "CHARGEBACK_REPORT",
    "FEE_BREAKDOWN",
    "SETTLEMENT_SUMMARY",
    "TRANSACTION_DETAIL"
  ],
  "formats": [
    "CSV",
    "PDF",
    "EXCEL"
  ]
}
```

curl:

```bash
curl http://localhost:8096/api/v1/analytics/status
```

---

## QueryController (`/api/v1/query`)

### GET /api/v1/query/datasets

Lists the catalog-safe datasets and columns available for ad-hoc queries.

Response (200):

```json
{
  "transactions": {
    "dateColumn": "created_date",
    "columns": [
      {
        "name": "merchant_id",
        "label": "Merchant",
        "type": "STRING",
        "measure": false
      },
      {
        "name": "amount_minor",
        "label": "Amount (minor)",
        "type": "NUMBER",
        "measure": true
      }
    ]
  },
  "settlements": {
    "dateColumn": "settlement_date",
    "columns": [
      {
        "name": "merchant_id",
        "label": "Merchant",
        "type": "STRING",
        "measure": false
      },
      {
        "name": "net_minor",
        "label": "Net (minor)",
        "type": "NUMBER",
        "measure": true
      }
    ]
  }
}
```

curl:

```bash
curl http://localhost:8096/api/v1/query/datasets
```

### POST /api/v1/query/run

Validates and executes an ad-hoc query against the `DatasetCatalog`, returning a bounded preview.
`measures[].aggregation` is one of `SUM`, `COUNT`, `COUNT_DISTINCT`, `AVG`, `MIN`, `MAX`.
`filters[].op` is one of `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `IN`, `NOT_IN`, `LIKE`, `BETWEEN`.

Request body:

```json
{
  "dataset": "settlements",
  "dimensions": [
    "merchant_id",
    "currency"
  ],
  "measures": [
    {
      "column": "net_minor",
      "aggregation": "SUM"
    },
    {
      "column": "settlement_id",
      "aggregation": "COUNT"
    }
  ],
  "filters": [
    {
      "column": "merchant_id",
      "op": "EQ",
      "values": [
        "MERCH-1001"
      ]
    }
  ],
  "fromDate": "2026-07-01",
  "toDate": "2026-07-24",
  "orderBy": [
    {
      "by": "net_minor",
      "desc": true
    }
  ],
  "limit": 50,
  "offset": 0
}
```

Response (200):

```json
{
  "columns": [
    {
      "name": "merchant_id",
      "label": "Merchant",
      "type": "STRING"
    },
    {
      "name": "currency",
      "label": "Currency",
      "type": "STRING"
    },
    {
      "name": "sum_net_minor",
      "label": "Net (minor)",
      "type": "NUMBER"
    },
    {
      "name": "count_settlement_id",
      "label": "Settlement ID",
      "type": "NUMBER"
    }
  ],
  "rows": [
    [
      "MERCH-1001",
      "USD",
      4582300,
      128
    ]
  ]
}
```

curl:

```bash
curl -X POST http://localhost:8096/api/v1/query/run \
  -H "Content-Type: application/json" \
  -d '{
        "dataset": "settlements",
        "dimensions": ["merchant_id", "currency"],
        "measures": [{"column":"net_minor","aggregation":"SUM"},{"column":"settlement_id","aggregation":"COUNT"}],
        "filters": [{"column":"merchant_id","op":"EQ","values":["MERCH-1001"]}],
        "fromDate": "2026-07-01",
        "toDate": "2026-07-24",
        "orderBy": [{"by":"net_minor","desc":true}],
        "limit": 50,
        "offset": 0
      }'
```

---

## ReportController (`/api/v1/reports`)

### POST /api/v1/reports

Submits an async report job. Returns `202 Accepted` with the `QUEUED` job and a `Location` header;
poll `GET /api/v1/reports/{id}` for completion. `reportType` is one of `SETTLEMENT_SUMMARY`,
`CHARGEBACK_REPORT`, `TRANSACTION_DETAIL`, `FEE_BREAKDOWN`, `AD_HOC` (the latter requires `query`
instead of `fromDate`/`toDate`). `format` is one of `CSV`, `PDF`, `EXCEL`.

Request body:

```json
{
  "reportType": "SETTLEMENT_SUMMARY",
  "format": "PDF",
  "merchantId": "MERCH-1001",
  "fromDate": "2026-07-01",
  "toDate": "2026-07-24",
  "options": {
    "includeReserves": true
  },
  "notifyEmail": "jane@merchant1001.com"
}
```

Response (202 Accepted, `Location: /api/v1/reports/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb`):

```json
{
  "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "merchantId": "MERCH-1001",
  "reportType": "SETTLEMENT_SUMMARY",
  "format": "PDF",
  "status": "QUEUED",
  "rowCount": null,
  "sizeBytes": null,
  "errorMessage": null,
  "downloadUrl": null,
  "createdAt": "2026-07-25T09:00:00Z",
  "startedAt": null,
  "completedAt": null,
  "expiresAt": null
}
```

curl:

```bash
curl -X POST http://localhost:8096/api/v1/reports \
  -H "Content-Type: application/json" \
  -d '{
        "reportType": "SETTLEMENT_SUMMARY",
        "format": "PDF",
        "merchantId": "MERCH-1001",
        "fromDate": "2026-07-01",
        "toDate": "2026-07-24",
        "options": {"includeReserves": true},
        "notifyEmail": "jane@merchant1001.com"
      }'
```

### GET /api/v1/reports/{id}

Response (200, using the seeded job):

```json
{
  "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "merchantId": "MERCH-1001",
  "reportType": "SETTLEMENT_SUMMARY",
  "format": "PDF",
  "status": "COMPLETED",
  "rowCount": 128,
  "sizeBytes": 45678,
  "errorMessage": null,
  "downloadUrl": "http://localhost:8096/api/v1/reports/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/download",
  "createdAt": "2026-07-24T08:00:00Z",
  "startedAt": "2026-07-24T08:00:05Z",
  "completedAt": "2026-07-24T08:00:20Z",
  "expiresAt": "2026-08-23T08:00:20Z"
}
```

curl:

```bash
curl http://localhost:8096/api/v1/reports/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
```

### GET /api/v1/reports

Paginated list of a merchant's report jobs. `merchantId` is required.

curl:

```bash
curl "http://localhost:8096/api/v1/reports?merchantId=MERCH-1001&page=0&size=20"
```

### GET /api/v1/reports/{id}/download

Downloads the generated file. Returns `302 Found` with a presigned `Location` when storage is S3
(`analytics.storage.backend=s3`); otherwise streams the file directly with
`Content-Disposition: attachment`. Returns `409 Conflict` (via `ReportNotReadyException`) if the
job is not yet `COMPLETED`.

curl:

```bash
curl -L -o settlement-summary.pdf \
  http://localhost:8096/api/v1/reports/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/download
```

---

## ScheduleController (`/api/v1/schedules`)

### POST /api/v1/schedules

Creates a recurring report schedule. `cadence` is one of `DAILY`, `WEEKLY`, `MONTHLY`.

Request body:

```json
{
  "reportType": "SETTLEMENT_SUMMARY",
  "format": "CSV",
  "merchantId": "MERCH-1001",
  "cadence": "DAILY",
  "cron": "0 0 6 * * *",
  "timezone": "UTC",
  "options": {
    "includeReserves": true
  },
  "notifyEmail": "jane@merchant1001.com",
  "enabled": true
}
```

Response (201 Created, `Location: /api/v1/schedules/cccccccc-cccc-cccc-cccc-cccccccccccc`):

```json
{
  "id": "cccccccc-cccc-cccc-cccc-cccccccccccc",
  "merchantId": "MERCH-1001",
  "reportType": "SETTLEMENT_SUMMARY",
  "format": "CSV",
  "cadence": "DAILY",
  "cron": "0 0 6 * * *",
  "timezone": "UTC",
  "enabled": true,
  "nextRunAt": "2026-07-26T06:00:00Z",
  "lastRunAt": null,
  "createdAt": "2026-07-25T09:00:00Z"
}
```

curl:

```bash
curl -X POST http://localhost:8096/api/v1/schedules \
  -H "Content-Type: application/json" \
  -d '{
        "reportType": "SETTLEMENT_SUMMARY",
        "format": "CSV",
        "merchantId": "MERCH-1001",
        "cadence": "DAILY",
        "cron": "0 0 6 * * *",
        "timezone": "UTC",
        "options": {"includeReserves": true},
        "notifyEmail": "jane@merchant1001.com",
        "enabled": true
      }'
```

### GET /api/v1/schedules/{id}

curl:

```bash
curl http://localhost:8096/api/v1/schedules/cccccccc-cccc-cccc-cccc-cccccccccccc
```

### GET /api/v1/schedules

Paginated list of a merchant's schedules. `merchantId` is required.

curl:

```bash
curl "http://localhost:8096/api/v1/schedules?merchantId=MERCH-1001&page=0&size=20"
```

### PATCH /api/v1/schedules/{id}

Partial update; omitted/`null` fields are left unchanged.

Request body:

```json
{
  "enabled": false
}
```

Response (200): full `ScheduleResponse` reflecting the update.

curl:

```bash
curl -X PATCH http://localhost:8096/api/v1/schedules/cccccccc-cccc-cccc-cccc-cccccccccccc \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### DELETE /api/v1/schedules/{id}

Response: `204 No Content`.

curl:

```bash
curl -X DELETE http://localhost:8096/api/v1/schedules/cccccccc-cccc-cccc-cccc-cccccccccccc
```
