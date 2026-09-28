# WorkforceOS — Technical Specification

**Status:** Living document | **Source of truth for status claims:** [PROGRESS.md](PROGRESS.md) and [ROADMAP.md](ROADMAP.md) | **Last aligned:** September 2026

This is the single-page master specification for WorkforceOS. It summarizes product scope, architecture, data, API, and non-functional requirements, and links out to the detailed documents that back each section. Where this file and a detailed document disagree, the detailed document wins; [PROGRESS.md](PROGRESS.md) is authoritative for "is it actually built" questions.

---

## 1. Product Summary

WorkforceOS is an enterprise REST API platform for organizations with shift-based employees. It centralizes workforce scheduling, attendance, leave, overtime, shift handover, approvals, notifications, auditing, and reporting behind a secure, auditable, multi-tenant Spring Boot backend, with an Angular frontend for the core operational workflows.

**Vision:** provide a reliable, auditable, and scalable platform that helps organizations plan schedules, monitor attendance, manage workforce requests, and maintain continuity between shifts.

Full narrative requirements: [PRD.md](PRD.md) · [BRD.md](BRD.md) · [SRS.md](SRS.md) · [FSD.md](FSD.md)

## 2. Roles

| Role | Responsibilities |
| --- | --- |
| `ADMIN` | System configuration, organizations, users, roles, health monitoring |
| `HR` | Employee and workforce administration |
| `MANAGER` | Rosters, schedule publication, approvals, workforce performance |
| `SUPERVISOR` | Daily operations, attendance validation, handovers, approvals |
| `EMPLOYEE` | View shifts, record attendance, submit workforce requests |

Every protected endpoint enforces authentication and role-based authorization; users cannot approve their own requests.

## 3. Scope

### P0 — Must have (delivered)
Authentication, user/role management, employee/department/team management, shift templates, roster lifecycle with conflict detection, clock in/out, attendance calculation, leave and overtime requests, approval engine, shift handover, audit logging.

### P1 — Should have (delivered)
Redis read-through caching, transactional PostgreSQL outbox with optional Kafka relay, notifications with delivery adapters, attendance correction, shift swap, advanced reports/dashboards, API rate limiting.

### P2 — Delivered ahead of schedule
Workforce risk intelligence pipeline, AI workforce assistant (offline + optional LLM), predictive absenteeism analytics, demand forecasting, advisory auto-scheduling, a config-gated external integration framework (payroll CSV, webhook, inbound biometric clock events).

### Phase 4 — Commercial SaaS rollout (in progress)
Delivered: `ADMIN`-gated organization CRUD, per-tenant admin account provisioning, service-layer tenant isolation (`TenantScope`) across every tenant-owned domain, tenant-aware caching, tenant-scoped background jobs.
Pending: self-service tenant onboarding, billing/plan/seat entitlements, frontend tenant admin console, PostgreSQL Row-Level Security as a defense-in-depth backstop.

### Explicitly out of scope for the initial release
Payroll processing, recruitment, performance management, full HRIS functionality, biometric/GPS hardware ownership, government payroll integration, native mobile applications.

Detailed phase checklist: [ROADMAP.md](ROADMAP.md) · capability-by-capability delivery ledger: [PROGRESS.md](PROGRESS.md)

## 4. Architecture

Modular monolith: Java 21, Spring Boot 3, Spring Security with signed bearer tokens, PostgreSQL via JPA + Flyway (`V1`–`V6`), Redis caching, transactional outbox with optional Kafka relay. Modules share one deployable runtime and database while keeping domain ownership and application-service boundaries explicit.

```text
HTTP request -> authentication -> authorization -> controller
             -> application service -> domain rules -> persistence
             -> domain event / audit record -> response
```

**Logical modules:** Organization/Tenancy, Identity, Workforce, Scheduling, Attendance, Requests (leave/overtime/swap/correction), Approval, Handover, Risk, Assistant, Analytics, Notification, Audit, Reporting, Integration, Events (outbox + Kafka relay).

