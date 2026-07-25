# PaymentProcessor Config Repository

This folder is the local staging area for the Spring Cloud Config Server backing
repo: **https://github.com/kalandhar26/configuration.git**

## How it works

Each microservice's `application.yml` now includes:

```yaml
spring:
  config:
    import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
  cloud:
    config:
      fail-fast: false
      retry:
        max-attempts: 6
        initial-interval: 1500
        multiplier: 1.5
        max-interval: 3000
```

On startup, each service calls the Config Server (default `http://localhost:8888`,
override with the `CONFIG_SERVER_URL` env var) and asks for config matching its
`spring.application.name`. The Config Server reads the matching `<application>.yml`
file from this git repo and serves it back. Because the import is `optional:`,
a service still starts from its local `application.yml` defaults if the Config
Server is unreachable (useful for local dev without the server running).

## File naming

Spring Cloud Config resolves files by `spring.application.name`, **not** the
repo/module folder name. Two services intentionally diverge:

| Service directory     | spring.application.name | Config file            |
|------------------------|--------------------------|-------------------------|
| reporting-service      | `analytics`               | `analytics.yml`         |
| all others              | `<dir-name>`               | `<dir-name>.yml`        |

`gateway-service` has no database and therefore no `spring.datasource` block.

## Database convention

All services share one local Postgres instance:

- Username: `postgres`
- Password: `Mysql@2022`
- Database naming: `<service>servicedb` (e.g. `auditservicedb`, `userservicedb`,
  `reconciliationservicedb`)

Every value below is still overridable per-environment via env vars
(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, etc.) — nothing
here is hardcoded for production use.

## Publishing this repo

```bash
cd configuration-repo
git init
git remote add origin https://github.com/kalandhar26/configuration.git
git add .
git commit -m "Initial service configuration"
git push -u origin main
```

Then run the Config Server pointed at that remote (`spring.cloud.config.server.git.uri`).
