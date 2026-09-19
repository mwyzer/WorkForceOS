# WorkforceOS Development Progress

**Last updated:** September 2026 | **Source of truth:** working tree on `main` (see `git log` and `git status`)

This file is a living tracker for what is built, where each capability runs (in-memory vs PostgreSQL), how it is verified, and what is still open. It complements the phase checklist in [ROADMAP.md](ROADMAP.md): ROADMAP says *whether* a phase is done, PROGRESS records *what exactly* ships and what remains.

## 1. Overall Status

| Milestone | Status |
| --- | --- |
| Phase 0 — Foundation | ✅ Complete |
| Phase 1 — P0 MVP core | ✅ Complete |
| Phase 2 — P1 operational scale | ✅ Complete |
| Phase 3 — P2 intelligence and integrations | ✅ Complete |
| Phase 4 — Commercial SaaS rollout | 🚧 In progress (tenant isolation + org administration delivered; onboarding, packaging, RLS hardening pending) |
| Production release | Not started (release evidence and acceptance gate pending) |

## 2. Delivery Ledger by Capability

Legend: ✅ delivered · 🟡 partially delivered · ⬜ not started. Persistence column refers to the production (`default`) profile; the `test` profile uses in-memory stores for isolation.

| Capability | Backend | Persistence | API | Frontend | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Auth, users, roles, signed bearer tokens | ✅ | ✅ PostgreSQL (JPA + Flyway) | ✅ `/auth/login` | ✅ login | ✅ | ✅ |
| Admin account provisioning | ✅ | ✅ PostgreSQL | ✅ `/admin/accounts` | ⬜ | ✅ `OrganizationAdminTests` | ✅ |
| Organization CRUD + current tenant | ✅ | ✅ PostgreSQL | ✅ `/organizations*` | ⬜ | ✅ `OrganizationControllerTests` | ✅ |
| Tenant isolation (service-layer `TenantScope`) | ✅ | ✅ all tenant-owned tables carry `organization_id` | ✅ enforced across endpoints | n/a | ✅ `TenantIsolationTests` | ✅ |
| Workforce (departments, teams, employees) | ✅ | ✅ PostgreSQL | ✅ | ✅ employees page | ✅ | ✅ |
| Shifts, rosters, conflicts, publish | ✅ | ✅ PostgreSQL | ✅ | ⬜ | ✅ | ✅ |
| Attendance (clock in/out, breaks, calculation) | ✅ (breaks via engine) | ✅ PostgreSQL | 🟡 break events not exposed | ⬜ | ✅ | 🟡 |
| Leave / overtime requests + approval engine | ✅ | ✅ PostgreSQL | ✅ (leave reject endpoint ⬜) | ⬜ | ✅ | 🟡 |
| Shift swap + attendance correction | ✅ | ✅ PostgreSQL | ✅ | ⬜ | ✅ | ✅ |
| Handovers | ✅ | ✅ PostgreSQL | ✅ | ⬜ | ✅ | ✅ |
| Notifications (projection + delivery adapters) | ✅ | ✅ PostgreSQL | ✅ | ⬜ | ✅ | ✅ |
| Audit log | ✅ | ✅ PostgreSQL | ✅ | ⬜ | ✅ | ✅ |
| Reports + dashboard | ✅ | ✅ PostgreSQL (aggregation caches) | ✅ | ✅ home + dashboard service | ✅ | ✅ |
| Event bus / transactional outbox | ✅ | ✅ PostgreSQL | ✅ `/events/*` | n/a | ✅ | ✅ |
| Kafka relay | ✅ | ✅ (relay marker in outbox) | n/a | n/a | 🟡 embedded/H2 Kafka path | ✅ config-gated |
| Rate limiting | ✅ | n/a | ✅ HTTP 429 + `Retry-After` | n/a | ✅ | ✅ |
| Observability (correlation, errors, metrics, health) | ✅ | n/a | ✅ | n/a | ✅ | ✅ |
| Risk intelligence pipeline | ✅ | ✅ PostgreSQL | ✅ | ✅ risk page | ✅ | ✅ |
| AI workforce assistant + risk LLM providers (OpenAI/Muse/Spark) | ✅ `WorkforceLlmClient` shared OpenAI-compatible client (`com.workforceos.ai`) | n/a | ✅ `/assistant/ask`, `/risk/analyze` | ⬜ | ✅ `WorkforceLlmClientTests` + `AssistantControllerTests` | ✅ provider config-gated |
| Predictive analytics (absenteeism, forecasting) | ✅ | n/a | ✅ `/analytics/*` | ⬜ | ✅ | ✅ |
| Advisory auto-scheduling | ✅ | n/a | ✅ `/rosters/{id}/auto-schedule` | ⬜ | ✅ | ✅ |
| External integrations (payroll CSV, webhook, biometric) | ✅ | n/a | ✅ `/integrations*` | ⬜ | ✅ | ✅ config-gated |
| Playwright UI end-to-end | n/a | n/a | n/a | ✅ `frontend/e2e` | ✅ 5 specs + journey smoke | 🟡 requires local backend; not yet CI-wired |

