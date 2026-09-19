# WorkforceOS Acceptance Criteria

**Status:** Draft MVP release gate | **Implementation:** P0 core, workflows, risk intelligence, observability, multi-tenant organization administration with service-layer tenant isolation across every tenant-owned domain, and PostgreSQL persistence for all domains are implemented with automated coverage

Each criterion is accepted only when the behavior, authorization, persistence, audit, and failure path are verified by automated tests or approved system evidence.

## Currently Verified

- Auth: login returns a signed bearer token; invalid credentials return `401` without enumeration; protected routes reject missing/invalid tokens (test coverage in controller and security tests).
- Schedule/attendance domain rules: overlapping assignments rejected, roster state transitions enforced, duplicate clock-in rejected, eligibility against published assignments enforced (`ScheduleEngine`/`AttendanceEngine` unit tests).
- Approval engine: reusable lifecycle, self-approval and duplicate decision rejection, history recorded (`ApprovalEngineTests`).
- Risk pipeline: five deterministic rules, impact calculation, severity banding, advisor fallback, deduplication, alerts, and summary endpoint (`risk` unit and API tests).
- Event bus: outbox recording, handler deduplication, retry backoff, transactional PostgreSQL persistence, and Kafka relay with backoff and idempotent relay markers (`EventPublisherTests`, `EventArchitectureTests`, `KafkaRelayerTests`, `KafkaRelayIntegrationTests`, `ObservabilityTests`).
- Notifications: domain events project into per-recipient notifications with per-channel delivery adapters, status transitions (`PENDING`/`SENT`/`DELIVERED`/`FAILED`/`READ`), attempt tracking, and retry (`NotificationDeliveryServiceTests`, `NotificationProjectionIntegrationTests`, `NotificationControllerTests`).
- PostgreSQL persistence: operational core, risk, and outbox entities validate against Flyway migrations and round-trip through the JPA stores (`PersistenceIntegrationTests`).
- Tenant isolation: `TenantScope` (`require`/`force`/`assertAccess`) scopes every service-layer read and write to the caller's organization; cross-tenant access returns `404`; report/analytics/dashboard caches are tenant-keyed; background projectors derive the tenant from the event's `organization_id` (`TenantIsolationTests`, tenant-aware caching tests).
- Organization administration: `ADMIN`-only organization CRUD, per-tenant account provisioning, and caller-tenant resolution via `GET /organizations/current`; a provisioned account authenticates against its own organization only (`OrganizationAdminTests`, `OrganizationControllerTests`).
- Error contract and request correlation: `ApiError` with `traceId`/`requestId`; response headers `X-Request-Id` and `traceparent` (`ObservabilityTests`).

## Authentication and Authorization

- A valid username/email and password returns an access token.
- Invalid credentials return `401` without account enumeration details.
- Protected endpoints reject missing or invalid tokens.
- Each role can perform only its documented actions and organization scope.
- A user cannot approve their own request.

## Workforce and Scheduling

- Authorized users can create, update, deactivate, and retrieve employees.
- Employees are associated with valid organizational structures.
- Authorized users can define shifts with clear start/end times, breaks, and overnight support.
- Managers can create rosters and assignments.
- Overlapping assignments for one employee are rejected with a clear conflict response.
- Only authorized users can publish a valid roster; invalid state transitions are rejected.

## Attendance

- An employee with an eligible assignment can clock in.
- Clock in without an eligible assignment is rejected.
- A second active clock in is rejected and does not create a duplicate record.
- Break and clock-out events follow the valid event sequence.
- Clock-out calculates the expected attendance status, lateness, early departure, and overtime where applicable.
- Overnight shifts calculate against the correct calendar boundary.
- Retrying a command with the same idempotency key does not duplicate attendance.

## Requests, Approval, and Handover

- Employees can submit and retrieve their leave and overtime requests.
- Requests follow the defined lifecycle and invalid transitions are rejected.
- Authorized supervisors/managers can approve or reject eligible requests.
- Every decision stores actor, timestamp, decision, and request history.
- A handover supports multiple items, submission, acknowledgement, and lifecycle enforcement.
- Submitted handovers cannot be edited outside an authorized editable state.

## Audit, Reporting, and Operations

- Important business actions create complete audit records.
- Reports are organization-scoped and return correct data for attendance, workforce, leave, overtime, absence, and approvals, including per-employee and per-department breakdowns.
- The dashboard exposes both a summary and an operations view with pending approvals, handovers, published rosters, and recent notifications.
- API rate limiting (token bucket, configurable per-minute limit, HTTP 429 with `Retry-After`, health/actuator excluded) is enforced when enabled.
- Health checks distinguish service readiness from dependency availability.
- Errors use the documented structure and include a trace identifier where available.
- Logs and metrics do not expose passwords, tokens, or unnecessary personal data.

## Workforce Risk Intelligence

- Analysis can be triggered on demand and runs automatically after roster publication and leave/overtime/employee-deactivation events.
- Repeated analysis for the same risk type, entity, and window does not duplicate assessments or alerts.
- Risk assessments persist risk type, severity, score, entity, window, impact, and evidence.
- HIGH/MEDIUM assessments create OPEN alerts that can be resolved idempotently.
- The advisor returns an explanation, impact summary, and recommendations offline (heuristic) and via LLM when configured and reachable.
- The risk dashboard displays the summary, pipeline, alert list, assessments, and recommendations.

## AI Assistance

- The assistant answers natural-language workforce questions (headcount, approvals, attendance, overtime, leave, risk, rosters) grounded in live data, without mutating anything.
- It works offline via deterministic intent classification and heuristics, and uses an OpenAI-compatible LLM when configured, falling back to heuristics on failure.
- Blank questions are rejected with `400`; unknown questions return a general overview.

## Predictive Analytics and Automated Scheduling

- Absenteeism insights score each employee from scheduled-vs-attended shifts, lateness, and approved leave, exposing rate, trend, risk level, and human-readable drivers, and tolerate a configurable window.
- Demand forecasting projects per-weekday staffing demand from historical assignment volume and compares it with scheduled shifts and approved leave to flag shortage days over a configurable horizon.
- Auto-scheduling returns an advisory plan that fills understaffed shift slots with active, non-conflicting, non-leave employees and never writes assignments without review.

## External Integrations

- Outbound integrations implement a vendor-neutral `OutboundIntegration` port and are discoverable with their enabled state via `GET /api/v1/integrations`.
- Reference adapters (payroll CSV export, HR webhook) are disabled by default and report `SKIPPED` when unconfigured, so the application boots without provider credentials.
- Inbound biometric/mobile clock events are accepted only when enabled, map to attendance commands, and return a per-event outcome (never failing the batch), with optional circular geofence rejection.

## Multi-Tenant Isolation and Organization Administration

- Organizations can be created, listed, read, and updated by `ADMIN` only; any other role receives `403`.
- `POST /admin/accounts` provisions a user account owned by the given organization; missing required fields return `400` and duplicate usernames return `409`.
- A provisioned account authenticates against its own organization, and `GET /organizations/current` resolves its tenant.
- Tenant-owned reads and writes are scoped to the caller's organization; access to resources owned by another organization returns `404` without disclosing existence.
- Cache keys for report, analytics, and dashboard aggregation results are tenant-prefixed so tenants cannot poison each other's results.
- Background risk analysis and notification projection write into the tenant of the triggering event, even on outbox replay.

## Release Evidence

Before MVP approval, attach API contract tests, integration test results, security test results, migration verification, performance measurements, and deployment rollback evidence. Unresolved deviations require product-owner approval.
