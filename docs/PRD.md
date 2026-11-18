# WorkforceOS

## Product Requirements Document (PRD)

**Document Version:** 1.0
**Status:** Draft
**Product Type:** Full-stack workforce operations platform
**Technology:** Angular frontend + Java 25 / Spring Boot 3 backend
**Database:** PostgreSQL
**Date:** August 2026

> **Implementation status:** the P0 core (auth, workforce, scheduling, attendance, leave/overtime approvals, handover, audit, reports) is implemented, with shift swap, attendance correction, notifications, Redis caching, the transactional PostgreSQL outbox/event bus with optional Kafka relay, observability, the workforce risk intelligence pipeline, advanced reports and dashboards, API rate limiting, an AI workforce assistant, predictive absenteeism analytics, demand forecasting, advisory auto-scheduling, and a config-gated external integration framework already delivered. Multi-tenant organization administration — `ADMIN`-gated organization CRUD, per-tenant account provisioning, and service-layer tenant isolation across every tenant-owned domain — is also delivered, alongside Playwright UI end-to-end coverage for the Angular app. PostgreSQL persists the workforce/request core, risk data, outbox, and notifications via Flyway; in-memory stores are retained only for the test profile. Real notification delivery (external providers), concrete vendor integration rollouts, and the remaining commercial SaaS packaging remain on the roadmap and require business approval — see [ROADMAP.md](ROADMAP.md) and [PROGRESS.md](PROGRESS.md).

---

# 1. Product Overview

WorkforceOS is an enterprise backend platform for managing workforce operations in organizations with shift-based employees.

The platform exposes REST APIs for workforce scheduling, attendance, leave, overtime, shift handover, approval workflows, notifications, auditing, and reporting.

The first version will be implemented as an Angular web frontend backed by a **modular monolith using Spring Boot**.

---

# 2. Product Vision

> Provide a reliable, auditable, and scalable backend platform that enables organizations to plan workforce schedules, monitor attendance, manage operational requests, and maintain continuity between shifts.

---

# 3. Product Goals

1. Provide a centralized workforce API.
2. Automate workforce business rules.
3. Provide reliable attendance processing.
4. Prevent roster conflicts.
5. Standardize approval workflows.
6. Digitize shift handovers.
7. Provide complete auditability.
8. Support asynchronous event processing.
9. Provide production-ready observability.
10. Establish a foundation for future integrations.

---

# 4. Target Users

## Employee

Needs to:

* View assigned shifts
* Clock in
* Clock out
* Record breaks
* Submit leave
* Submit overtime
* Submit shift swap
* Create handover

## Supervisor

Needs to:

* View team schedules
* Monitor attendance
* Validate attendance
* Review handovers
* Approve requests

## Manager

Needs to:

* Manage rosters
* Publish schedules
* Approve requests
* Monitor workforce performance
* Access reports

## HR

Needs to:

* Manage employee data
* Monitor workforce attendance
* Generate reports

## Administrator

Needs to:

* Manage users
* Manage roles
* Configure system
* Monitor system health

---

# 5. Product Modules

```text
Authentication
Workforce
Scheduling
Attendance
Leave
Overtime
Shift Swap
Handover
Approval
Notification
Audit
Reporting
```

---

# 6. MVP Scope

## P0 — Must Have

* Authentication
* User management
* Role-based authorization
* Employee management
* Department management
* Team management
* Shift management
* Roster management
* Roster conflict detection
* Clock in
* Clock out
* Attendance calculation
* Leave requests
* Overtime requests
* Approval workflow
* Shift handover
* Audit log

## P1 — Should Have

* Redis caching
* Kafka events
* Notifications
* Attendance correction
* Shift swap
* Advanced reporting
* API rate limiting

## P2 — Future

* AI Workforce Copilot
* Predictive workforce analytics
* Automated scheduling
* External HR integrations
* Payroll integrations

---

# 7. Functional Requirements

## 7.1 Authentication

### FR-AUTH-001

Users must be able to authenticate using username/email and password.

### FR-AUTH-002

The API must issue an access token after successful authentication.

### FR-AUTH-003

Protected endpoints must require valid authentication.

### FR-AUTH-004

Users must only access resources permitted by their role.

### FR-AUTH-005

System administrators can provision user accounts scoped to a specific organization; a provisioned account authenticates against that organization only.

### FR-AUTH-006

All tenant-owned reads and writes must be scoped to the caller's organization; access to resources owned by another organization must return `404` without disclosing their existence.

---

# 8. Employee Management

### FR-EMP-001

Authorized users can create employees.

### FR-EMP-002

Authorized users can update employee information.

### FR-EMP-003

Authorized users can deactivate employees.

### FR-EMP-004

Employees must belong to an organizational structure.

Example:

```text
Department
    |
    +-- Team
          |
          +-- Employee
```

---

