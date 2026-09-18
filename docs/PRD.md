# WorkforceOS

## Product Requirements Document (PRD)

**Document Version:** 1.0
**Status:** Draft
**Product Type:** Full-stack workforce operations platform
**Technology:** Angular frontend + Java 21 / Spring Boot 3 backend
**Database:** PostgreSQL
**Date:** August 2026

> **Implementation status:** the P0 core (auth, workforce, scheduling, attendance, leave/overtime approvals, handover, audit, reports) is implemented, with shift swap, attendance correction, notifications, Redis caching, the transactional PostgreSQL outbox/event bus with optional Kafka relay, observability, the workforce risk intelligence pipeline, advanced reports and dashboards, API rate limiting, an AI workforce assistant, predictive absenteeism analytics, demand forecasting, advisory auto-scheduling, and a config-gated external integration framework already delivered. PostgreSQL persists the workforce/request core, risk data, outbox, and notifications via Flyway; in-memory stores are retained only for the test profile. Real notification delivery (external providers) and concrete vendor integration rollouts remain on the roadmap and require business approval — see [ROADMAP.md](ROADMAP.md).

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

Base URL:

```text
/api/v1
```

Authentication:

```text
Authorization: Bearer <token>
```

Example endpoints:

```http
POST /api/v1/auth/login

GET /api/v1/employees
POST /api/v1/employees
GET /api/v1/employees/{id}

GET /api/v1/shifts
POST /api/v1/shifts

GET /api/v1/rosters
POST /api/v1/rosters
POST /api/v1/rosters/{id}/publish

POST /api/v1/attendance/clock-in
POST /api/v1/attendance/clock-out

GET /api/v1/attendance/me

POST /api/v1/leave-requests
POST /api/v1/leave-requests/{id}/approve
POST /api/v1/leave-requests/{id}/reject

POST /api/v1/overtime-requests
POST /api/v1/overtime-requests/{id}/approve
POST /api/v1/overtime-requests/{id}/reject

POST /api/v1/handovers
POST /api/v1/handovers/{id}/submit
POST /api/v1/handovers/{id}/acknowledge
```

---

# 22. Non-Functional Requirements

## Performance

The API should target:

* p95 read latency < 500 ms
* p95 write latency < 1 second
* Support concurrent attendance operations
* Efficient pagination for large datasets

Targets may be revised after load testing.

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

The system should publish domain events for important operations.

Example:

```text
AttendanceClockedIn
AttendanceClockedOut
RosterPublished
LeaveApproved
LeaveRejected
OvertimeApproved
HandoverSubmitted
HandoverAcknowledged
```

Architecture:

```text
REST API
   |
   v
Application Service
   |
   v
Domain Operation
   |
   v
Database Transaction
   |
   v
Domain Event
   |
   v
Kafka
   |
   +----> Notification
   |
   +----> Audit
   |
   +----> Analytics
```

---

# 24. Caching

Redis may be used for:

* Frequently accessed workforce data
* Shift configuration
* Dashboard statistics
* Rate limiting
* Distributed locking

Cache invalidation must occur when underlying data changes.

---

# 25. Idempotency

Critical operations should support idempotency.

Example:

```http
POST /api/v1/attendance/clock-in

Idempotency-Key: 7f3e4d...
```

The same request must not create duplicate attendance records.

This is particularly important when clients retry requests because of network failures.

---

# 26. Error Handling

The API should use a standardized error format.

Example:

```json
{
  "timestamp": "2026-08-28T08:30:00Z",
  "status": 409,
  "code": "ATTENDANCE_ALREADY_ACTIVE",
  "message": "Employee already has an active attendance session",
  "path": "/api/v1/attendance/clock-in"
}
```

HTTP status codes:

```text
200 OK
201 CREATED
204 NO_CONTENT
400 BAD_REQUEST
401 UNAUTHORIZED
403 FORBIDDEN
404 NOT_FOUND
409 CONFLICT
422 UNPROCESSABLE_ENTITY
429 TOO_MANY_REQUESTS
500 INTERNAL_SERVER_ERROR
```

---

# 27. Pagination

Collection endpoints must support pagination.

Example:

```http
GET /api/v1/employees?page=0&size=20
```

Response:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

# 28. Database Requirements

Primary database:

```text
PostgreSQL
```

Migration:

```text
Flyway
```

The database must maintain referential integrity through:

* Primary keys
* Foreign keys
* Unique constraints
* Check constraints
* Indexes

Important indexes should be created for:

* Employee identity
* Attendance employee/date
* Roster employee/date
* Approval status
* Handover status
* Audit timestamp

---

# 29. Testing Requirements

## Unit Tests

Business logic must have unit tests.

Examples:

```text
LateCalculationService
RosterConflictService
AttendanceService
ApprovalService
OvertimeService
```

## Integration Tests

Integration tests must verify:

* Database operations
* Transactions
* Security
* API behavior

## Testcontainers

PostgreSQL and Kafka should be tested using Testcontainers where appropriate.

## Test Coverage

Initial target:

```text
Business logic coverage: ≥ 80%
```

Coverage is a quality indicator, not the sole definition of test quality.

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

Initial deployment:

```text
Docker
Docker Compose
PostgreSQL
Redis
Kafka
Spring Boot
```

CI/CD:

```text
Git
   |
   v
GitHub
   |
   v
CI Pipeline
   |
   ├── Build
   ├── Unit Test
   ├── Integration Test
   ├── Security Check
   └── Docker Build
```

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

## Employee

* [ ] Admin/HR can create employee.
* [ ] Employee can be updated.
* [ ] Employee can be deactivated.
* [ ] Employee belongs to a team.

## Shift

* [ ] Shift can be created.
* [ ] Shift can be updated.
* [ ] Overnight shift is supported.
* [ ] Break configuration is supported.

## Roster

* [ ] Manager can create roster.
* [ ] Employee can be assigned.
* [ ] Overlapping shifts are rejected.
* [ ] Roster can be published.

## Attendance

* [ ] Employee can clock in.
* [ ] Employee can clock out.
* [ ] Duplicate clock-in is rejected.
* [ ] Late status is calculated.
* [ ] Overnight attendance works.

## Leave

* [ ] Employee can submit leave.
* [ ] Manager can approve.
* [ ] Manager can reject.
* [ ] Approval history is stored.

## Overtime

* [ ] Employee can submit overtime.
* [ ] Manager can approve.
* [ ] Manager can reject.
* [ ] Approval is auditable.

## Handover

* [ ] Employee can create handover.
* [ ] Handover can be submitted.
* [ ] Receiving shift can acknowledge.
* [ ] Handover history is preserved.

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
Java 21
Spring Boot 3
Spring Security
JPA / Hibernate
PostgreSQL
Redis
Kafka
Flyway
JUnit
Testcontainers
Docker
Actuator
Micrometer
Prometheus
```

---

**End of PRD**
