# WorkforceOS Functional Specification Document

**Version:** 1.0 | **Status:** Draft | **Date:** September 2026

## 1. Purpose

This FSD specifies the functional behavior of WorkforceOS in terms of inputs, processing, outputs, business rules, and state transitions. It complements the [SRS](SRS.md) by describing *how* each required capability behaves, and it aligns with the implemented backend (Spring Boot 3 / Java 21), persisted risk data, and Angular frontend.

References: [PRD](PRD.md), [BRD](BRD.md), [SRS](SRS.md), [ARCHITECTURE](ARCHITECTURE.md), [API-SPECIFICATION](API-SPECIFICATION.md), [EVENT-DESIGN](EVENT-DESIGN.md).

## 2. Functional Areas

1. Identity and access
2. Workforce management
3. Scheduling
4. Attendance
5. Requests and approvals
6. Shift handover
7. Workforce risk intelligence
8. Alerts and notifications
9. Reporting
10. Audit

## 3. Functional Specifications

Specifications use the identifiers `FS-<area>-<n>`. P1/P2 items are explicitly marked and are not part of the current build.

### 3.1 Identity and Access

| ID | Behavior |
| --- | --- |
| FS-ID-01 | Admin credentials (`admin`/`admin` by default) navigate to bootstrap the identity store when no user exists. |
| FS-ID-02 | `POST /api/v1/auth/login` accepts `username` and `password`, validates credentials, and returns a signed bearer token (HMAC-SHA256 over `username:expiry`, valid 1 hour); roles are resolved from the user account on each request. |
| FS-ID-03 | Protected endpoints reject missing, expired, malformed, or unauthorized tokens with a standardized error response; authorization enforces role and ownership rules. |
| FS-ID-04 | Password hashing uses a modern adaptive algorithm; plaintext passwords are never stored or logged. |
| FS-ID-05 | Roles `ADMIN`, `HR`, `MANAGER`, `SUPERVISOR`, `EMPLOYEE` gate capabilities per section 4. |
| FS-ID-06 | Every tenant-owned read and write is scoped to the caller's organization via `TenantScope` (`require`/`force`/`assertAccess`); a write is forced to the current tenant and single-resource reads from another organization return `404` so tenants cannot probe each other. |
| FS-ID-07 | `ADMIN` users manage organizations: list, read, and create (`GET/POST /api/v1/organizations`), update (`GET/PATCH /api/v1/organizations/{id}`), and resolve the caller's tenant (`GET /api/v1/organizations/current`); any other role receives `403`. |
| FS-ID-08 | `ADMIN` provisions user accounts owned by a specific organization via `POST /api/v1/admin/accounts`; missing required fields return `400`, existing usernames return `409`, and the provisioned account authenticates against its own organization only. |

### 3.2 Workforce Management

| ID | Behavior |
| --- | --- |
| FS-WF-01 | Authorized users create, update, and query employees with department, team, position, and contact details. |
| FS-WF-02 | Authorized users deactivate employees; deactivation publishes the `EmployeeDeactivated` domain event, which triggers a risk recompute. |
| FS-WF-03 | Users and roles are managed by `ADMIN`; user-role changes are authoritative for access decisions. |
| FS-WF-04 | The system rejects modifications to employees that violate dependency rules (e.g., deactivating an employee with active assignments is allowed but immediately re-evaluated for risk). |

### 3.3 Scheduling

| ID | Behavior |
| --- | --- |
| FS-SC-01 | Managers create shift templates defining start, end, duration, overnight flag, and break definitions. |
| FS-SC-02 | Managers create rosters, assign employees, and validate conflicts before content is visible to employees. |
| FS-SC-03 | The system rejects overlapping assignments for the same employee. |
| FS-SC-04 | Roster lifecycle: `DRAFT -> PUBLISHED -> COMPLETED` and `DRAFT/PUBLISHED -> CANCELLED`. |
| FS-SC-05 | Publishing a roster publishes the `RosterPublished` event, which triggers a risk recompute. |
| FS-SC-06 | Overnight shifts span calendar days; attendance and risk rules treat them as single continuous assignments. |

