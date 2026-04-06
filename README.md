# Eagle Platform Delivery

Spring Boot backend with JWT security, RBAC, object-level reservation checks, immutable audit logging, encrypted sensitive fields, offline order processing, durable async notification queue, and Angular frontend delivery.

## Run With Docker
1. Copy `.env.example` to `.env`.
2. Run `docker compose --env-file .env up --build`.
3. Backend API is available at `http://localhost:8080`.
4. Angular frontend is available at `http://localhost:4200`.

## Run Without Docker
1. Start PostgreSQL locally and create database/user from `.env.example`.
2. Set env vars:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET`
   - `DATA_ENCRYPTION_KEY`
3. Run backend from `app/backend` with `mvn spring-boot:run`.
4. Run Angular frontend from `app/frontend`:
   - `npm install`
   - `npm run start`

## Security + Reliability Controls Implemented
- Authenticated-by-default API with JWT filter and role route rules.
- Role and object-level authorization for reservations and privileged operations.
- Login lockout: 5 failed attempts -> 15 minute lock.
- Password policy: minimum 12 chars with uppercase/lowercase/digit/special.
- Immutable append-only `audit_logs` chain with hash linking.
- Staff contact encryption at rest using field-level AES-GCM converter.
- Request rate limiting: 60 API requests per minute per authenticated user (fallback IP key for unauthenticated calls).
- Durable async local queue for notification refresh jobs with retries and basic circuit breaker.
- Offline order lifecycle (`UNPAID`, `PAID`, `REFUNDED`) with idempotency and CSV reconciliation + checksum duplicate detection.
- Nightly encrypted backup snapshots and admin canary health endpoint (`/api/v1/health/canary`).
- Maintenance SLA automation: due time computed as 4 business hours and overdue auto-flagging.
- 30-day critical asset inspection cadence generation.

## Frontend
- `app/frontend` is an Angular application with role-based Requester, Front Desk, Maintenance, Operations, and Admin workspaces that call Spring APIs.

## Demo Credentials
- requester.demo / ChangeMe!1234
- agent.demo / ChangeMe!1234
- tech.demo / ChangeMe!1234
- ops.demo / ChangeMe!1234
- admin.demo / ChangeMe!1234
# Facility-Reservation
