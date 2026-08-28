# WorkforceOS Test Plan

**Status:** Draft | **Scope:** MVP and platform foundations

## 1. Objectives

Verify business rules, authorization, data integrity, API compatibility, reliability, and operational readiness. Highest risk areas are attendance concurrency, roster conflicts, self-approval, tenant isolation, idempotency, and overnight calculations.

## 2. Test Levels

- **Unit**: domain rules, state transitions, time calculations, validators, and authorization policies.
- **Integration**: Spring application services with PostgreSQL, transactions, migrations, repositories, and outbox writes.
- **API contract**: request/response schemas, status codes, error contract, pagination, and authentication.
- **End-to-end**: employee, manager, supervisor, HR, and admin workflows across module boundaries.
- **Security**: authentication failures, RBAC, resource scope, injection, secrets, headers, rate limiting, and audit coverage.
- **Performance**: p95 latency, concurrent attendance commands, pagination, report queries, and event throughput when enabled.
- **Resilience**: database outage, retry, duplicate delivery, dependency timeout, malformed event, and restart recovery.

## 3. Core Scenarios

- Create and publish a roster without conflicts.
- Reject overlapping assignments, invalid lifecycle transitions, and unauthorized publication.
- Clock in, take breaks, clock out, and calculate present/late/early/overtime outcomes.
- Reject duplicate active clock-ins and verify idempotent retries.
- Calculate attendance across midnight for overnight shifts.
- Submit, approve, reject, cancel, and audit leave and overtime requests.
- Prevent request self-approval and cross-organization access.
- Submit and acknowledge a handover while preventing unauthorized edits.
- Verify reports and audit entries match committed transactional data.

## 4. Test Data

Use isolated organizations, users for every role, active/inactive employees, day and overnight shifts, conflicting assignments, approved leave, approved overtime, incomplete attendance, and duplicate command keys. Do not use production personal data.

## 5. Automation and Environments

Run unit and static checks on every change. Run integration and contract suites in CI with an ephemeral PostgreSQL instance. Run end-to-end, security, performance, and resilience suites before release and after material infrastructure changes. Exact tools and CI provider are TBD.

## 6. Defect Severity

- **Blocker**: data loss, security bypass, duplicate attendance, or unusable deployment.
- **Critical**: incorrect payroll-relevant calculation, unauthorized approval, tenant escape, or broken core workflow.
- **Major**: important feature failure with workaround.
- **Minor**: limited-impact defect or presentation issue.

Blocker and critical defects block release unless explicitly accepted by the product owner and security owner.

## 7. Exit Criteria

All P0 acceptance criteria pass, no unaccepted blocker/critical defects remain, migrations succeed from a clean database, security checks pass, performance targets have measured results, and deployment/rollback evidence is available.
