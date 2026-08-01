# Spring Boot Configuration Guide - YAML Environment Profiles

This document explains the YAML configuration structure for all microservices in the Payment Processor platform.

## Overview

Each microservice includes 4 YAML configuration files:

1. **application.yml** - Default/base configuration
2. **application-local.yml** - Local development environment
3. **application-dev.yml** - Development environment (hosted)
4. **application-prod.yml** - Production environment

## Activating Profiles

To activate a specific profile, use the Spring profile system:

### Command Line

```bash
# Local development
java -jar service.jar --spring.profiles.active=local

# Dev environment
java -jar service.jar --spring.profiles.active=dev

# Production
java -jar service.jar --spring.profiles.active=prod
```

### Environment Variable

```bash
export SPRING_PROFILES_ACTIVE=local
java -jar service.jar
```

### application.yml Override

```yaml
spring:
  profiles:
    active: dev
```

### Gradle (bootRun)

```bash
./gradlew :service-name:bootRun --args='--spring.profiles.active=local'
```

---

## Configuration Profiles Breakdown

### 1. Default Profile (application.yml)

**Use Case:** Default settings, matches local/localhost setup

- Database: `localhost:5432` (local PostgreSQL)
- Eureka: `localhost:8761` (local Eureka server)
- Credentials: Default postgres/postgres
- SQL: `validate` mode (assumes schema exists)
- Logging: DEBUG for application code
- DDL Auto: `validate` (doesn't modify schema)

### 2. Local Profile (application-local.yml)

**Use Case:** Local machine development

- Database: `localhost:5432` with **local** database name
- Eureka: **Disabled** (register-with-eureka: false)
- Credentials: Default postgres/postgres
- SQL: `create-drop` mode (creates and drops schema on startup)
- Logging: DEBUG for application + Hibernate SQL
- DDL Auto: `create-drop` (recreates schema from scratch)
- Best for: Rapid development and testing locally

### 3. Dev Profile (application-dev.yml)

**Use Case:** Development/staging environment (hosted infrastructure)

- Database: `db-dev.internal:5432` with dev database
- Eureka: Dev Eureka server (register-with-eureka: true)
- Credentials: **Environment variables** (`${DB_USERNAME}`, `${DB_PASSWORD}`)
- SQL: `validate` mode
- Logging: WARN (less verbose)
- DDL Auto: `validate`
- Connection Pool: Standard (not tuned)
- Best for: Shared dev environment, staging tests

### 4. Prod Profile (application-prod.yml)

**Use Case:** Production environment

- Database: `db-prod.internal:5432` with prod database
- Eureka: Prod Eureka server
- Credentials: **Environment variables** (must be set securely)
- SQL: `validate` mode
- SSL/TLS: **Enabled** with keystore
- Logging: WARN (minimal)
- Connection Pool: **Optimized** (larger pools, better performance)
- Timeouts: Higher retry attempts and longer timeouts
- Monitoring: Prometheus metrics enabled
- Best for: Production deployment on K8s/cloud

---

## Environment Variables

### Required for Dev/Prod Environments

```bash
# Database credentials
export DB_USERNAME=prod_db_user
export DB_PASSWORD=secure_password_here

# Redis (Limit Service only)
export REDIS_PASSWORD=redis_secure_password

# SSL/TLS (Production)
export SSL_KEYSTORE_PATH=/path/to/keystore.p12
export SSL_KEYSTORE_PASSWORD=keystore_password
```

### Using .env Files (for local development)

Create `.env` file in project root:

```bash
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_PASSWORD=redis_password
SSL_KEYSTORE_PATH=/path/to/keystore.p12
SSL_KEYSTORE_PASSWORD=password
```

Then load with:

```bash
source .env
java -jar service.jar --spring.profiles.active=dev
```

---

## Service-Specific Configuration

### Tokenization Service (Port 8084)

**Profiles:**

- **local:** create-drop, localhost DB, disabled Eureka
- **dev:** validate, dev.internal DB, dev Eureka, shorter retention (testing)
- **prod:** validate, prod.internal DB, prod Eureka, HSM enabled, full retention (2555 days)

**Key Config:**

```yaml
tokenization:
  encryption:
    algorithm: AES/GCM/NoPadding
    key-size: 256
    hsm-enabled: false (local/dev) | true (prod)
  vault:
    retention-days: 30 (local/dev) | 2555 (prod)
  audit:
    enabled: true (all profiles)
```

### Limit Service (Port 8085)

**Profiles:**

- **local:** create-drop, localhost DB/Redis, disabled Eureka
- **dev:** validate, dev.internal DB/Redis, dev Eureka
- **prod:** validate, prod.internal DB/Redis, prod Eureka, optimized pool (30 conns)

**Key Config:**

```yaml
limit:
  daily-transaction-limit: 10000.00 (local/dev) | 50000.00 (prod)
  monthly-transaction-limit: 100000.00 (local/dev) | 500000.00 (prod)
  per-transaction-limit: 50000.00 (local/dev) | 100000.00 (prod)
  velocity-max-transactions: 20 (local/dev) | 100 (prod)
```

### Authorization Service (Port 8086)

**Profiles:**

- **local:** create-drop, localhost DB, disabled Eureka
- **dev:** validate, dev.internal DB, dev Eureka
- **prod:** validate, prod.internal DB, prod Eureka, SSL enabled, longer timeouts

**Key Config:**

```yaml
authorization:
  timeout-seconds: 30 (local/dev) | 60 (prod)
  retry-attempts: 3 (local/dev) | 5 (prod)
  avs-required: true (all profiles)
  3ds-enabled: true (all profiles)
```

---

## Quick Start

### Local Development

```bash
# Start PostgreSQL locally
docker run --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres

# Start Redis locally (for Limit Service)
docker run --name redis -p 6379:6379 -d redis

# Run service with local profile
cd tokenization-service
../../gradlew bootRun --args='--spring.profiles.active=local'
```

### Development Environment

```bash
# Set environment variables
export DB_USERNAME=dev_user
export DB_PASSWORD=dev_password
export REDIS_PASSWORD=redis_password

# Run service with dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Production Deployment (Docker)

```dockerfile
FROM openjdk:21-slim
COPY target/service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

### Production Deployment (Kubernetes)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: tokenization-service
spec:
  containers:
    - name: tokenization
      image: payment-processor/tokenization-service:1.0.0
      env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
```

---

## Configuration Hierarchy

Spring Boot loads configurations in order (later overrides earlier):

1. `application.yml` (defaults)
2. `application-{profile}.yml` (profile-specific)
3. Environment variables
4. System properties
5. Command-line arguments

Example: If `application.yml` has `server.port: 8084` and you run with `--server.port=9000`, it uses `9000`.

---

## Database Connection Pooling

### Local Profile

- **Type:** Default HikariCP
- **Max Pool Size:** 10
- **Min Idle:** 2

### Dev Profile

- **Type:** HikariCP
- **Max Pool Size:** 20 (Tokenization) | 30 (Limit)
- **Min Idle:** 5-10

### Prod Profile

- **Type:** HikariCP
- **Max Pool Size:** 20-30
- **Min Idle:** 5-10
- **Max Lifetime:** 30 minutes (1800000 ms)

---

## Monitoring & Observability

All services expose actuator endpoints (when enabled):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

**Available endpoints:**

- `GET /actuator/health` - Service health
- `GET /actuator/metrics` - Available metrics
- `GET /actuator/metrics/{metric}` - Specific metric
- `GET /actuator/prometheus` - Prometheus scrape endpoint

**Example:**

```bash
curl http://localhost:8084/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

---

## Logging Configuration

### Local Profile

```
[DEBUG] com.paymentprocessor - Application debug logs
[DEBUG] org.springframework.web - Spring Web debug logs
[DEBUG] org.hibernate.SQL - SQL statements
```

### Dev Profile

```
[WARN] root - All other logs
[INFO] com.paymentprocessor - Application info logs
```

### Prod Profile

```
[WARN] root - All other logs
[INFO] com.paymentprocessor - Application info logs
Format: Timestamp - Logger - Message
```

---

## Troubleshooting

### Database Connection Failed

```
Error: ERROR: database "tokenization_db" does not exist

Solution:
1. Check if PostgreSQL is running: `psql -U postgres`
2. Create database: `createdb tokenization_db`
3. Or use local profile which creates DB on startup: `--spring.profiles.active=local`
```

### Port Already in Use

```
Error: java.net.BindException: Address already in use

Solution:
1. Change port in YAML: `server.port: 8090`
2. Or kill existing process: `lsof -i :8084 | kill -9 {PID}`
```

### Eureka Connection Failed

```
Error: com.netflix.discovery.shared.transport.TransportException

Solution:
Use local profile to disable Eureka: `--spring.profiles.active=local`
Or ensure Eureka server is running on `localhost:8761`
```

### Environment Variable Not Found

```
Error: Could not resolve placeholder 'DB_PASSWORD' in value...

Solution:
1. Export the variable: `export DB_USERNAME=postgres`
2. Or set default: `${DB_USERNAME:postgres}` in YAML
```

