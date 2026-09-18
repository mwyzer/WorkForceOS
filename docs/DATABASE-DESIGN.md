# WorkforceOS Database Design

**Status:** Implemented in part | **Database:** PostgreSQL; Flyway migrations with `ddl-auto: validate`

## 1. Principles

- Use UUID primary keys unless operational constraints require another strategy.
- Store timestamps as timezone-aware values and normalize service timestamps to UTC.
- Store business-local timezone on the organization; calculate shifts using explicit timezone rules.
- Use foreign keys, check constraints, unique constraints, and indexes to enforce invariants close to the data.
- Keep audit and event records append-only.
- Apply schema changes through versioned Flyway migrations (`db/migration/V<n>__*.sql`); Hibernate validates entities against the schema at startup and never auto-updates in production.

## 2. Implemented Schema (Flyway)

Versioned migrations create the tables below. `V1__init.sql` covers the relational operational core, `V2__risk.sql` covers the risk intelligence persistence, `V3__persistence.sql` promotes schedule, attendance, handover, approval, notification, audit, shift-swap, and outbox data to PostgreSQL, `V4__organizations.sql` introduces the tenant root and `organization_id` discriminators, and `V5__outbox_kafka.sql` adds the `kafka_published_at` relay marker to `outbox_events`.

| Table | Purpose |
| --- | --- |
| `organizations` | Tenant root: id, name, timezone, active, created_at |
| `departments` | Organizational departments (id, organization_id, name, active) |
| `teams` | Teams within departments (FK to departments) |
| `employees` | Workforce profile: number, name, email, department, team, active |
| `leave_requests` | Leave request and lifecycle data |
| `overtime_requests` | Overtime request and lifecycle data |
| `user_accounts` | Login identity with BCrypt password hash |
| `user_account_roles` | Role assignment (composite PK username + role) |
| `shift_templates` | Reusable shift definitions and break policy |
| `rosters` | Schedule publication lifecycle |
| `roster_assignments` | Employee-to-shift assignment for a date/time range |
| `attendance_sessions` | Clock in/out sessions per assignment |
| `attendance_corrections` | Controlled attendance correction workflow |
| `shift_swap_requests` | Proposed employee shift exchange |
| `approval_flows` / `approval_actions` | Reusable approval lifecycle and actor history |
| `handovers` | Shift handover reports and contents |
| `notifications` | Notification intents, channels, and status |
| `audit_logs` | Actor, action, resource, timestamp, and change data |
| `outbox_events` | Transactionally recorded domain events |
| `risk_assessments` | Persisted risk assessment per risk type/entity/window, deduplicated by `dedupe_key` |
| `risk_recommendations` | Recommendations per assessment (FK to risk_assessments) |
| `risk_alerts` | Open/resolved alerts per assessment (FK to risk_assessments) |

Key constraints and indexes implemented:

- `uq_employees_employee_number` unique on `employees.employee_number`.
- `uq_risk_assessments_dedupe_key` unique on `risk_assessments.dedupe_key` (the `type:entityId:windowStartMillis` dedupe key that keeps repeated analysis runs idempotent).
- `idx_risk_assessments_created_at` and `idx_risk_alerts_status` for the risk dashboard queries.

## 3. Relationship Baseline

```text
organization 1--* department 1--* team 1--* employee
employee 1--* leave_request / overtime_request
employee 1--* attendance_session / attendance_correction
roster 1--* roster_assignment 1--* attendance_session
employee 1--* handover
employee 1--* shift_swap_request
request 1--* approval_action
user 1--* user_role
risk_assessment 1--* risk_recommendation
risk_assessment 1--* risk_alert
```

## 4. Tenant and Isolation Strategy

WorkforceOS uses **shared-schema multi-tenancy** with an `organization_id` discriminator:

- `organizations` is the tenant root; a default organization is seeded by `V4__organizations.sql` and mirrored at runtime by `OrganizationBootstrap`.
- Every tenant-owned table carries `organization_id NOT NULL` (a `UUID` referencing `organizations.id`) with an index; `outbox_events` retains its nullable `organization_id` because outbox records may be created for system operations.
- Per request, `TenantContextFilter` resolves the authenticated user's organization into `TenantContext` (`ThreadLocal`). Unauthenticated traffic and accounts without an explicit organization fall back to the default organization.
- Enforcement points: reads and writes are scoped by `organization_id` at the JPA repository/persistence layer; the discriminator columns make this a column-level filter that can be hardened repository-by-repository and later replaced by PostgreSQL row-level security policies without schema changes.
- The MVP is a single-organization deployment; real multi-tenant resolution (per-account organization membership) is wired through `TenantResolver` and will activate when organization administration is exposed.

## 5. Planned Tables

The following areas are optional or not yet scheduled; when created they must follow the tenant discriminator convention above:

- `roles` / `user_roles` (role definitions are constants today; assignments live in `user_account_roles`)
- `positions` — optional job/position definitions

## 6. Important Constraints

- Employee identity and organization membership must be unique according to policy.
- A roster assignment must reference an existing employee and shift and have a valid time range.
- Overlapping assignments for the same employee must be rejected. PostgreSQL exclusion constraints or transactional locking are candidates.
- Attendance events must have a valid event type and event time; one active session per employee must be enforced.
- Request and handover status changes must follow their state machines.
- Approval actor cannot equal request creator.
- Outbox event identifiers and idempotency keys must be unique within their ownership scope.

## 7. Indexing Baseline

Index organization and status columns used in authorization and reporting (`idx_*_organization_id`, `idx_risk_alerts_status`, `idx_outbox_events_status`, `idx_outbox_events_kafka_pending`, and the assignment/attendance employee indexes are in place). Add composite indexes for employee plus time range on assignments and attendance, request status plus created time, and event processing status plus next-attempt time. Confirm all indexes with query plans and production-like data.

## 8. Retention and Privacy

Retention periods for attendance, audit, and outbox data are policy decisions. Personal data should be minimized, access-controlled, encrypted in transit, and removed or anonymized according to organizational and legal requirements. Audit retention must not silently conflict with deletion obligations.

## 9. Open Decisions

Partitioning, archival, soft deletion policy, and hardening tenant-scoped reads repository-by-repository (the discriminator columns now exist; persistence-layer filtering is being extended across stores).