## 3. Persistence and Migration State

- Flyway migrations `V1__init.sql` … `V6__notifications.sql` are the single source of truth; Hibernate runs `ddl-auto: validate` so drift fails startup.
- `V4__organizations.sql` added the tenant root and an `organization_id` discriminator (+ index) to every tenant-owned table; `outbox_events` keeps a nullable `organization_id` for system-originated events.
- Tenant isolation is enforced in application services via `TenantScope` (`require`/`force`/`assertAccess` → 404), keeping the same store compatible with the in-memory test profile.
- Known residual global reads: `NotificationDeliveryService.findDue` and the `AttendanceEngine.clockIn` eligibility probe (see [DATABASE-DESIGN.md](DATABASE-DESIGN.md) §9).

## 4. Verification State

| Discipline | Evidence | Status |
| --- | --- | --- |
| Backend unit + controller suite | `mvn -q test` (`test` profile, H2) | ✅ 149 tests, baseline recorded in [TEST-PLAN.md](TEST-PLAN.md) |
| JPA/Flyway round-trip | `PersistenceIntegrationTests` (H2 PostgreSQL mode, `ddl-auto=validate`) | ✅ |
| Latency/concurrency gate | `PerformanceSmokeTests` (p95 budget + 64-way burst) | ✅ per build |
| Tenant isolation + org admin | `TenantIsolationTests`, `OrganizationAdminTests`, `OrganizationControllerTests` | ✅ |
| UI end-to-end | Playwright Chromium: login, home, employees, risk, journey-smoke | 🟡 needs backend running locally; screenshots → `screenshot/` |
| Security/load/platform tests | Per [TEST-PLAN.md](TEST-PLAN.md) §6.4–§6.6 | ⬜ planned for release gate |

## 5. Open Items and Next Steps

High priority:

- Self-service tenant onboarding (signup → org creation → admin provisioning) on top of the admin API.
- Commercial packaging: plans/catalog, billing, seat entitlements, license enforcement.
- Deployment hardening: PostgreSQL Row-Level Security as a persistence-layer backstop; per-tenant connection/partition decision.
- Frontend tenant admin console.

Medium priority:

- Expose break-event recording and leave `reject` via API (the engine/event support exists).
- Wire Playwright E2E into CI (install chromium in the pipeline, start backend on demand).
- Publish an OpenAPI document; document pagination/sorting/filtering and rate-limit contract explicitly.

Tracked decisions:

- JWT adoption vs. current HMAC bearer token, token issuer/key rotation.
- Kafka consumer onboarding, topic/schema registry, dead-letter handling for the relay path.
- Report materialization strategy after baseline query measurements.

## 6. How to Update This File

When a phase or capability changes status, update this ledger, the matching phase line in [ROADMAP.md](ROADMAP.md), and re-check the related acceptance criteria in [ACCEPTANCE-CRITERIA.md](ACCEPTANCE-CRITERIA.md). Keep `Last updated` current. Prefer factual, verifiable statements over intent.