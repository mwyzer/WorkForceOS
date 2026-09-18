# WorkforceOS Software Requirements Specification

**Version:** 1.0 | **Status:** Draft | **Date:** August 2026 | **Implementation tracking:** see change log across this doc set

## 1. Purpose

This SRS translates the business and product requirements into a testable software contract for WorkforceOS, an enterprise workforce operations REST API.

## 2. Scope

The MVP covers authentication, RBAC, workforce and organizational data, shifts, rosters, attendance, leave, overtime, approvals, shift handover, audit logging, operational queries, and workforce risk intelligence (risk engine, LLM/heuristic analysis, persisted assessments and alerts, dashboard UI). Caching (Redis read-through), notifications, the transactional PostgreSQL outbox/event bus with optional Kafka relay, shift swaps, attendance corrections, advanced reports and dashboards, API rate limiting, an AI workforce assistant (offline heuristic with optional LLM), predictive absenteeism analytics, demand forecasting, advisory auto-scheduling, and a config-gated external integration framework (payroll CSV/webhook outbound, biometric clock ingestion with geofencing) are implemented in the current build; concrete vendor rollouts require business approval.

Payroll, recruitment, performance management, full HRIS, hardware integrations, mobile applications, and deeper AI prediction are outside the initial release.

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

### Workforce risk intelligence

- The system shall analyze workforce data and detect coverage shortfalls, staffing liquidity, attendance trends, overtime dependency, and single-point-of-failure risks.
- Risk analysis shall run on demand via `POST /api/v1/risk/analyze` and automatically on roster publication, leave approval, overtime approval, and employee deactivation events.
- The system shall support a periodic recompute (configurable cron) and reuse LLM or heuristic analysis for scheduled and event-driven runs.
- Risk assessments shall be persisted, deduplicated by risk type, entity, and analysis window, and queryable via `GET /api/v1/risk/assessments`.
- The system shall return an explanation, severity score, and structured recommendations for each risk assessment, with catalog-based fallback when no LLM is configured or the provider call fails.
- Severity shall be banded as `HIGH` (risk index ≥ 80), `MEDIUM` (50–79), and `LOW` (< 50).
- The system shall publish alerts for assessed risks and expose an `OPEN` / `RESOLVED` lifecycle via `GET /api/v1/risk/alerts` and `POST /api/v1/risk/alerts/{id}/resolve`.
- The frontend risk dashboard shall display the latest risk summary, pipeline overview, active alerts, and persisted assessments with recommendations.

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
- Risk alert: `OPEN -> RESOLVED`; risk assessment is append-only (new versions for updated windows).

## 7. Traceability

Detailed business rationale is in [BRD.md](BRD.md), product scope is in [PRD.md](PRD.md), and verification is defined in [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md) and [TEST-PLAN.md](TEST-PLAN.md).
