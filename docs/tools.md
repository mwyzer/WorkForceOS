# WorkforceOS — Tools & Technology Stack

**Purpose:** a single-page inventory of the languages, frameworks, libraries, and infrastructure tools used to build, run, and operate WorkforceOS, with what each one is used for and where its configuration lives. For product scope and architecture, see [spec.md](spec.md); this file is the "what's actually in the toolbox" reference.

---

## 1. Languages & Runtimes

| Tool | Version | Used for |
| --- | --- | --- |
| Java | 25 | Backend language ([pom.xml](../pom.xml)) |
| TypeScript | ~5.1 | Frontend language ([frontend/package.json](../frontend/package.json)) |
| Node.js | current LTS | Frontend build/dev tooling |

## 2. Backend — Framework & Core Libraries

Managed via Maven ([pom.xml](../pom.xml)), parented on `spring-boot-starter-parent:3.5.12`.

| Tool | Used for |
| --- | --- |
| Spring Boot (Web starter) | REST API, embedded Tomcat, application bootstrap |
| Spring Security | Authentication filter chain, bearer-token verification, RBAC |
| Spring Data JPA / Hibernate | Entity mapping and persistence against PostgreSQL, `ddl-auto: validate` |
| Spring Data Redis | Read-through caching client |
| Spring Cache | Cache abstraction (`@Cacheable`) over the Redis-backed cache manager |
| Spring Kafka | `KafkaRelayer` producer for the optional domain-event relay |
| Spring Boot Actuator | Health, info, and metrics endpoints; liveness/readiness probes |
| Flyway (`flyway-core`, `flyway-database-postgresql`) | Versioned schema migrations, `V1__init.sql` … `V6__notifications.sql` |
| PostgreSQL JDBC driver | Runtime database connectivity |
| H2 | In-memory database for the `test` Spring profile (PostgreSQL-compatibility mode) |
| JUnit 5 / Spring Boot Test (`spring-boot-starter-test`) | Unit and integration testing |

No ORM code generation, mapping, or logging-facade libraries (e.g. Lombok, MapStruct) are currently pulled in — entities and DTOs are hand-written.

## 3. Frontend — Framework & Libraries

Managed via npm ([frontend/package.json](../frontend/package.json)).

| Tool | Version | Used for |
| --- | --- | --- |
| Angular (`core`, `common`, `router`, `forms`, `animations`, `platform-browser`) | 16.2 | SPA framework, routing, guarded pages, forms |
| RxJS | 7.8 | Reactive state and HTTP stream handling |
| Angular CLI / `@angular-devkit/build-angular` | 16.2 | Build, dev server (`ng serve`), watch builds |
| Zone.js | 0.13 | Angular change-detection runtime |
| Karma + Jasmine | 6.4 / 4.6/4.3 | Unit testing (`ng test`) |
| Playwright (`@playwright/test`) | 1.63 | End-to-end browser testing (`frontend/e2e`: login, home, employees, risk, journey smoke) |
| TypeScript | 5.1 | Language/compiler |

Run: `npm install && npm start` (dev server on `:4200`); `npm run e2e` / `npm run e2e:headed` for Playwright.

## 4. Data Stores & Messaging

| Tool | Role | Config |
| --- | --- | --- |
| PostgreSQL 16 | System of record for the operational domain, risk data, outbox, notifications | `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` |
| Redis 7 | Read-through cache for reports/lookups (60s TTL) | `REDIS_HOST`/`REDIS_PORT` |
| Apache Kafka 3.9 | Optional relay target for domain events to external consumers | `WORKFORCE_EVENTS_KAFKA_*` (disabled by default; enabled in Docker Compose) |

Transactional outbox pattern is implemented in-process (no external library) via `EventPublisher`/`KafkaRelayer` — see [EVENT-DESIGN.md](EVENT-DESIGN.md).

## 5. AI / LLM Integration

| Tool | Used for |
| --- | --- |
| `com.workforceos.ai.WorkforceLlmClient` (custom, OpenAI-compatible HTTP client) | Shared client for the risk advisor and workforce assistant |
| Provider support: `openai`, `muse` (default model `muse`), `spark` (default model `spark-1.3`) | Config-gated, swappable LLM backends; each has its own base URL/API key/timeout |
| Heuristic fallback (`HeuristicAssistant`, heuristic risk advisor) | Deterministic, offline behavior when no LLM is configured or a call fails |

