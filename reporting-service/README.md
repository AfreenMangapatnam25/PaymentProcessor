# analytics — Reporting & Analytics Service

**Store:** ClickHouse (OLAP replica, owns nothing) · **Job metadata:** Postgres · **Files:** S3 / local

## What it is

A read-only reporting service over the ClickHouse OLAP replica that is fed by
Debezium → Kafka → ClickHouse from the payment, ledger, merchant, settlement and
dispute services. Reports are OLAP scans over 10⁹ rows — a columnar-store problem.
It **owns no business data**: every fact row is a projection of another service's
truth. The one thing it *does* own is operational job metadata (report requests,
status, schedules), which lives in Postgres.

**Reports never query OLTP.** All dashboards and exports read from ClickHouse.

## Core capabilities

- **Scheduled reports** — daily / weekly / monthly recurring definitions; a dispatcher
  materializes each into an async job covering the previous complete period.
- **Ad-hoc query builder** — catalog-safe queries over `transactions`, `settlements`,
  `fees` and `disputes`. Columns, filters, operators and aggregations are whitelisted;
  every value is bound as a parameter (no SQL injection surface).
- **Export formats** — CSV (streaming), Excel (POI SXSSF, streaming), PDF (OpenPDF).
- **Merchant report templates** — settlement summary, chargeback report, transaction
  detail, fee breakdown.
- **Async generation with notifications** — work runs on a bounded executor; on
  completion the service publishes a `report.completed` Kafka event and emails the
  requester a download link.

## Architecture

```
POST /api/v1/reports ──▶ ReportService ──▶ report_job (Postgres, QUEUED)
                                │
                                ▼  @Async (bounded pool)
                       ReportGenerationWorker
                     ┌──────────┼─────────────┐
              template/ad-hoc   exporter    storage (S3 / local)
              (ClickHouse RO)   (CSV/PDF/XLSX)      │
                                                    ▼
                              markCompleted ──▶ NotificationDispatcher
                                                 ├─ Kafka: report.completed
                                                 └─ Email: download link
```

Scheduling: `ScheduledReportDispatcher` polls `scheduled_report` every minute and
advances `next_run_at` (optimistic-locked so it is safe across instances).
`RetentionJob` expires completed reports and deletes their files nightly.

## Data model

- **ClickHouse (owns nothing)** — Kimball star schema: `fact_payments`,
  `fact_ledger_entries`, `fact_settlements`, `fact_disputes`, plus `dim_merchant`,
  `dim_bin`, `dim_time`. See `src/main/resources/clickhouse-schema.sql`.
- **Postgres (owned)** — `report_job` and `scheduled_report`, created by the Flyway
  migration in `src/main/resources/db/migration`.

## API

| Method                 | Path                            | Purpose                                          |
|------------------------|---------------------------------|--------------------------------------------------|
| `POST`                 | `/api/v1/reports`               | Submit an async report (202 + job)               |
| `GET`                  | `/api/v1/reports/{id}`          | Job status / metadata                            |
| `GET`                  | `/api/v1/reports?merchantId=`   | List a merchant's jobs (paged)                   |
| `GET`                  | `/api/v1/reports/{id}/download` | Download file (302 to presigned URL on S3)       |
| `POST`                 | `/api/v1/schedules`             | Create a recurring schedule                      |
| `GET`/`PATCH`/`DELETE` | `/api/v1/schedules/{id}`        | Manage a schedule                                |
| `GET`                  | `/api/v1/query/datasets`        | Discover queryable datasets & columns            |
| `POST`                 | `/api/v1/query/run`             | Validate + run an ad-hoc query (bounded preview) |
| `GET`                  | `/actuator/health`              | Liveness/readiness incl. ClickHouse check        |
| `GET`                  | `/swagger-ui.html`              | Interactive API docs                             |

### Example: submit a settlement summary as PDF

```json
POST /api/v1/reports
{
  "reportType": "SETTLEMENT_SUMMARY",
  "format": "PDF",
  "merchantId": "mrc_123",
  "fromDate": "2026-06-01",
  "toDate": "2026-06-30",
  "notifyEmail": "ops@merchant.com"
}
```

### Example: ad-hoc fee query

