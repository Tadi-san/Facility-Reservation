# 1. Verdict
Fail

# 2. Scope and Verification Boundary
- Reviewed repository structure, delivery docs, backend/frontend source, security config, auth flow, controllers/services/models, and test footprint.
- Static evidence reviewed from:
  - `README.md`, `docker-compose.yml`, `docs/design.md`, `docs/api-spec.md`
  - `app/backend/src/main/java/**`
  - `app/backend/src/main/resources/static/**`
  - `app/backend/migrations/001_init.sql`
  - `app/frontend/src/main.rs`
  - Rust workspace artifacts (`Cargo.toml`, `app/backend/src/main.rs`)
- Not executed:
  - Docker commands (explicitly prohibited by review rules).
  - End-to-end runtime startup (README requires Docker).
  - Maven/Gradle/Rust build/test runs (would require dependency/runtime setup beyond documented non-Docker path).
- Docker-based verification required by project docs but not executed:
  - Yes. README startup path is Docker-only (`README.md:5`).
- What remains unconfirmed:
  - Actual runtime behavior under Docker.
  - Whether hidden local changes outside reviewed files provide missing controls/tests.

# 3. Top Findings

## Finding 1
- Severity: Blocker
- Conclusion: Core security is effectively disabled; all routes are publicly accessible.
- Brief rationale: Security filter permits every request without authentication or authorization checks.
- Evidence:
  - `app/backend/src/main/java/com/eagle/app/config/SecurityConfig.java:16-18` (`anyRequest().permitAll()`).
- Impact: Any unauthenticated user can invoke admin/front-desk/maintenance/operations endpoints, causing full privilege bypass.
- Minimum actionable fix: Enforce authenticated-by-default policy in `SecurityFilterChain`, add JWT auth filter, and restrict route access by role.

## Finding 2
- Severity: Blocker
- Conclusion: Prompt-required Angular frontend is not delivered.
- Brief rationale: No Angular project files exist; frontend is static HTML/JS plus separate Dioxus Rust UI.
- Evidence:
  - No Angular files found (`angular.json`, `package.json`, `tsconfig.json`, `.ts`) from repository-wide search.
  - Static UI entrypoint: `app/backend/src/main/resources/static/index.html:1-10`.
  - JS app logic: `app/backend/src/main/resources/static/app.js`.
  - Rust frontend exists instead: `app/frontend/src/main.rs:1-72`.
- Impact: Material prompt deviation from mandated architecture and delivery expectations.
- Minimum actionable fix: Replace/add a real Angular application consuming Spring REST APIs; remove or clearly isolate non-deliverable alternative stacks.

## Finding 3
- Severity: Blocker
- Conclusion: Route-level and object-level authorization are missing; cross-user data/actions are exposed.
- Brief rationale: Controllers expose broad read/write endpoints with no role annotations or principal-based enforcement.
- Evidence:
  - Reservation list exposes all reservations: `ReservationController.java:27-30`.
  - Front desk operations unguarded: `FrontDeskController.java:34-116`.
  - Operations moderation/promotions/announcements unguarded: `OperationsController.java:25-57`.
  - Principal fallback to demo identity: `ReservationController.java:34`, `ReservationController.java:47`.
- Impact: Privilege escalation and horizontal access risk; unauthorized users can access/modify business-critical resources.
- Minimum actionable fix: Add route RBAC (`@PreAuthorize` and/or request matcher rules), remove demo principal fallback, enforce ownership checks per resource.

## Finding 4
- Severity: High
- Conclusion: Authentication policy does not satisfy required lockout behavior (5 failed attempts => 15-minute lock).
- Brief rationale: Failed attempts are incremented, but no lock timestamp/window enforcement exists.
- Evidence:
  - Failed attempts increment/reset only: `AuthService.java:26`, `AuthService.java:30`.
  - `User` model has `failedAttempts` only, no lock-until field: `User.java:24-25`.
