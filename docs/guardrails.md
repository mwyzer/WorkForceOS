# WorkforceOS — Guardrails

**Purpose:** the non-negotiable invariants of this codebase. Anyone — human or AI agent — changing backend code must not violate these without an explicit, documented decision. Each rule links to where it's enforced today and the doc that describes it in full. This file is a checklist to consult *before* a change, not a restatement of [spec.md](spec.md).

Rules are phrased as "must" / "must never" on purpose — treat them as a diff review checklist.

---

## 1. Tenant Isolation

- **Every tenant-owned table carries `organization_id NOT NULL`, and every read/write to it must go through `TenantScope`.** Enforcement is at the service layer (`TenantScope.require()` / `.force()` / `.assertAccess()`), not the database — there is no PostgreSQL Row-Level Security backstop yet, so a missed `TenantScope` call is a silent cross-tenant leak, not a rejected query.
- **Single-resource reads across tenants must return `404`, never `403` or the resource itself.** `TenantScope.assertAccess()` exists specifically so a caller cannot distinguish "not found" from "belongs to another org."
- **New tenant-owned entities must add `organization_id` in the same Flyway migration that creates the table**, plus an index (`idx_<table>_organization_id`), and must be wired into the relevant service via `TenantScope` before merge.
- **Background jobs and event projectors are the one place tenant scoping is easy to forget.** `RiskAnalysisScheduler` and `NotificationProjector` derive tenant from the triggering event's `organization_id`; any new scheduled job or projector must do the same rather than reading globally.
- Two residual global reads are known and accepted by design — `NotificationDeliveryService.findDue` and the `AttendanceEngine.clockIn` presence probe (writes stay tenant-correct; see [DATABASE-DESIGN.md §9](DATABASE-DESIGN.md)). Do not silently add a third; if a new global read is unavoidable, document it in the same place.

*Enforced in:* `backend/main/java/com/workforceos/organization/TenantContext*.java`, `TenantScope` usages (17 files today). *Full detail:* [DATABASE-DESIGN.md §4/§9](DATABASE-DESIGN.md), [SECURITY.md §1](SECURITY.md).

## 2. Authorization

- **Deny by default.** Every new endpoint needs an explicit role check; there is no "authenticated = allowed" fallback.
- **A user must never approve or reject their own request.** `ApprovalEngine.decide()` rejects when `flow.registrar()` equals the acting user — any new approval-style workflow must repeat this check, not assume the generic engine is always in the call path.
- **Organization administration (`/organizations*`, `/admin/accounts`) is `ADMIN`-only**, gated by `CurrentUser.requireAdmin()`. Do not relax this to `MANAGER`/`HR` without a product decision.
- **A provisioned account authenticates against its own organization only.** Don't add a code path that lets a login resolve to an organization other than the one it was provisioned under.

*Enforced in:* `backend/main/java/com/workforceos/approval/ApprovalEngine.java`, `authentication/`, `organization/OrganizationController.java`. *Full detail:* [SECURITY.md §1](SECURITY.md).

## 3. Attendance & Scheduling Integrity

- **An employee cannot have more than one active clock-in session at a time.** Any change to `AttendanceEngine` must preserve this check before accepting a new `clock-in`.
- **Roster assignments for the same employee must not overlap.** Conflict detection runs before a roster assignment is persisted — never bypass it for "quick fixes" or bulk-import paths.
- **An employee needs an active assignment before attendance can be recorded against it.**
- **Overnight shifts (crossing midnight) must remain a first-class case** in every attendance/lateness calculation — do not add a code path that assumes `endTime > startTime`.

*Enforced in:* `backend/main/java/com/workforceos/schedule/ScheduleEngine.java`, `attendance/AttendanceEngine.java`. *Full detail:* [PRD.md §11–§12](PRD.md), [FSD.md](FSD.md).

## 4. Approved-State Immutability & Audit

- **Approved requests (leave, overtime, shift swap) are not modified directly.** Changes go through the attendance-correction workflow, not a raw update to the approved record.
- **Submitted handovers are not editable** unless explicitly returned to an editable state by its own state machine.
- **Audit records are append-only.** Never expose an update or delete path for `audit_logs` — including from admin tooling or data-fix scripts.
- **Every approval action, roster publish, and handover transition must produce an audit record** (actor, action, resource, resource ID, timestamp). If you add a new state-changing endpoint in these domains, add the audit write in the same change, not as a follow-up.

*Enforced in:* `backend/main/java/com/workforceos/scheduling/AuditLogController.java` and callers. *Full detail:* [SECURITY.md §4](SECURITY.md).

## 5. Idempotency & Duplication

- **There is no generic `Idempotency-Key` HTTP header middleware today** — don't assume one exists when reviewing or extending a controller. Idempotency is currently achieved per-domain via dedupe keys (e.g. `risk_assessments.dedupe_key` = `type:entityId:windowStartMillis`, the outbox's event-id uniqueness, biometric clock-event dedupe). [PRD.md §25](PRD.md) previously showed this header as if implemented; that section now points here.
- **If you add a new dedupe key, make it a real unique constraint in the Flyway migration**, not just an application-level check — the risk module's `uq_risk_assessments_dedupe_key` is the pattern to follow.
- **Repeated risk analysis runs, event redelivery, and notification retries must not create duplicate rows.** Test this explicitly for any new recompute or retry path, not just the happy path.

