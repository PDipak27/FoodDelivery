# Release Notes

## v1.0.0-SNAPSHOT — Initial Local Build

**Date:** 2026-05-10  
**Status:** Development / Local only

---

### What's included

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Spring Cloud Gateway — JWT validation, routing |
| Auth Service | 8081 | Register, login, JWT + refresh token rotation |
| User Service | 8082 | Customer profile CRUD |
| Restaurant Service | 8083 | Restaurant and menu management (MongoDB + Redis cache) |
| Order Service | 8084 | Order lifecycle orchestration |
| Payment Service | 8085 | Mock payment stub (SUCCESS / FAILED) |
| Delivery Service | 8086 | Agent assignment and delivery status transitions |
| Notification Service | 8087 | HTML email via Mailpit (local SMTP) |

---

### Tech stack

- **Java 17** · **Spring Boot 3.2.5** · **Maven** (multi-module)
- **Spring Cloud Gateway 2023.0.1** (reactive)
- **PostgreSQL 18** (local) — one DB `fooddelivery_db`, 5 schemas
- **MongoDB 7** (local) — restaurant documents with embedded menus
- **Redis 7.2** (Docker container) — menu cache + token blacklist at gateway
- **Mailpit** (Docker container) — local SMTP capture
- **JJWT 0.12.3** — access tokens (15 min) + refresh tokens (7 days, SHA-256 hashed)
- **RestClient** (Spring 6.1) — inter-service HTTP calls
- **Testcontainers** — integration tests (PostgreSQL, MongoDB containers)
- **Lombok** — boilerplate reduction across all services

---

### Design decisions

- **Gateway is the auth boundary** — JWT validated once at the gateway; downstream services receive `X-User-Id` and `X-User-Role` headers and trust them.
- **Refresh token rotation** — every `/auth/refresh` revokes the old token and issues a new one; token hash (not raw value) stored in DB.
- **MongoDB for restaurants** — menu items embedded in restaurant document; always read together, no joins needed.
- **Notification is fire-and-forget** — `NotificationClient` swallows exceptions so email failure never rolls back an order.
- **Payment mock** — amounts > 9999 simulate failure; useful for testing error paths without a real gateway.
- **Single Postgres DB, multiple schemas** — easier local setup; schemas mirror service boundaries, straightforward to split into separate DBs later.

---

### Setup checklist (first run)

1. Start infra containers:
   ```
   docker compose -f docker/docker-compose.infra.yml up -d
   ```
2. Create Postgres DB and schemas (run manually):
   ```
   psql -U postgres -c "CREATE DATABASE fooddelivery_db;"
   psql -U postgres -d fooddelivery_db -f db-scripts/00_create_schemas.sql
   psql -U postgres -d fooddelivery_db -f db-scripts/auth/V1__init_auth.sql
   psql -U postgres -d fooddelivery_db -f db-scripts/user_svc/V1__init_user.sql
   psql -U postgres -d fooddelivery_db -f db-scripts/orders/V1__init_orders.sql
   psql -U postgres -d fooddelivery_db -f db-scripts/payments/V1__init_payments.sql
   psql -U postgres -d fooddelivery_db -f db-scripts/delivery/V1__init_delivery.sql
   ```
3. Set environment variable:
   ```
   set JWT_SECRET=your-secret-key-minimum-32-characters-long
   ```
4. Start services in Eclipse: run each `*Application.java` main class, or:
   ```
   mvn spring-boot:run -pl auth-service
   ```
5. View captured emails: http://localhost:8025

---

### Known limitations (v1.0.0-SNAPSHOT)

- No Dockerfiles yet for service images (docker-compose.services.yml requires them — to be added in v1.1.0)
- No CICD pipeline
- No distributed tracing / observability
- Single-schema Postgres (migration to per-DB planned in v2.0.0)
- Payment service is a stub — no real gateway integration
- No rate limiting configured on the gateway beyond what Spring Cloud Gateway defaults provide

---

## Planned — v1.1.0

- Dockerfile per service (multi-stage, slim JRE image)
- Docker network setup script
- Postman collection (full happy-path + error-path flows)
- Seed data script for demo