- Impact: Brute-force mitigation is incomplete and below explicit security requirements.
- Minimum actionable fix: Add lock state (e.g., `lockUntil`), enforce lockout after 5 failures, auto-release after 15 minutes, and return consistent auth error semantics.

## Finding 5
- Severity: High
- Conclusion: Password complexity/min-length enforcement is missing for auth/admin user creation.
- Brief rationale: DTO/service do not enforce required minimum 12 chars + complexity.
- Evidence:
  - `LoginRequest` has no validation: `LoginRequest.java:3`.
  - `AdminUserCreateRequest` has `@NotBlank` only for password: `AdminUserCreateRequest.java:5-8`.
  - No `@Valid` usage in controllers (search result empty).
- Impact: Weak passwords can be accepted, violating prompt security constraints.
- Minimum actionable fix: Add bean validation constraints (length + regex complexity), apply `@Valid` in controllers, and add server-side policy checks in service layer.

## Finding 6
- Severity: High
- Conclusion: Immutable operation auditing is not implemented.
- Brief rationale: No audit-log model/repository/controller/service for operation journaling; no append-only audit mechanism found.
- Evidence:
  - Search for audit entities/services in Java source returned none.
  - Models list lacks audit model (`app/backend/src/main/java/com/eagle/app/model/*`).
- Impact: Fails explicit compliance requirement and weakens incident/accountability traceability.
- Minimum actionable fix: Add append-only audit log table/entity with actor/action/resource/time/outcome, and write entries from all sensitive mutating operations.

## Finding 7
- Severity: High
- Conclusion: Sensitive staff contact data is not encrypted at rest.
- Brief rationale: `staffContactInfo` is persisted as plain string field; only masked at response formatting.
- Evidence:
  - Plain persisted field: `User.java:21-22`.
  - Masking only in controller response: `UserController.java:32-35`.
- Impact: Violates explicit requirement for encryption at rest for sensitive fields.
- Minimum actionable fix: Apply field-level encryption (JPA converter or database encryption), key management, and migration for existing data.

## Finding 8
- Severity: High
- Conclusion: Optional offline payments/order subsystem is missing (orders, idempotency keys, reconciliation, checksum duplicate detection).
- Brief rationale: Implementation only supports promotion discount calculation; no order lifecycle entities/APIs.
- Evidence:
  - Finance endpoint only applies promo math: `FinanceController.java:18-21`, `PromotionService.java:19-26`.
  - No order-related models in model directory listing.
  - Repository search for `idempot`, `checksum`, `csv`, `refund`, `unpaid/paid` yielded no relevant backend implementation.
- Impact: Core prompt requirement not delivered.
- Minimum actionable fix: Add offline order domain (`unpaid/paid/refunded`), idempotent create/update API via client key, CSV reconciliation with checksum + duplicate detection.

## Finding 9
- Severity: High
- Conclusion: Reliability requirements are largely absent (rate limiting, circuit breakers, backup jobs, canary workflow, static asset caching strategy).
- Brief rationale: No implementation or configuration for these mandated controls was found.
- Evidence:
  - Keyword scan across Java/resources found no rate limiter/circuit breaker/backup/canary constructs.
  - Health endpoint is minimal only: `HealthController.java:10-13`.
- Impact: Fails explicit non-functional requirements and reduces production resilience.
- Minimum actionable fix: Implement per-user rate limiting, resilience patterns around internal subsystems, scheduled encrypted backups, canary deployment gates, and static caching headers.

## Finding 10
- Severity: High
- Conclusion: Async job queue requirement is not met.
- Brief rationale: Notifications are processed synchronously via request-triggered refresh endpoint; no queue worker infrastructure.
- Evidence:
  - Manual refresh endpoint triggers queue/process inline: `FrontDeskController.java:98-103`.
  - Notification processing runs immediately in service methods: `NotificationService.java:23-43`.
- Impact: Diverges from prompt requirement for local async queue for notifications/indexing; operational scalability risk.
- Minimum actionable fix: Introduce durable local job queue and worker(s) for banner scheduling/processing and indexing tasks.

