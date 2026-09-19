# WorkforceOS Test Plan

**Status:** Draft | **Scope:** MVP and platform foundations | **Implementation:** unit and controller suites run via Maven Surefire against the `test` profile (H2); Testcontainers/PostgreSQL integration suites are not yet wired; UI end-to-end runs via Playwright against the Angular app with the backend started locally

**Test disciplines in this plan:** unit, smoke, regression, and blackbox (API-level via HTTP) testing are defined in §6, alongside the Playwright UI end-to-end layer. Each discipline lists its run cadence and owner.

## 1. Objectives

Verify business rules, authorization, data integrity, API compatibility, reliability, and operational readiness. Highest risk areas are attendance concurrency, roster conflicts, self-approval, tenant isolation, idempotency, and overnight calculations.

## 2. Test Levels

Every level below maps to a discipline and a run cadence in §6.

- **Unit**: domain rules, state transitions, time calculations, validators, and authorization policies.
- **Integration**: Spring application services with PostgreSQL, transactions, migrations, repositories, and outbox writes.
- **API contract**: request/response schemas, status codes, error contract, pagination, and authentication.
- **End-to-end**: employee, manager, supervisor, HR, and admin workflows across module boundaries.
- **Security**: authentication failures, RBAC, resource scope, injection, secrets, headers, rate limiting, and audit coverage.
- **Performance**: p95 latency, concurrent attendance commands, pagination, report queries, and event throughput when enabled. Automated gate: `PerformanceSmokeTests` measures sequential p95 across representative read endpoints (budget: p95 < 1 s, single request < 2 s) and a 64-way concurrent burst that must all succeed within 5 s. Real figures come from a load harness (Gatling or JMeter) run against the Docker Compose stack: script a 5-minute soak of the read/report endpoints at 50 concurrent users plus a burst to 200 users, and capture the PRD targets — p95 reads < 500 ms, p95 writes < 1 s, concurrent attendance commands — before release.
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

Run unit and static checks on every change (`mvn test`). The current suite runs with the `test` Spring profile against an embedded H2 database; it covers the schedule engine, attendance engine and calculation, approval engine, risk rules/impact/advisor/persistence/API, event publisher and outbox architecture, the Kafka relay, observability (error contract, correlation headers, health indicator), organization/tenant isolation, and the operational controllers. `PersistenceIntegrationTests` runs the real JPA stores and Kafka relay against an H2 PostgreSQL-mode database with Flyway migrations and Hibernate `ddl-auto=validate`, mirroring the production startup path. `PerformanceSmokeTests` enforces a per-build latency/concurrency gate with generous budgets. PostgreSQL-backed integration tests (Testcontainers, e.g. embedded Kafka) and contract suites are planned for CI. Run end-to-end, security, performance, and resilience suites before release and after material infrastructure changes. Exact CI provider is TBD. Discipline-specific run commands and owners are consolidated in §6.6.

## 6. Test Disciplines and Strategies

This section defines four disciplines — unit, smoke, regression, blackbox (API) — plus the Playwright UI end-to-end layer, with scope, existing coverage, triggers, and ownership. Cadence and owners are summarized in §6.6.

### 6.1 Unit Testing

- **Goal:** verify domain rules, calculations, validators, services, and filters in isolation; fast, deterministic, no database or network.
- **Rules:** pure JUnit 5 with Mockito; must run in milliseconds; no Spring context where a plain constructor works; every `include`/correctness branch deserves a case.
- **Existing coverage (representative):** `AttendanceEngineTests`, `ScheduleEngineTests`, `ApprovalEngineTests`, `RiskEngineTests`, `RiskImpactCalculatorTests`, `HeuristicRiskAdvisorTests`, `AssistantIntentClassifierTests`, `GeofenceValidatorTests`, `BiometricClockServiceTests`, `PayrollCsvIntegrationTests`, `DemandForecastServiceTests`, `AbsenteeismAnalyticsServiceTests`, `EventPublisherTests`, `RateLimitFilterTests`.
- **Trigger:** every change — `mvn -q test` runs the full backend suite (`test` profile, H2).

### 6.2 Smoke Testing

- **Goal:** prove the application builds, boots, and answers core requests before deeper suites run.
- **Build smoke:** `WorkforceOsApplicationTests` loads the Spring context; `/api/v1/health` must return 200. Runs on every build. This is a *correctness* smoke and must not be confused with `PerformanceSmokeTests`, which is a latency/concurrency *gate* (§5).
- **Post-deploy smoke checklist** (manual/scripted against the running Docker Compose stack): health, login, list tenants/employees, one read report, one write flow (roster or attendance), audit entry appears. Owner: Ops/QA. Trigger: every deployment and after material infrastructure changes.
- **Artifacts:** the UI smoke step is automated by the Playwright `journey-smoke` spec (§6.5), which saves screenshots to `screenshot/`.

### 6.3 Regression Testing