# 9. Shift Management

The system must support shift templates.

Example:

```text
Morning
08:00 - 16:00

Evening
16:00 - 00:00

Night
00:00 - 08:00
```

### FR-SHIFT-001

Authorized users can create shift templates.

### FR-SHIFT-002

The system must support overnight shifts.

### FR-SHIFT-003

The system must support configurable break periods.

### FR-SHIFT-004

Shift definitions must have a clear start and end time.

---

# 10. Roster Management

### FR-ROSTER-001

Managers can create rosters.

### FR-ROSTER-002

Managers can assign employees to shifts.

### FR-ROSTER-003

The system must detect overlapping assignments.

### FR-ROSTER-004

A roster can have the following states:

```text
DRAFT
PUBLISHED
CANCELLED
COMPLETED
```

### FR-ROSTER-005

Only authorized users can publish a roster.

---

# 11. Attendance

Attendance is event-based.

Supported events:

```text
CLOCK_IN
BREAK_START
BREAK_END
CLOCK_OUT
```

### FR-ATT-001

An employee can clock in only when eligible for the assigned shift.

### FR-ATT-002

Duplicate active clock-ins must be rejected.

### FR-ATT-003

The system must calculate lateness.

Example:

```text
Scheduled: 08:00
Actual:    08:15

Late: 15 minutes
```

### FR-ATT-004

The system must calculate early departure.

### FR-ATT-005

The system must support overnight attendance.

### FR-ATT-006

Attendance operations must be transactional.

---

# 12. Attendance Status

The system may calculate:

```text
PRESENT
LATE
ABSENT
EARLY_LEAVE
OVERTIME
INCOMPLETE
```

Status calculation must use:

* Assigned shift
* Scheduled start
* Scheduled end
* Actual attendance
* Approved leave
* Approved overtime

---

# 13. Leave Management

### FR-LEAVE-001

Employees can submit leave requests.

### FR-LEAVE-002

Employees can view their requests.

### FR-LEAVE-003

Managers can approve or reject leave.

### FR-LEAVE-004

Leave requests must have a lifecycle:

```text
PENDING
APPROVED
REJECTED
CANCELLED
```

### FR-LEAVE-005

Approval actions must be recorded.

---

# 14. Overtime

### FR-OT-001

Employees can submit overtime requests.

### FR-OT-002

Managers can approve or reject overtime.

### FR-OT-003

Approved overtime must be associated with an employee and date.

### FR-OT-004

Overtime approval must generate an auditable event.

---

# 15. Shift Swap

Employees may request a shift exchange.

Flow:

```text
Employee A
    |
    v
Shift Swap Request
    |
    v
Employee B
    |
    v
Supervisor
    |
    v
Approved
```

The system must validate:

* Both employees exist.
* Both shifts exist.
* Both employees are eligible.
* No scheduling conflict exists.

---

# 16. Handover

### FR-HO-001

Employees can create a handover report.

### FR-HO-002

A handover can contain multiple items.

Example:

```text
Handover
├── Operational Summary
├── Outstanding Tasks
├── Incident
├── Equipment
├── Safety
└── Notes
```

### FR-HO-003

Handover lifecycle:

```text
DRAFT
SUBMITTED
ACKNOWLEDGED
CLOSED
```

### FR-HO-004

The receiving shift can acknowledge a submitted handover.

### FR-HO-005

Handover history must remain auditable.

---

# 17. Approval Engine

The approval engine should be reusable across modules.

Supported requests:

```text
LEAVE
OVERTIME
SHIFT_SWAP
ATTENDANCE_CORRECTION
```

Generic lifecycle:

```text
PENDING
   |
   +----> APPROVED
   |
   +----> REJECTED
```

### FR-APP-001

Only authorized users may approve requests.

### FR-APP-002

Users cannot approve their own requests.

### FR-APP-003

Approval actions must be persisted.

### FR-APP-004

The system must maintain approval history.

---

# 18. Notification

Notifications may be generated for:

* Roster published
* Leave approved
* Leave rejected
* Overtime approved
* Overtime rejected
* Handover submitted
* Approval pending

Future implementations may support:

```text
Email
WhatsApp
Push Notification
Webhook
```

---

# 19. Audit Log

The system must record important actions.

Example:

```text
User: Muhammad
Action: APPROVE_OVERTIME
Resource: OvertimeRequest
Resource ID: 1024
Timestamp: 2026-08-28T08:30:00Z
```

Audit records should include:

* Actor
* Action
* Resource
* Resource ID
* Timestamp
* IP address where applicable
* Before/after values where appropriate

---

# 20. Reporting

The API must support:

### Attendance

```text
Present
Late
Absent
Early Leave
Overtime
```

### Workforce

```text
Total Employees
Active Employees
Employees by Department
Employees by Team
```

### Overtime