- PostgreSQL is the source of truth for the full operational domain, risk data, outbox, and notifications; in-memory repositories are retained only for the `test` profile.
- Tenant isolation is enforced in the service layer via `TenantScope` (`require`/`force`/`assertAccess`), with every tenant-owned table carrying an `organization_id` discriminator; cross-tenant access returns `404`.
- Domain events are written transactionally via the outbox pattern, delivered in-process with idempotent deduplication, and optionally relayed to Kafka (`workforce.events.kafka.enabled`).
- External integrations sit behind the `OutboundIntegration` port; unconfigured adapters degrade to `SKIPPED` rather than blocking startup.
- LLM-backed features (risk advisor, assistant) share one OpenAI-compatible client supporting `openai`, `muse`, and `spark` providers, each config-gated, with heuristic fallback so the system works fully offline.

Full detail, diagrams, and open decisions: [ARCHITECTURE.md](ARCHITECTURE.md)

## 5. Data Model

Primary database: PostgreSQL, versioned via Flyway. Core entities: `Organization`, `User`/`Role`, `Employee`/`Department`/`Team`, `ShiftTemplate`, `Roster`/`RosterAssignment`, `AttendanceEvent`/`AttendanceRecord`, `LeaveRequest`, `OvertimeRequest`, `ShiftSwapRequest`, `AttendanceCorrection`, `Handover`/`HandoverItem`, `ApprovalAction`, `AuditLog`, `Notification`, `OutboxEvent`, plus the risk module's assessment/recommendation/alert tables.

Referential integrity is maintained through primary/foreign keys, unique constraints, check constraints, and targeted indexes (employee identity, attendance employee/date, roster employee/date, approval status, handover status, audit timestamp).

Full schema, ER diagram, and the tenant-isolation column strategy: [DATABASE-DESIGN.md](DATABASE-DESIGN.md)

## 6. API

Base path: `/api/v1`. Protected endpoints require `Authorization: Bearer <token>`, issued by `POST /api/v1/auth/login`.

Representative endpoint groups:

```text
/auth/login
/employees, /departments, /teams
/shifts, /rosters, /rosters/{id}/publish, /rosters/{id}/assignments, /rosters/{id}/auto-schedule
/attendance/clock-in, /attendance/clock-out, /attendance-corrections
/leave-requests, /overtime-requests, /shift-swap-requests  (+ /{id}/approve)
/handovers, /handovers/{id}/submit, /handovers/{id}/acknowledge
/audit-logs
/notifications
/reports/*, /dashboard/operations, /analytics/*
/risk/analyze, /risk/summary, /risk/assessments, /risk/alerts
/assistant/ask
/organizations, /organizations/current, /admin/accounts
/integrations*
/health
```

Collections are paginated (`page`, `size`) and return a standard envelope (`content`, `page`, `size`, `totalElements`, `totalPages`). Errors use a consistent structure: timestamp, HTTP status, application error code, message, request path. Critical write operations accept an idempotency key to make retries safe.

Full endpoint reference: [API-SPECIFICATION.md](API-SPECIFICATION.md)

## 7. Events

Domain events (e.g. `AttendanceClockedIn`, `RosterPublished`, `LeaveApproved`, `OvertimeApproved`, `HandoverAcknowledged`) are persisted transactionally alongside the triggering write, then delivered to in-process handlers (notifications, audit projection, risk recompute) and optionally relayed to Kafka for external consumers.

Full event catalog and delivery guarantees: [EVENT-DESIGN.md](EVENT-DESIGN.md)

## 8. Non-Functional Requirements

