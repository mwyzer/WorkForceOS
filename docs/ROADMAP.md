# WorkforceOS Roadmap

**Status:** Draft | **Planning horizon:** MVP and post-MVP | **Tracked:** delivered items below reflect the current build

Dates are intentionally omitted until implementation capacity, dependencies, and product-owner priorities are confirmed.

## Phase 0: Foundation

- [x] Confirm access boundaries and RBAC baseline (default `admin`/`manager` accounts, role enforcement per route).
- [x] Establish Java 21/Spring Boot 3 project structure with dedicated domain packages.
- [x] Set coding, migration (Flyway), API versioning (`/api/v1`), and conventions.
- [x] Implement configuration, health checks, error contract, request correlation, and security baseline.
- [x] Define tenant/organization model and isolation strategy (organizations table, per-table `organization_id` discriminator, `TenantContext` resolution per request; see `DATABASE-DESIGN.md` §9).

## Phase 1: P0 MVP Core

- [x] Authentication, users, roles, and RBAC.
- [x] Departments, teams, positions, and employees (PostgreSQL via Flyway).
- [x] Shift templates with breaks and overnight support; roster lifecycle and conflict detection.
- [x] Attendance events, transactional clock in/out, and calculation (in-memory engine).
- [x] Leave and overtime requests with a reusable approval engine.
- [x] Handover creation, submission, acknowledgement.
- [x] Audit log, notifications, and baseline operational reports.
- [x] PostgreSQL persistence for schedule, attendance, handover, approval, audit, and notification data (verified via JPA stores + Flyway migrations + Hibernate schema validation in `PersistenceIntegrationTests`).

## Phase 2: P1 Operational Scale

- [x] Redis read-through caching for measured hot paths (report/lookup results).
- [x] In-memory outbox and event bus with deduplicated delivery, bounded retry, and inspection endpoints.
- [x] Attendance correction and shift swap workflows (approval-driven).
- [x] Transactional PostgreSQL outbox and Kafka event delivery (transactional `outbox_events` writes, `KafkaRelayer` publishes to `workforce.domain.events` with idempotent producer and relay tracking via `kafka_published_at`; disabled by default through `workforce.events.kafka.enabled`).
- [x] Notification generation from domain events and delivery adapters with delivery status (`NotificationProjector` maps events to in-app notifications; `NotificationDeliveryService` dispatches via per-channel adapters with `PENDING`/`SENT`/`DELIVERED`/`FAILED`/`READ` status, attempt tracking, backoff retry, and a scheduled delivery processor).
- [x] Advanced reports, dashboards, and API rate limiting (`/api/v1/reports/attendance-by-employee`, `/overtime-by-employee`, `/department-staffing`; `/api/v1/dashboard/operations`; token-bucket `RateLimitFilter` returning 429 + `Retry-After`, excludes health/actuator).
- [x] Load testing against latency and concurrency targets (`PerformanceSmokeTests` per-build gate for p95/latency budgets and a 64-way concurrent burst; load-harness methodology and PRD latency targets in `TEST-PLAN.md`).

## Phase 3: P2 Intelligence and Integrations

- [x] Workforce risk intelligence (heuristic risk rules, optional LLM analysis, persisted assessments, alerts, and recommendations; dashboard on the Angular frontend) — delivered ahead of roadmap order.
- [x] AI workforce assistant (offline intent-classified answers grounded in live workforce data via `/api/v1/assistant/ask`, with optional OpenAI-compatible LLM answering and heuristic fallback; see `com.workforceos.assistant`).
- [x] Predictive workforce analytics and absenteeism insights (`GET /api/v1/analytics/absenteeism` scores per-employee absence/late rates, trend, and drivers; `GET /api/v1/analytics/forecast` projects weekday staffing demand against scheduled shifts and approved leave).
- [x] Automated scheduling and demand forecasting (`GET /api/v1/rosters/{id}/auto-schedule` returns an advisory plan proposing eligible, non-conflicting, non-leave staff to cover understaffed shift slots; demand forecasting feeds target headcount).
- [ ] External HR, payroll, mobile, geolocation, or biometric integrations as approved.

## Prioritization Principles

1. Protect attendance correctness and authorization before adding convenience features.
2. Deliver auditable workflows before automation and analytics.
3. Measure latency, error rate, adoption, and operational outcomes before scaling infrastructure.
4. Treat integrations as versioned contracts with explicit ownership and failure handling.

## Exit Criteria

A phase exits when its acceptance criteria are met, security and operational evidence is reviewed, migrations are repeatable, rollback is documented, and product/business owners accept known risks.
