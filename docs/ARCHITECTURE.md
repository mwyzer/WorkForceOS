# WorkforceOS Architecture

**Status:** Draft | **Decision horizon:** MVP

## 1. Architectural Direction

WorkforceOS will begin as a modular monolith built with Java 21, Spring Boot 3, PostgreSQL, and Spring Security with JWT. Modules share one deployable runtime and database while keeping domain ownership, application services, and persistence boundaries explicit.

This balances delivery speed and transactional consistency with a path to later extract high-volume or independently scaled capabilities.

## 2. Logical Modules

- **Identity**: authentication, users, roles, token issuance.
- **Workforce**: employees, departments, teams, positions.
- **Scheduling**: shifts, rosters, assignments, conflict detection.
- **Attendance**: event ingestion and attendance calculation.
- **Requests**: leave, overtime, shift swap, and correction requests.
- **Approval**: reusable approval policies, actions, and history.
- **Handover**: handover reports, items, and acknowledgement.
- **Notification**: notification intents and delivery adapters.
- **Audit**: immutable business and security audit records.
- **Reporting**: read-optimized operational queries.

## 3. Request Flow

```text
HTTP request -> authentication -> authorization -> controller
             -> application service -> domain rules -> transaction
             -> repository -> PostgreSQL
             -> domain event / audit record -> response
```

Controllers remain thin. Application services coordinate transactions and module boundaries. Domain rules reject invalid transitions before persistence. Repositories do not expose persistence concerns to API clients.

## 4. Data and Integration Boundaries

PostgreSQL is the source of truth for transactional data. Domain events should be recorded reliably with the transaction, using an outbox approach before external delivery is enabled. Kafka is a P1 integration option for asynchronous consumers such as notifications, audit projection, and analytics. Redis is a P1 option for caching, rate limiting, and distributed locking.

External integrations are adapters behind module-owned ports. No external provider should be called directly from a controller or domain object.

## 5. Consistency Rules

- Roster assignment conflict checks and attendance writes require database transactions.
- Critical commands accept an idempotency key and persist the result for safe retries.
- Audit records are append-only from application code.
- Cross-module reads use explicit query services or reporting projections.
- Module code must not bypass another module's application boundary to mutate its tables.

## 6. Deployment Shape

The MVP is one stateless API service, one PostgreSQL database, and managed secrets/configuration. Health endpoints support orchestration. Additional workers may be introduced for outbox/event delivery and notifications without changing the public API.

## 7. Architectural Risks

The main risks are shared-database coupling, event delivery failure, unclear tenant/organization boundaries, and attendance concurrency. These are addressed through module ownership, transactional outbox processing, an explicit organization identifier, unique constraints, locking where needed, and concurrency tests.

## 8. Open Decisions

- Tenant isolation strategy and organization administration model.
- Exact JWT issuer, key rotation, and token lifetimes.
- Migration tool and repository framework conventions.
- Kafka/Redis hosting and adoption milestone.
- Report materialization strategy after baseline query measurements.