*Enforced in:* `backend/main/java/com/workforceos/risk/RiskAssessmentRepository.java`, `event/EventPublisher.java`. *Full detail:* [DATABASE-DESIGN.md §2](DATABASE-DESIGN.md), [EVENT-DESIGN.md](EVENT-DESIGN.md).

## 6. Schema & Persistence

- **Schema changes go through a new versioned Flyway migration file (`V<n+1>__*.sql`), never an edit to an already-applied migration.** `ddl-auto: validate` means Hibernate will fail startup on drift by design — that failure is a signal to fix the migration, not to loosen `ddl-auto`.
- **Never enable `ddl-auto: update` or `create` outside the `test` profile.**
- **The `test` profile's in-memory/H2 stores must stay behaviorally equivalent to the PostgreSQL path** (same tenant scoping, same conflict rules) — a passing test on H2 that would violate a PostgreSQL constraint is a bug in the test double, not a pass.

*Full detail:* [DATABASE-DESIGN.md §1](DATABASE-DESIGN.md), [DEPLOYMENT.md §5](DEPLOYMENT.md).

## 7. Module Boundaries

- **A module must not write to another module's tables directly.** Cross-module effects go through that module's application service or an emitted domain event — not a shared repository call.
- **Controllers stay thin.** Business rules belong in application/domain services (`ScheduleEngine`, `AttendanceEngine`, `ApprovalEngine`, `RiskAnalysisService`, …), not in `@RestController` methods.
- **External calls (LLM providers, payroll/webhook/biometric integrations) must go through their module's port** (`RiskAdvisorPort`, `OutboundIntegration`) — never call an external HTTP endpoint directly from a controller or domain object.
- **Integration adapters must degrade to `SKIPPED`/disabled when unconfigured, not throw or block startup.** Every adapter is opt-in via a `*_ENABLED` flag, off by default.

*Full detail:* [ARCHITECTURE.md §5](ARCHITECTURE.md).

## 8. AI / LLM Usage

- **LLM provider credentials (base URL, API key) are server-side config only** — never returned in an API response or exposed to the frontend.
- **Every LLM-backed feature (risk advisor, assistant) must have a working heuristic/offline fallback.** A missing or failing LLM call must degrade gracefully, not error out to the caller — this is a load-bearing product property (the system must work fully offline), not an implementation detail.
- **Do not log prompt payloads or raw LLM responses.** Mask names/identifiers in any analysis text that does get logged.
- **Feed the LLM only the operational slice needed for the task at hand** (roster/attendance/leave/overtime/swap snapshots for risk analysis) — not full personal profiles.

*Enforced in:* `backend/main/java/com/workforceos/ai/WorkforceLlmClient.java`, `risk/` heuristic advisor, `assistant/HeuristicAssistant.java`. *Full detail:* [SECURITY.md §6](SECURITY.md).

## 9. API Contract

- **Base path stays `/api/v1`.** A breaking change to an existing `/api/v1` response shape needs a new version path, not an in-place change.
- **Error responses keep the standard shape** (`timestamp`, `status`, `code`/`error`, `message`, `path`, `traceId`, `requestId`) — don't return ad hoc error bodies from a new controller.
- **Collection endpoints must be paginated** (`page`, `size`) with the standard envelope (`content`, `page`, `size`, `totalElements`, `totalPages`) — don't add an unpaginated `List<...>` endpoint for a table that can grow unbounded.
- **Health, metrics, and outbox/event inspection endpoints are intentionally global (never tenant-scoped).** Don't tenant-scope them to "be consistent" — that would break operational monitoring.

*Full detail:* [API-SPECIFICATION.md](API-SPECIFICATION.md), [OBSERVABILITY.md §1.1](OBSERVABILITY.md).

## 10. Testing

- **A change to a business rule (conflict detection, approval, attendance calculation, tenant scoping) needs a test that fails without the change**, not just a passing test after it.
- **Tenant-isolation changes need a `TenantIsolationTests`-style case**: prove a cross-tenant read 404s and a write lands under the correct `organization_id`.
- **Don't let the backend suite regress from green.** Current baseline is 149 passing tests (`mvn test`, `test` profile, H2) — a merge that drops this without an explicit, reviewed reason is a red flag, not a formatting issue.

*Full detail:* [TEST-PLAN.md](TEST-PLAN.md).

---

## What this file is not

This is not a security policy, a threat model, or a test plan — see [SECURITY.md](SECURITY.md) and [TEST-PLAN.md](TEST-PLAN.md) for those. It is not a status tracker — see [PROGRESS.md](PROGRESS.md) for what's built versus pending. It exists so a change that *looks* locally correct doesn't quietly break a cross-cutting invariant (tenant isolation, auditability, idempotency, offline LLM fallback) that isn't obvious from the file being edited.

**Maintaining this file:** add a rule here only when breaking it would cause a cross-cutting failure (data leak, duplicate record, silent audit gap) rather than a local bug — local bugs belong in tests, not in this list. Update a rule's "Enforced in" pointer if the responsible class is renamed or moved.