```json
POST /api/v1/query/run
{
  "dataset": "fees",
  "dimensions": [
    "connector",
    "card_brand"
  ],
  "measures": [
    {
      "column": "fee_minor",
      "aggregation": "SUM"
    }
  ],
  "filters": [
    {
      "column": "currency",
      "op": "EQ",
      "values": [
        "USD"
      ]
    }
  ],
  "fromDate": "2026-06-01",
  "toDate": "2026-06-30",
  "orderBy": [
    {
      "by": "sum_fee_minor",
      "desc": true
    }
  ],
  "limit": 100
}
```

## Running locally

```bash
docker compose up -d postgres clickhouse kafka mailhog   # dependencies
# load the OLAP schema:
clickhouse-client --query "CREATE DATABASE IF NOT EXISTS analytics"
clickhouse-client -d analytics < src/main/resources/clickhouse-schema.sql
./gradlew :reporting-service:bootRun
```

The service needs **both** Postgres (job/schedule metadata, Flyway-migrated) and
ClickHouse (report content, read-only) reachable to fully function; without
ClickHouse the `/actuator/health` check fails and report generation errors out.

Swagger: http://localhost:8096/swagger-ui.html · MailHog: http://localhost:8025

## Configuration

All config is in `src/main/resources/application.yml` and overridable via environment.

| Env                                                          | Default        | Notes              |
|--------------------------------------------------------------|----------------|--------------------|
| `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD`       | localhost      | Job metadata store |
| `CLICKHOUSE_URL` / `CLICKHOUSE_USER` / `CLICKHOUSE_PASSWORD` | localhost      | Read-only OLAP     |
| `KAFKA_BOOTSTRAP`                                            | localhost:9092 | Event bus          |
| `SMTP_HOST` / `SMTP_PORT`                                    | localhost:1025 | Email              |
| `STORAGE_BACKEND`                                            | `local`        | `local` or `s3`    |
| `STORAGE_S3_BUCKET` / `STORAGE_S3_ENDPOINT` / `AWS_REGION`   | —              | S3/MinIO           |

## Safety notes

- ClickHouse connection is opened with `readonly=2` and a bounded `max_result_rows`
  as defense-in-depth; the JDBC template also caps rows and query time.
- The ad-hoc builder never concatenates user input into SQL — identifiers come from
  `DatasetCatalog`, values are `?` placeholders.
- Report files are stored under `merchantId/yyyy/MM/jobId.ext`; local storage guards
  against path traversal, and per-merchant in-flight concurrency is capped.

## How it relates to other services

Fed by CDC from **payment-service**, **ledger-service**, **merchant-service**,
**settlement-service**, **dispute-service**. Publishes `report.completed` to Kafka
for downstream consumers (webhooks, merchant dashboards).

**gateway-service** proxies to it: `application.yml` routes `/api/v1/reports/**`,
`/api/v1/analytics/**`, `/api/v1/query/**` and `/api/v1/schedules/**` to
`REPORTING_SERVICE_URI` (default `http://reporting-service:8080` in the gateway's
route config — note this differs from the service's own `server.port` of `8096`,
so the URI env var must be set correctly in deployment), with a
`forward:/fallback/reporting-service` circuit-breaker fallback.

## Monorepo integration

`reporting-service` is a Gradle module (Spring Boot 3.3.2, Java 21 toolchain) and is
now wired into the root `settings.gradle` alongside the other 16 services. Its
`build.gradle` was rewritten from a 9-line stub — it previously declared almost none
of the dependencies the code actually requires, and the module was excluded from
`settings.gradle` entirely, so `./gradlew build` silently skipped it even though the
full controller/service/entity tree already existed on disk. Both gaps are now fixed:

- `com.clickhouse:clickhouse-jdbc:0.6.3` — read-only OLAP driver
- `com.github.librepdf:openpdf:1.3.39` — PDF export
- `org.apache.poi:poi-ooxml:5.2.5` — Excel (XLSX) export
- `software.amazon.awssdk:s3` (BOM `2.26.21`) — S3-backed report storage
- `org.springframework.kafka:spring-kafka` — `report.completed` event publishing
- `org.flywaydb:flyway-core` / `flyway-database-postgresql` — Postgres job-metadata migrations
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0` — Swagger UI
- `spring-boot-starter-mail` — email notifier (JavaMail/SMTP)