No LLM SDK dependency is pulled in — calls go through the shared HTTP client, keeping the integration provider-agnostic. Config keys: `WORKFORCE_RISK_AI_*`, `WORKFORCE_ASSISTANT_AI_*`, `WORKFORCE_*_MUSE_*`, `WORKFORCE_*_SPARK_*` — see [DEPLOYMENT.md §4](DEPLOYMENT.md).

## 6. Observability

| Tool | Used for |
| --- | --- |
| Micrometer (via Spring Boot Actuator) | Application metrics (`workforceos.outbox.entries`, `workforceos.events.handled`) |
| Spring Boot Actuator | `/actuator/health`, `/actuator/info`, `/actuator/metrics`, liveness/readiness probes |
| Custom `RequestCorrelationFilter` | Per-request `requestId` and W3C `traceId`, `X-Request-Id`/`traceparent` response headers |
| Logback (`logback-spring.xml`) | Structured logging with MDC (`requestId`, `traceId`, `actor`) |
| Custom `ApiExceptionHandler` | Uniform `ApiError` response contract |

Prometheus scraping and Grafana dashboards are the documented target ([OBSERVABILITY.md](OBSERVABILITY.md)) but no Prometheus registry dependency is wired into the backend yet — metrics currently surface only through Actuator's own endpoint.

## 7. Containerization & Local Orchestration

| Tool | Used for |
| --- | --- |
| Docker | Backend image: multi-stage `maven:3.9-eclipse-temurin-25` build → `eclipse-temurin:25-jre` runtime ([Dockerfile](../Dockerfile)) |
| Docker (frontend) | nginx-backed Angular image, proxies `/api` to the backend ([frontend/Dockerfile](../frontend/Dockerfile)) |
| Docker Compose | Local multi-service stack: `postgres`, `redis`, `kafka`, `backend`, `frontend` ([docker-compose.yml](../docker-compose.yml)) |

Run the full stack: `docker compose up --build` (backend on `:8080`, frontend on `:4200`).

## 8. Build & Dev Tooling

| Tool | Used for |
| --- | --- |
| Maven (`mvn`) | Backend build, dependency management, test execution (`mvn test`, `mvn spring-boot:run`) |
| Maven Surefire plugin | Runs backend tests under the `test` Spring profile |
| npm / Angular CLI | Frontend install, build, serve, test |
| Git / GitHub | Version control (no CI workflow committed under `.github/workflows` yet — see [DEPLOYMENT.md §2](DEPLOYMENT.md)) |

## 9. Testing Tools Summary

| Layer | Tool |
| --- | --- |
| Backend unit/integration | JUnit 5, Spring Boot Test, H2 (PostgreSQL mode) |
| Backend persistence contract | `PersistenceIntegrationTests` (Flyway schema vs. JPA entities, `ddl-auto=validate`) |
| Backend performance gate | `PerformanceSmokeTests` (p95 latency budget, 64-way concurrency burst) |
| Backend tenancy | `TenantIsolationTests`, `OrganizationAdminTests`, `OrganizationControllerTests` |
| Frontend unit | Karma + Jasmine |
| Frontend end-to-end | Playwright (Chromium) |

Full strategy and coverage targets: [TEST-PLAN.md](TEST-PLAN.md).

## 10. Related Documents

| Document | Covers |
| --- | --- |
| [spec.md](spec.md) | Product scope, architecture, API, non-functional requirements |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module boundaries and design decisions |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Environments, configuration variables, release process |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Logging, metrics, tracing, alerting standard |
| [SECURITY.md](SECURITY.md) | AuthN/authZ and hardening tooling |
| [TEST-PLAN.md](TEST-PLAN.md) | Testing strategy and coverage targets |

---

**Maintaining this file:** update it whenever a dependency is added, removed, or replaced in [pom.xml](../pom.xml) or [frontend/package.json](../frontend/package.json), or when a new piece of infrastructure is added to [docker-compose.yml](../docker-compose.yml).