## Finding 11
- Severity: High
- Conclusion: Delivered documentation is internally contradictory and materially misaligned with this project.
- Brief rationale: `docs/design.md` and `docs/api-spec.md` describe a different product (Rocket + Dioxus + MySQL exam/proctor system), conflicting with reservation suite delivery.
- Evidence:
  - `docs/design.md` architecture/domain sections reference Rocket/Dioxus/MySQL and exam workflows.
  - `docs/api-spec.md` endpoints reference intake/candidates/exam sessions/print outputs.
- Impact: Fails delivery clarity and acceptance runnability consistency expectations.
- Minimum actionable fix: Replace stale docs with accurate architecture/API/runbook for the delivered reservation platform.

## Finding 12
- Severity: Medium
- Conclusion: Repository contains substantial unrelated legacy code/artifacts that increase architecture risk.
- Brief rationale: Rust workspace + Rocket backend source coexist with Spring project without clear isolation.
- Evidence:
  - Root workspace includes Rust members: `Cargo.toml:1-3`.
  - Legacy backend Rust dependencies: `app/backend/Cargo.toml:1-23`.
  - Unrelated Rocket app source: `app/backend/src/main.rs:1-120`.
- Impact: Maintenance confusion, accidental build/deploy drift, and prompt-fit credibility damage.
- Minimum actionable fix: Remove/archive non-deliverable stack or isolate it in a clearly marked legacy folder excluded from delivery artifacts.

## Finding 13
- Severity: Medium
- Conclusion: Test suite is effectively missing for backend/frontend delivery.
- Brief rationale: No project test directories or application tests found.
- Evidence:
  - `Test-Path` checks: `app/backend/src/test` = `False`, `app/frontend/src/test` = `False`, `app/frontend/tests` = `False`.
  - Repository search returned no meaningful app tests (only dependency-generated artifacts under `target/`).
- Impact: Delivery confidence is low for core flows, failures, and security regressions.
- Minimum actionable fix: Add baseline automated tests for core happy path + authz failures + conflict logic + asset/room reservability + metric validation.

## Finding 14
- Severity: Medium
- Conclusion: Key DTO validation annotations are present but not enforced due missing `@Valid` at controller boundaries.
- Brief rationale: Request DTO constraints do not activate without controller-side validation trigger.
- Evidence:
  - DTO constraints exist (e.g., `RoomCreateRequest.java:5-13`, `AdminUserCreateRequest.java:5-9`).
  - No `@Valid` usage in controller methods (search returned none).
- Impact: Invalid/malformed payloads can bypass expected guardrails.
- Minimum actionable fix: Add `@Valid` to request body parameters and consistent 400 handling for constraint violations.

## Finding 15
- Severity: Medium
- Conclusion: Maintenance workflow is only partially implemented; required inspection/repair closed-loop operational flow is incomplete at API level.
- Brief rationale: Maintenance controller only exposes list endpoints; no explicit create/update/close ticket endpoints with parts/labor closure transitions.
- Evidence:
  - `MaintenanceController.java:19-23` only returns inspections/tickets pages.
  - `MaintenanceTicket` model is minimal and lacks lifecycle fields tying closure outcomes to asset state transitions (`MaintenanceTicket.java:10-19`).
- Impact: Cannot demonstrate required end-to-end technician workflow from inspection to closed repair with accountability.
- Minimum actionable fix: Implement inspection logging + ticket open/update/close endpoints and enforce linkage to asset state closure (`NORMAL` or `RETIRED`).

## Finding 16
- Severity: Medium
- Conclusion: Metric versioning stores only a definition string; weighted dimensions are validated transiently but not persisted as structured version data.
- Brief rationale: Service checks weight sum but model has no weights field.
- Evidence:
  - Validation occurs in service: `MetricLibraryService.java:20-23`.
  - Persisted model fields omit weighted dimensions: `MetricTemplate.java:9-21`.