### 3.4 Attendance

| ID | Behavior |
| --- | --- |
| FS-AT-01 | An assigned employee creates clock-in, break-start, break-end, and clock-out events. |
| FS-AT-02 | The system prevents two active clock-ins for the same employee and rejects out-of-sequence transitions. |
| FS-AT-03 | Attendance calculation takes the assigned shift, actual events, approved leave, and approved overtime into account; it supports overnight shifts. |
| FS-AT-04 | Calculation produces lateness, early departure, absence, overtime, and incomplete-attendance outcomes as applicable. |
| FS-AT-05 | Attendance writes are transactional and idempotent; duplicates from retried requests do not create multiple records. |

### 3.5 Requests and Approvals

| ID | Behavior |
| --- | --- |
| FS-AP-01 | Employees submit leave, overtime, shift swap, and attendance correction requests (`PENDING`). |
| FS-AP-02 | The reusable approval engine handles all request types with a single state machine: `PENDING -> APPROVED | REJECTED`; pending requests may be cancelled by policy. |
| FS-AP-03 | Only authorized supervisors/managers act on requests; a requester can never approve their own request. |
| FS-AP-04 | Approval of an approved-eligible leave or overtime request publishes the respective domain event, which triggers a risk recompute. |
| FS-AP-05 | State transitions and approval history are persisted and audit logged; approved requests require a correction process before modification. |
| FS-AP-06 | Shift swap and attendance correction workflows are implemented with the same approval engine and persisted through PostgreSQL JPA stores; break-event recording stays pending. |

### 3.6 Shift Handover

| ID | Behavior |
| --- | --- |
| FS-HO-01 | Employees create handovers with operational, task, incident, equipment, safety, and note items (`DRAFT`). |
| FS-HO-02 | Submission moves a handover to `SUBMITTED`; content is no longer editable unless returned to an editable state. |
| FS-HO-03 | The receiving shift acknowledges a submitted handover (`ACKNOWLEDGED`), then it closes (`CLOSED`). |
| FS-HO-04 | Every transition is audited with the acting user and timestamp. |

### 3.7 Workforce Risk Intelligence

The pipeline converts operational data into structured, explained, and persisted risks with mitigation recommendations.

| ID | Behavior |
| --- | --- |
| FS-RK-01 | The risk engine consumes a snapshot of employees, rosters, assignments, attendance records, approved leave/overtime, and swap coverage. |
| FS-RK-02 | Five deterministic rules produce risk assessments: `COVERAGE_SHORTFALL`, `STAFFING_LIQUIDITY`, `ATTENDANCE_TREND`, `OVERTIME_DEPENDENCY`, and `SINGLE_POINT_OF_FAILURE`. |
| FS-RK-03 | Each assessment records risk type, severity, score, entity type/ID, team, department, window start/end, impact minutes, and evidence. |
| FS-RK-04 | `RiskImpactCalculator` derives a composite risk index as a severity-weighted average of assessment scores (weights `HIGH` 1.0, `MEDIUM` 0.6, `LOW` 0.3), computes slot coverage from uncovered assignments, and expresses impact as uncovered labor hours and an estimated labor cost. |
| FS-RK-05 | Severity bands are `HIGH` (risk index ≥ 80), `MEDIUM` (50–79), and `LOW` (< 50). |
| FS-RK-06 | Analysis is invoked on demand (`POST /api/v1/risk/analyze`) and automatically on `RosterPublished`, `LeaveApproved`, `OvertimeApproved`, and `EmployeeDeactivated` events. |
| FS-RK-07 | A scheduled recompute runs on the configurable `workforce.risk.recompute-cron` (default `0 0 */6 * * *`). |
| FS-RK-08 | The composite advisor (LLM first when `ai-provider=openai`, `muse`, or `spark` is configured and available, heuristic otherwise) adds a natural-language explanation, impact summary, and structured recommendations; any LLM failure falls back to the heuristic advisor. |
| FS-RK-09 | The mitigation catalog contributes deterministic recommendations keyed by risk type (e.g., `CREATE_SHIFT_SWAP`, `CREATE_OVERTIME`, `NOTIFY_MANAGER`) that are merged with LLM output and de-duplicated by action type. |
| FS-RK-10 | Assessments are persisted and de-duplicated: a new analysis reuses the existing assessment for the same `type:entityId:windowStartMillis` instead of creating a duplicate. Sliding windows are stabilized to Monday (`weekBucket`) for liquidity and the 1st of month (`monthBucket`) for attendance/overtime. |
| FS-RK-11 | Assessments that reach `HIGH` or `MEDIUM` create or update an alert (`OPEN`), de-duplicated by assessment ID. |
| FS-RK-12 | `POST /api/v1/risk/alerts/{id}/resolve` marks an alert `RESOLVED`; resolving a non-existent alert is idempotent and returns no error. |
| FS-RK-13 | `GET /api/v1/risk/summary` returns the deterministic dashboard summary: overall risk index, coverage percentage, assessment counts by severity, open alerts, latest analysis, advisor mode, and per-risk-type aggregates. |
| FS-RK-14 | The Angular risk dashboard (`/risk`) renders the summary KPIs, the six-stage pipeline infographic (Workforce data → Risk engine → Structured risk → LLM analysis → Recommendations → Alert and action), advisor mode, alerts with resolve actions, and expandable assessments with recommendations. |
| FS-RK-15 | Configuration under `workforce.risk` (`application.yml`, env-overridable) controls horizons, thresholds, labor rate, recompute cron, and LLM settings; the heuristic advisor guarantees offline operation. |