```text
Total Requests
Approved
Rejected
Pending
Total Hours
```

### Leave

```text
Pending
Approved
Rejected
Leave by Department
```

---

# 21. API Requirements

> Superseded by [API-SPECIFICATION.md](API-SPECIFICATION.md), the maintained record of the current `v1` endpoint surface, conventions, and status codes. The endpoint list originally kept here predated features shipped since (risk, assistant, analytics, integrations, organizations) and had drifted out of sync — do not use it as a reference.

---

# 22. Non-Functional Requirements

## Performance

The API should target:

* p95 read latency < 500 ms
* p95 write latency < 1 second
* Support concurrent attendance operations
* Efficient pagination for large datasets

Targets may be revised after load testing. **Correction:** pagination is not yet implemented (see [§27](#27-pagination)); the in-JVM `PerformanceSmokeTests` gate uses deliberately generous budgets (1000 ms p95, per [PROGRESS.md §4](PROGRESS.md)) as a regression smoke check, not a validated measurement of these targets — full load-harness verification is still pending per [TEST-PLAN.md §5](TEST-PLAN.md).

## Availability

Target:

```text
99.9% monthly availability
```

## Security

The system must implement:

* Spring Security
* JWT authentication
* Password hashing
* RBAC
* Input validation
* Authorization
* Secure HTTP headers
* Rate limiting
* Audit logging

**Correction:** authentication is not JWT — `AuthService` issues an opaque bearer token signed with HMAC-SHA256, not a JWT. JWT adoption remains an open tracked decision; see [PROGRESS.md §5](PROGRESS.md).

## Reliability

The system should support:

* Database transactions
* Idempotency
* Retry mechanisms
* Event retry
* Error handling
* Health checks

---

# 23. Event Architecture

> Superseded by [EVENT-DESIGN.md](EVENT-DESIGN.md). The implemented architecture is a transactional PostgreSQL outbox with synchronous in-process handlers and an optional Kafka relay (`workforce.events.kafka.enabled`) — not the generic "REST API → Kafka" flow originally sketched here.

---

# 24. Caching

Redis may be used for:

* Frequently accessed workforce data
* Shift configuration
* Dashboard statistics
* Rate limiting
* Distributed locking

Cache invalidation must occur when underlying data changes. **Correction:** rate limiting and distributed locking are not Redis-backed today — `RateLimitFilter` uses a per-instance in-memory `ConcurrentHashMap` token bucket, and no distributed-locking mechanism is implemented anywhere in the codebase. Redis (`RedisCacheConfig`) is used only for `@Cacheable` read-through caching of workforce/report/dashboard data.

---

# 25. Idempotency

Critical operations must not create duplicate records on retry. **Correction:** there is no generic `Idempotency-Key` HTTP header middleware implemented — the header example previously shown here does not reflect a real contract. Idempotency is achieved per-domain via dedupe keys enforced as unique database constraints (e.g. `risk_assessments.uq_risk_assessments_dedupe_key`, the outbox's event-id uniqueness) plus domain invariants (e.g. one active clock-in session per employee). See [guardrails.md §5](guardrails.md) and [DATABASE-DESIGN.md §2](DATABASE-DESIGN.md).

---

# 26. Error Handling

> Superseded by [API-SPECIFICATION.md §5](API-SPECIFICATION.md), which documents the actual `ApiError` shape returned by `ApiExceptionHandler` — it also carries `traceId` and `requestId` for log correlation, which the example originally shown here omitted.

---

# 27. Pagination

**Not yet implemented.** Collection endpoints currently return full, unpaginated lists — the response shape shown here is a target design, not current behavior. See [API-SPECIFICATION.md §1](API-SPECIFICATION.md), which flags pagination parameters and page size as TBD, and [PROGRESS.md](PROGRESS.md) for tracking.

---

# 28. Database Requirements

> Superseded by [DATABASE-DESIGN.md](DATABASE-DESIGN.md), the maintained record of the implemented schema, migrations (`V1`–`V6`), constraints, indexes, and tenant-isolation strategy.

---

# 29. Testing Requirements

> Superseded by [TEST-PLAN.md](TEST-PLAN.md), the maintained test strategy and current baseline (149 backend tests via `mvn test`, `test` profile/H2, 0 failures). **Correction:** Testcontainers is not wired into the build (no dependency in `pom.xml`) — integration tests run against H2 in PostgreSQL-compatibility mode today; see [tools.md §9](tools.md) for the verified toolchain.

---

# 30. Observability

The backend should provide:

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
```

Metrics should include:

```text
HTTP request latency
HTTP error rate
Attendance operations
Approval operations
Kafka events
Database connection pool
JVM metrics
```

Logs should be structured and include correlation/request IDs where possible.

---

# 31. Deployment

> Superseded by [DEPLOYMENT.md](DEPLOYMENT.md), the maintained record of environments, configuration variables, and release process. **Correction:** the CI pipeline sketched here does not exist yet — there is no committed `.github/workflows`; automated checks currently run manually (`mvn test`, Playwright) — see [PROGRESS.md §5](PROGRESS.md).

---

# 32. Definition of Done

A feature is considered complete when:

* Business requirements are implemented.
* API endpoint is documented.
* Validation is implemented.
* Authorization is implemented.
* Business rules are tested.
* Database migration is included.
* Error handling is implemented.
* Audit requirements are satisfied.
* Unit tests pass.
* Integration tests pass.
* API documentation is updated.

---

# 33. MVP Acceptance Criteria

> Superseded by [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md), the maintained release gate. It covers everything in the checklist that used to live here plus risk intelligence, AI assistance, predictive analytics, external integrations, and multi-tenant isolation, each tied to its automated test evidence.

---

# 34. Release Plan

## Release 0.1 — Foundation

```text
Spring Boot setup
Database
Flyway
Security
Exception handling
API conventions
Testing framework
Docker
```

## Release 0.2 — Workforce

```text
Users
Roles
Employees
Departments
Teams
Positions
```

## Release 0.3 — Scheduling

```text
Shift
Shift Template
Roster
Assignment
Conflict Detection
```

## Release 0.4 — Attendance

```text
Clock In
Clock Out
Break
Attendance Calculation
Attendance History
```

## Release 0.5 — Workflow

```text
Leave
Overtime
Shift Swap
Approval
```

## Release 0.6 — Operations

```text
Handover
Audit
Notification
```

## Release 0.7 — Infrastructure

```text
Redis
Kafka
Async Processing
Observability
```

## Release 1.0 — Production Candidate

```text
Security Hardening
Performance Testing
Integration Testing
Load Testing
Documentation
Deployment
```

---

# 35. Future Product Roadmap

```text
                    WorkforceOS
                         |
        ┌────────────────┼────────────────┐
        v                v                v
   Operations       Analytics             AI
        |                |                |
   Scheduling       Workforce KPI     AI Copilot
   Attendance       Forecasting        RAG/SOP
   Handover         Trends             Insights
   Approval         Anomaly            Recommendations
```

---

# 36. Product Success Metrics

The MVP should measure:

* API availability
* API latency
* Attendance transaction success rate
* Roster conflict detection rate
* Approval completion rate
* Handover completion rate
* Error rate
* Event processing success rate

Technical targets:

```text
Availability       ≥ 99.9%
Attendance success ≥ 99%
Critical API p95   < 1 second
Event success      ≥ 99.9%
```

---

# 37. Product Principles

### Reliability First

Workforce and attendance data must be treated as operationally important data.

### Explicit Business Rules

Business logic must be implemented in application/domain services rather than hidden inside controllers.

### Auditable

Important actions must be traceable.

### Secure by Default

Every protected operation must enforce authentication and authorization.

### API First

The backend must be independently consumable by web applications, mobile applications, integrations, or other services.

### Modular

The system should remain modular so that individual domains can evolve independently.

### Production Ready

Observability, testing, error handling, migrations, and deployment are part of the product—not afterthoughts.

---

# 38. Technical Product Direction

The initial architecture will use a **Modular Monolith** rather than immediately adopting microservices.

The web application frontend will use **Angular** and consume the backend through the documented REST API. Angular should provide role-aware workflows for employees, supervisors, managers, HR users, and administrators.

```text
            Angular
               |
            REST API
               |
            Spring Boot
                       |
        ┌──────────────┼──────────────┐
        v              v              v
    Workforce      Scheduling      Attendance
        |              |              |
        └──────────────┼──────────────┘
                       v
                   PostgreSQL
                       |
                ┌──────┴──────┐
                v             v
              Redis         Kafka
```

The architecture should allow future extraction of high-volume modules into independent services without requiring a complete rewrite.

---

# 39. Final Product Definition

**WorkforceOS** is an enterprise workforce operations platform with an Angular web frontend and a Spring Boot backend that provides:

> **Workforce + Scheduling + Attendance + Approval + Handover + Audit + Reporting**

through a secure, transactional, observable, and integration-ready REST API.

The MVP prioritizes correctness and business rules over architectural complexity.

The system should demonstrate enterprise backend engineering practices using:

```text
Angular
Java 25
Spring Boot 3
Spring Security
JPA / Hibernate
PostgreSQL
Redis
Kafka
Flyway
JUnit
Docker
Actuator
Micrometer
```

**Correction:** `Testcontainers` and a dedicated `Prometheus` metrics registry were on the original target list but are not wired into the build — there is no such dependency in `pom.xml` today; integration tests run against H2, and metrics are exposed only through Spring Boot Actuator. See [tools.md](tools.md) for the current, code-verified dependency list.

---

**End of PRD**
