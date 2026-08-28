# WorkforceOS Database Design

**Status:** Draft logical model | **Database:** PostgreSQL

## 1. Principles

- Use UUID primary keys unless operational constraints require another strategy.
- Store timestamps as timezone-aware values and normalize service timestamps to UTC.
- Store business-local timezone on the organization; calculate shifts using explicit timezone rules.
- Use foreign keys, check constraints, unique constraints, and indexes to enforce invariants close to the data.
- Keep audit and event records append-only.
- Apply schema changes through versioned migrations; migration tooling is TBD.

## 2. Core Tables

| Table | Purpose |
| --- | --- |
| `organizations` | Tenant or operating organization boundary |
| `users` | Login identity and account status |
| `roles` / `user_roles` | Role-based authorization |
| `departments` | Organizational departments |
| `teams` | Teams within departments |
| `employees` | Workforce profile linked to a user where applicable |
| `positions` | Optional job/position definitions |
| `shift_templates` | Reusable shift definitions and break policy |
| `rosters` | Schedule publication lifecycle |
| `roster_assignments` | Employee-to-shift assignment for a date/time range |
| `attendance_events` | Immutable clock and break events |
| `attendance_records` | Calculated attendance summary |
| `leave_requests` | Leave request and lifecycle data |
| `overtime_requests` | Overtime request and lifecycle data |
| `shift_swap_requests` | Proposed employee shift exchange |
| `attendance_correction_requests` | Controlled correction workflow |
| `approval_actions` | Approval decision and actor |
| `handovers` / `handover_items` | Shift handover reports and contents |
| `audit_logs` | Actor, action, resource, timestamp, and change data |
| `outbox_events` | Transactionally recorded domain events |

## 3. Relationships

```text
organization 1--* department 1--* team 1--* employee
organization 1--* roster 1--* roster_assignment *--1 employee
shift_template 1--* roster_assignment
employee 1--* attendance_event
employee 1--* attendance_record
employee 1--* leave_request / overtime_request / handover
request 1--* approval_action
```

## 4. Important Constraints

- Employee identity and organization membership must be unique according to policy.
- A roster assignment must reference an existing employee and shift and have a valid time range.
- Overlapping assignments for the same employee must be rejected. PostgreSQL exclusion constraints or transactional locking are candidates.
- Attendance events must have a valid event type and event time; one active session per employee must be enforced.
- Request and handover status changes must follow their state machines.
- Approval actor cannot equal request creator.
- Outbox event identifiers and idempotency keys must be unique within their ownership scope.

## 5. Indexing Baseline

Index organization and status columns used in authorization and reporting. Add composite indexes for employee plus time range on assignments and attendance, request status plus created time, and event processing status plus next-attempt time. Confirm all indexes with query plans and production-like data.

## 6. Retention and Privacy

Retention periods for attendance, audit, and outbox data are policy decisions. Personal data should be minimized, access-controlled, encrypted in transit, and removed or anonymized according to organizational and legal requirements. Audit retention must not silently conflict with deletion obligations.

## 7. Open Decisions

Exact columns, nullable rules, partitioning, archival, soft deletion policy, tenant strategy, and migration tool are implementation decisions to be recorded before schema creation.