### 3.8 Alerts and Notifications

| ID | Behavior |
| --- | --- |
| FS-NT-01 | Risk alerts follow `OPEN -> RESOLVED`; only users with appropriate access can resolve alerts. |
| FS-NT-02 | Notification intents are created via the API and generated automatically by `NotificationProjector` from domain events with delivery status (`PENDING`/`SENT`/`DELIVERED`/`FAILED`/`READ`); `NotificationDeliveryService` dispatches through per-channel adapters (in-app, logging) with attempt tracking, backoff retry, and a scheduled delivery processor. External provider channels (email, push, webhook) remain on the roadmap. |

### 3.9 Reporting

| ID | Behavior |
| --- | --- |
| FS-RP-01 | Operational reports cover attendance, workforce, overtime, leave, absence, and approvals, queried via `GET /api/v1/reports/*`. |
| FS-RP-02 | The risk summary endpoint (§3.7) acts as the risk intelligence report consumed by the dashboard. |
| FS-RP-03 | Pagination and UTC timestamps are applied at collection/report boundaries. |

### 3.10 Audit

| ID | Behavior |
| --- | --- |
| FS-AU-01 | Important user actions, data changes, approvals, and domain events are recorded with actor, action, resource, resource ID, timestamp, and before/after values where permitted. |
| FS-AU-02 | Audit records are append-only and queryable via `GET /api/v1/audit-logs`. |

## 4. Capability Matrix by Role

| Capability | ADMIN | HR | MANAGER | SUPERVISOR | EMPLOYEE |
| --- | :-: | :-: | :-: | :-: | :-: |
| Login and view own data | ✓ | ✓ | ✓ | ✓ | ✓ |
| Organization admin and account provisioning | ✓ | – | – | – | – |
| Employee CRUD and deactivation | ✓ | ✓ | – | – | – |
| Shift templates and rosters | – | – | ✓ | ✓ | – |
| Publish rosters | – | – | ✓ | ✓ | – |
| Record own attendance | – | – | – | ✓ | ✓ |
| Submit requests | – | – | ✓ | ✓ | ✓ |
| Approve requests | – | – | ✓ | ✓ | – |
| Handovers: create | – | – | – | ✓ | ✓ |
| Handovers: acknowledge | – | – | – | ✓ | ✓ |
| Risk summary, audits, assessments | ✓ | ✓ | ✓ | ✓ | – |
| Resolve risk alerts | ✓ | ✓ | ✓ | ✓ | – |