- Impact: Historical scorecard version fidelity/auditability is weakened versus prompt intent.
- Minimum actionable fix: Persist weighted dimensions per version (JSON or normalized table) and expose in read APIs.

## Finding 17
- Severity: Medium
- Conclusion: Security response handling may leak internal exception messages.
- Brief rationale: Global exception handler returns raw exception message in 500 responses.
- Evidence:
  - `GlobalExceptionHandler.java:22-27` includes `"Unexpected error: " + ex.getMessage()`.
- Impact: Potential sensitive internals disclosure to clients.
- Minimum actionable fix: Return generic 500 message externally and log detailed exception server-side.

## Finding 18
- Severity: Medium
- Conclusion: Startup path is Docker-only in docs, creating a verification boundary for non-Docker local acceptance.
- Brief rationale: No documented non-Docker startup/run instructions.
- Evidence:
  - README setup uses only `docker compose ... up --build`: `README.md:4-6`.
- Impact: Limits independent acceptance verification under no-Docker constraints.
- Minimum actionable fix: Add non-Docker local run instructions for backend/frontend and DB bootstrap, or clearly state Docker is mandatory with rationale.

# 4. Security Summary
- authentication: Fail
  - Evidence: `permitAll` on all routes (`SecurityConfig.java:16-18`), missing lockout window enforcement (`AuthService.java:26-30`, `User.java:24-25`), password policy gap.
- route authorization: Fail
  - Evidence: No route restrictions/@PreAuthorize across sensitive controllers (`ReservationController`, `FrontDeskController`, `OperationsController`, `FacilityAdminController`).
- object-level authorization: Fail
  - Evidence: all-reservations endpoint exposed (`ReservationController.java:27-30`), principal fallback behavior and requester override path (`ReservationController.java:34-49`).
- tenant / user isolation: Partial Pass
  - Evidence: Some user-scoped endpoint exists (`/reservations/mine`), but broader unrestricted endpoints expose cross-user access; multi-tenant boundaries cannot be confirmed from current design.

# 5. Test Sufficiency Summary
- Test Overview
  - Unit tests exist: Missing (no application unit test directories found).
  - API / integration tests exist: Missing.
  - Obvious test entry points if present: Cannot confirm any maintained app test runner from current tree.
- Core Coverage
  - happy path: missing
  - key failure paths: missing
  - security-critical coverage: missing
- Major Gaps
  - Missing authn/authz tests (401/403) for every privileged endpoint.
  - Missing reservation conflict and room reservability regression tests (asset OUT_OF_SERVICE/UNDER_REPAIR transitions).
  - Missing offline-order/idempotency/reconciliation tests (feature currently absent).
- Final Test Verdict
  - Fail

# 6. Engineering Quality Summary
- Delivery confidence is materially reduced by architectural inconsistency: Spring Boot app is mixed with legacy Rust workspace/backend/frontend artifacts that target a different domain.
- Core module decomposition inside Spring code is reasonable in isolation (controllers/services/repositories), but missing security controls, auditing, reliability mechanisms, and test scaffolding make the implementation non-professional for acceptance.
- Documentation quality is a major architectural risk: key docs describe a different system, preventing trustworthy handoff and verification.

# 7. Next Actions
1. Enforce authentication/authorization immediately: replace `permitAll`, add JWT auth filter, define role matrix for each endpoint, and add object ownership checks.
2. Resolve architecture/prompt mismatch: deliver a real Angular frontend and remove or isolate unrelated Rust/Dioxus/Rocket artifacts from the accepted deliverable.
3. Implement missing mandatory business/security requirements: lockout window, password complexity, immutable audit logs, encryption-at-rest for sensitive fields, offline orders with idempotency and reconciliation.
4. Add minimum automated test suite covering core flow, failure states (400/401/403/404/409), and security-critical paths.
5. Rewrite docs/runbook to match actual delivered system and include a clear non-Docker local verification path where possible.