- **Performance:** p95 read latency < 500 ms, p95 write latency < 1 s, gated per build by `PerformanceSmokeTests`.
- **Availability:** 99.9% monthly target.
- **Security:** Spring Security, signed bearer tokens, password hashing, RBAC, input validation, secure headers, token-bucket rate limiting (429 + `Retry-After`), append-only audit logging. Details: [SECURITY.md](SECURITY.md)
- **Reliability:** transactional writes, idempotency on critical commands, bounded event retry, standardized error handling, health checks.
- **Observability:** Spring Boot Actuator, Micrometer, Prometheus-compatible metrics, structured logs with correlation/request IDs. Details: [OBSERVABILITY.md](OBSERVABILITY.md)
- **Multi-tenancy:** every tenant-owned read/write scoped to the caller's organization; cross-tenant access returns `404` without disclosing existence.

Targets are subject to revision after further load testing.

## 9. Testing

Unit tests cover business logic (attendance calculation, roster conflicts, approval, overtime); integration tests exercise database transactions, security, and API behavior against PostgreSQL-mode H2; `PersistenceIntegrationTests` validates the Flyway schema against JPA entities; `TenantIsolationTests` and `OrganizationAdminTests` cover the tenancy boundary; Playwright drives the Angular frontend end-to-end (login, home, employees, risk). Current baseline: 149 backend tests passing.

Full plan and coverage targets: [TEST-PLAN.md](TEST-PLAN.md) · acceptance checklist: [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md) · user stories: [USER-STORIES.md](USER-STORIES.md)

## 10. Deployment

Docker Compose stack: stateless Spring Boot API, PostgreSQL, Redis, and an nginx-backed Angular frontend proxying `/api` to the backend. Health is exposed via `GET /api/v1/health` and Spring Boot Actuator. Configuration and secrets are injected via environment variables (see [README.md](../README.md) for the required `WORKFORCE_AUTH_*` variables).

Full deployment topology and CI/CD pipeline: [DEPLOYMENT.md](DEPLOYMENT.md)

## 11. Current Implementation Status

As of this writing, Phases 0–3 (Foundation, P0 MVP, P1 operational scale, P2 intelligence/integrations) are complete. Phase 4 (commercial SaaS rollout) has delivered tenant isolation and organization administration; self-service onboarding, billing/plan enforcement, a frontend tenant admin console, and PostgreSQL Row-Level Security remain open. Known gaps: break-event recording and leave rejection exist in the engine but are not yet exposed via API; Playwright E2E is not yet wired into CI.

Authoritative, continuously updated status: [PROGRESS.md](PROGRESS.md)

## 12. Document Map

| Document | Purpose |
| --- | --- |
| [PRD.md](PRD.md) | Product requirements, full FR catalog, release plan |
| [BRD.md](BRD.md) | Business requirements and justification |
| [SRS.md](SRS.md) | Software requirements specification |
| [FSD.md](FSD.md) | Functional specification |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Modules, boundaries, deployment shape, open decisions |
| [DATABASE-DESIGN.md](DATABASE-DESIGN.md) | Schema, ER diagram, tenant strategy |
| [API-SPECIFICATION.md](API-SPECIFICATION.md) | Full endpoint reference |
| [EVENT-DESIGN.md](EVENT-DESIGN.md) | Domain event catalog and delivery |
| [SECURITY.md](SECURITY.md) | AuthN/authZ, hardening, tenant isolation |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Metrics, logging, tracing |
| [TEST-PLAN.md](TEST-PLAN.md) | Test strategy and coverage |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Environments, CI/CD, runtime topology |
| [USER-STORIES.md](USER-STORIES.md) | Role-based user stories |
| [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md) | MVP acceptance checklist |
| [ROADMAP.md](ROADMAP.md) | Phase-by-phase delivery checklist |
| [PROGRESS.md](PROGRESS.md) | Living capability-by-capability delivery ledger |

---

**Maintaining this file:** update it when scope, architecture, or phase status materially changes; keep claims consistent with [PROGRESS.md](PROGRESS.md) rather than aspirational.