## 5. Cross-Cutting Rules

- An employee must have an active assignment before recording attendance.
- Employees cannot be assigned to overlapping shifts.
- Users cannot approve their own requests.
- All tenant-owned data is scoped to the caller's organization; access to another organization's resources returns `404`.
- Approved requests are immutable unless corrected by an authorized workflow.
- Submitted handovers are not editable unless returned to an editable state.
- Critical operations (attendance writes, risk analysis runs) are idempotent and transactional.
- All timestamps use UTC at service boundaries; risk windows are bucketed per configurable locale rules (Monday/1st-of-month).
- Adapters for external providers (LLM, future integrations) are called from application services through ports, never from controllers.

## 6. Configuration Reference (Implemented)

| Key | Default | Purpose |
| --- | --- | --- |
| `workforce.risk.liquidity-days` | `14` | Liquidity forecasting horizon. |
| `workforce.risk.attendance-min-sessions` | `4` | Minimum sessions before attendance trend is scored. |
| `workforce.risk.attendance-issue-threshold` | `0.25` | Issue-rate threshold for attendance rule. |
| `workforce.risk.overtime-window-days` | `30` | Rolling window for overtime dependency. |
| `workforce.risk.overtime-high-hours` | `20` | Hours band for `HIGH` overtime dependency. |
| `workforce.risk.overtime-medium-hours` | `10` | Hours band for `MEDIUM` overtime dependency. |
| `workforce.risk.labor-rate-per-hour` | `50` | Labor rate for impact estimation. |
| `workforce.risk.recompute-cron` | `0 0 */6 * * *` | Scheduled risk recompute. |
| `workforce.risk.ai-provider` | `none` | LLM provider: `none` (heuristic), `openai`, `muse`, or `spark`. |
| `workforce.risk.ai-base-url` / `ai-model` / `ai-api-key` | | Generic LLM endpoint configuration (server-side, never exposed); muse/spark default to these when their own keys are unset. |
| `workforce.risk.muse-base-url` / `muse-model` / `muse-api-key` | | **Muse** provider settings; `muse-model` defaults to `muse`. |
| `workforce.risk.spark-base-url` / `spark-model` / `spark-api-key` | | **Spark** provider settings; `spark-model` defaults to `spark-1.3`. |
| `workforce.risk.ai-timeout-ms` | `15000` | LLM call timeout (muse/spark inherit it unless `muse-timeout-ms` / `spark-timeout-ms` are set). |
| `workforce.assistant.ai-provider` | falls back to `workforce.risk.ai-provider` | LLM provider for the workforce assistant: `none`, `openai`, `muse`, or `spark`. |
| `workforce.assistant.ai-base-url` / `ai-model` / `ai-api-key` | | Assistant LLM endpoint configuration, defaulting to the risk settings. |
| `workforce.assistant.muse-*` / `spark-*` | | Assistant Muse/Spark settings (same shape as risk, falling back to the assistant generic keys). |

## 7. State Models

- Roster: `DRAFT -> PUBLISHED -> COMPLETED`; `DRAFT/PUBLISHED -> CANCELLED`.
- Request: `PENDING -> APPROVED | REJECTED`; pending requests may be cancelled by policy.
- Handover: `DRAFT -> SUBMITTED -> ACKNOWLEDGED -> CLOSED`.
- Attendance: event stream of `CLOCK_IN`, `BREAK_START`, `BREAK_END`, `CLOCK_OUT`.
- Risk alert: `OPEN -> RESOLVED`.
- Risk assessment: append-only; a new analysis replaces/updates the assessment for the same dedupe key and window.

## 8. Traceability

- PRD functional requirements map to the FS-* behaviors above; the SRS lists the normative shall-statements.
- Verification lives in [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md) and [TEST-PLAN.md](TEST-PLAN.md). Implemented coverage includes unit tests for the risk engine, impact calculator, advisors, and risk API, plus controller tests for the operational modules.