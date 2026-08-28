# WorkforceOS Software Requirements Specification

**Version:** 1.0 | **Status:** Draft | **Date:** August 2026

## 1. Purpose

This SRS translates the business and product requirements into a testable software contract for WorkforceOS, an enterprise workforce operations REST API.

## 2. Scope

The MVP covers authentication, RBAC, workforce and organizational data, shifts, rosters, attendance, leave, overtime, approvals, shift handover, audit logging, and operational queries. Notifications, caching, Kafka events, shift swaps, attendance corrections, advanced reporting, and rate limiting are P1 unless explicitly prioritized.

Payroll, recruitment, performance management, full HRIS, hardware integrations, mobile applications, and AI prediction are outside the initial release.

## 3. Actors

- `EMPLOYEE`: views assignments, records attendance, submits requests and handovers.
- `SUPERVISOR`: manages daily operations, validates attendance, reviews handovers, and approves requests.
- `MANAGER`: manages rosters, publishes schedules, approves requests, and views reports.
- `HR`: manages employee and organizational data and reports.
- `ADMIN`: manages users, roles, configuration, and health monitoring.

## 4. Functional Requirements

### Identity and access

- The system shall authenticate by username/email and password.
- Successful authentication shall return an access token.
- Protected resources shall reject missing, expired, malformed, or unauthorized tokens.
- Authorization shall enforce role and resource ownership rules.

### Workforce and scheduling

- Authorized users shall create, update, deactivate, and query employees.
- Employees shall belong to a department and team; positions may be recorded.
- Authorized users shall manage shift templates with start, end, overnight, and break definitions.
- Managers shall create rosters, assign employees, and publish valid rosters.
- The system shall reject overlapping assignments and invalid roster transitions.

### Attendance

- Eligible employees shall create clock-in, break-start, break-end, and clock-out events.
- The system shall prevent duplicate active clock-ins and invalid event sequences.
- Attendance calculation shall use the assigned shift, actual events, approved leave, and approved overtime.
- The system shall calculate lateness, early departure, absence, overtime, and incomplete attendance where applicable.
- Attendance writes shall be transactional and idempotent.

### Requests and approvals

- Employees shall submit and view leave and overtime requests.
- The approval engine shall support leave, overtime, shift swap, and attendance correction request types.
- Authorized supervisors/managers shall approve or reject requests.
- Request state changes and approval history shall be persisted and auditable.
- A requester shall not approve their own request.

### Handover, audit, and reporting

- Employees shall create handovers containing operational, task, incident, equipment, safety, and note items.
- Receiving shifts shall acknowledge submitted handovers.
- Important user actions, data changes, approvals, and domain events shall be audit logged.
- Authorized users shall query attendance, workforce, overtime, leave, absence, and approval reports.

## 5. Quality Requirements

- API targets: p95 reads below 500 ms and writes below 1 second, subject to load testing.
- Availability target: 99.9% monthly.
- The system shall use validation, transactions, secure headers, health checks, retries, and standardized errors.
- APIs shall support pagination for large collections and UTC timestamps at service boundaries.
- The service shall expose sufficient logs, metrics, and traces to diagnose failed requests and background processing.

## 6. State Models

- Roster: `DRAFT -> PUBLISHED -> COMPLETED`; `DRAFT/PUBLISHED -> CANCELLED`.
- Request: `PENDING -> APPROVED | REJECTED`; pending requests may be cancelled by policy.
- Handover: `DRAFT -> SUBMITTED -> ACKNOWLEDGED -> CLOSED`.
- Attendance: event stream of `CLOCK_IN`, `BREAK_START`, `BREAK_END`, and `CLOCK_OUT`.

## 7. Traceability

Detailed business rationale is in [BRD.md](BRD.md), product scope is in [PRD.md](PRD.md), and verification is defined in [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md) and [TEST-PLAN.md](TEST-PLAN.md).
