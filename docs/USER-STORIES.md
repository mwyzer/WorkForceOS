# WorkforceOS User Stories

**Status:** Draft MVP backlog

## Employee

- **US-EMP-001** As an employee, I want to sign in securely so that I can access my workforce information.
- **US-EMP-002** As an employee, I want to view my assigned shifts so that I know when and where I work.
- **US-EMP-003** As an employee, I want to clock in and out and record breaks so that my attendance is captured accurately.
- **US-EMP-004** As an employee, I want to view attendance results so that I can identify lateness, early departure, or incomplete records.
- **US-EMP-005** As an employee, I want to submit and track leave and overtime requests so that I know their status.
- **US-EMP-006** As an employee, I want to create and submit a handover so that the next shift receives operational context.

## Supervisor

- **US-SUP-001** As a supervisor, I want to view my team's schedule and attendance so that I can manage daily operations.
- **US-SUP-002** As a supervisor, I want to validate attendance anomalies so that records can be corrected through an authorized process.
- **US-SUP-003** As a supervisor, I want to review and acknowledge handovers so that shift continuity is maintained.
- **US-SUP-004** As a supervisor, I want to approve or reject eligible requests so that employees receive timely decisions.

## Manager

- **US-MGR-001** As a manager, I want to create rosters and assign employees so that staffing matches operational needs.
- **US-MGR-002** As a manager, I want conflicts detected before publication so that employees are not double-booked.
- **US-MGR-003** As a manager, I want to publish a valid roster so that employees can rely on the schedule.
- **US-MGR-004** As a manager, I want operational reports so that I can monitor workforce performance.

## HR and Administrator

- **US-HR-001** As HR, I want to manage employee and organizational data so that workforce records remain current.
- **US-HR-002** As HR, I want attendance and workforce reports so that I can support operational decisions.
- **US-ADM-001** As an administrator, I want to manage users and roles so that access follows organizational responsibility.
- **US-ADM-002** As an administrator, I want health and audit visibility so that I can operate the platform safely.

## Cross-Cutting

- **US-PLAT-001** As an operator, I want important actions audited so that changes are accountable.
- **US-PLAT-002** As an API client, I want consistent errors and idempotent critical commands so that failures can be handled safely.
- **US-PLAT-003** As an operator, I want a view of the event outbox and received events so that delivery failures are visible.
- **US-PLAT-004** As an operator, I want health and outbox-health signals so that I can react before failures become user-visible.

## Workforce Risk Intelligence

- **US-RSK-001** As a manager, I want the platform to surface coverage shortfalls and staffing liquidity risks so that I can plan coverage before it is critical.
- **US-RSK-002** As a manager, I want each risk explained with severity, impact, and recommended actions so that I can decide what to do.
- **US-RSK-003** As a supervisor, I want a dashboard of open alerts that I can resolve so that follow-ups are tracked.
- **US-RSK-004** As a manager, I want to trigger an analysis on demand and see a trend of detections so that I can review changes over time.
- **US-RSK-005** As an analyst, I want recorded attendance and overtime dependency risks so that underlying patterns (lateness, overtime, single-coverage overnight shifts) are not missed.