- **Goal:** detect behavioral regressions across the whole product as the codebase grows.
- **Strategy:** run the complete suite (`mvn -q test`, currently **149 tests**) on every change and each release; compare against the recorded baseline (test count, failure/error deltas, and `PerformanceSmokeTests` budgets). Any drop in coverage count or a failing previously-green test blocks merge/release.
- **Golden-scenario registry:** real defects already fixed are locked by a checked scenario so they never reappear silently:
  - `BreakPeriodJson` must use the injected Spring `ObjectMapper` (a static instance lost Spring modules).
  - `NotificationProjector` must map roster IDs via `UUID::toString` before building notification bodies (`java.util.UUID` vs `String`).
  - `AbsenteeismAnalyticsService` window arithmetic must not lose precision (`plusDays(window / 2.0)` → `window / 2`).
  - Overnight attendance spans midnight must be calculated correctly.
  - A request must never be self-approved; cross-organization access must be rejected.
  - Shift swaps must enforce eligibility/license/match rules.
- **Owner:** Dev (fix) + QA (confirmation). **Trigger:** per-commit partial, full suite before every release.

### 6.4 Blackbox Testing — API Level via HTTP

- **Definition:** the running application is treated as a black box. Tests exercise only the public `/api/v1/*` contract over HTTP and assert status codes, JSON schemas and fields, authentication/RBAC behavior, tenant isolation, rate limiting (HTTP 429 + `Retry-After: 60`), validation/error contract, and pagination.
- **Approach (three options, lowest to highest fidelity):**
  1. **Scriptable smoke pass** — PowerShell/`RestClient`/`curl` calls against the Docker Compose stack covering one case per resource group (auth, employees, schedule, attendance, leave/overtime, reports, risk, dashboard, analytics, integrations).
  2. **JUnit HTTP suite** — `TestRestTemplate` against a packaged jar (`java -jar backend/target/...`) verifying the same contract programmatically in CI.
  3. **Contract collection** — Postman/Newman (or Spring Cloud Contract) exported from the API specification for cross-team reuse.
- **Data:** disposable organizations and users created through the API; never production personal data.
- **Owner:** QA. **Trigger:** nightly against the Compose stack; mandatory full pass before release.

### 6.5 UI End-to-End — Playwright

- **Goal:** blackbox the UI layer: drive the real Angular app (Karma/Jasmine unit tests are out of scope here), exercise user journeys through the browser, and capture visual evidence.
- **Scope:** login flow, home page, employees list, risk dashboard, and one full journey (login → employees → risk → home). Each spec ends by saving a full-page PNG.
- **Screenshots:** saved to the repository-root `screenshot/` folder (`frontend/playwright.config.ts` sets `outputDir: ../screenshot`; specs explicitly write `screenshot/<page>.png`). `.gitignore` excludes `screenshot/` because the artifacts are regenerated on every run.
- **Tooling:** `@playwright/test` (Chromium) as a devDependency of `frontend/`. Auth state is logged in once and reused via storageState, so specs exercise authorized flows without re-submitting the form.
- **Runbook** (requires the backend):
  1. Start the backend: `docker compose up -d` (or `mvn spring-boot:run` with seed data). Default seeded accounts: `admin`/`admin`, `manager`/`manager` (from `AuthService`).
  2. Start the frontend: `npm start` in `frontend/` (proxy `/api` → `localhost:8080`).
  3. Run: `npm run e2e` in `frontend/`. Browse `screenshot/` for artifacts.
- **CI:** install browsers with `npx playwright install chromium` (and OS dependencies on containers); run after unit+integration suites.
- **Owner:** QA. **Trigger:** nightly; pre-release.

### 6.6 Cadence and Ownership

| Discipline | Trigger | Command / evidence | Owner |
| --- | --- | --- | --- |
| Unit | Every change | `mvn -q test` (with `test` profile) | Dev |
| Smoke (build) | Every build | `WorkforceOsApplicationTests` + `/api/v1/health` | Dev/CI |
| Smoke (post-deploy) | Every deploy | Checklist + `screenshot/` journey-smoke artifacts | Ops/QA |
| Regression | Per-commit + pre-release | Full `mvn -q test`, compare baseline (149) + performance budgets | Dev/QA |
| Blackbox (API) | Nightly + pre-release | HTTP script / TestRestTemplate / Newman vs Compose stack | QA |
| UI E2E (Playwright) | Nightly + pre-release | `npm run e2e` in `frontend/`; `screenshot/*.png` | QA |

## 7. Defect Severity

- **Blocker**: data loss, security bypass, duplicate attendance, or unusable deployment.
- **Critical**: incorrect payroll-relevant calculation, unauthorized approval, tenant escape, or broken core workflow.
- **Major**: important feature failure with workaround.
- **Minor**: limited-impact defect or presentation issue.

Blocker and critical defects block release unless explicitly accepted by the product owner and security owner.

## 8. Exit Criteria

All P0 acceptance criteria pass, no unaccepted blocker/critical defects remain, migrations succeed from a clean database, security checks pass, performance targets have measured results, and deployment/rollback evidence is available.
