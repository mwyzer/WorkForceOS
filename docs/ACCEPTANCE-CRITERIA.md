# WorkforceOS Acceptance Criteria

**Status:** Draft MVP release gate

Each criterion is accepted only when the behavior, authorization, persistence, audit, and failure path are verified by automated tests or approved system evidence.

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
- Reports are organization-scoped and return correct data for attendance, workforce, leave, overtime, absence, and approvals.
- Health checks distinguish service readiness from dependency availability.
- Errors use the documented structure and include a trace identifier where available.
- Logs and metrics do not expose passwords, tokens, or unnecessary personal data.

## Release Evidence

Before MVP approval, attach API contract tests, integration test results, security test results, migration verification, performance measurements, and deployment rollback evidence. Unresolved deviations require product-owner approval.
