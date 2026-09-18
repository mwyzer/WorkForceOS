# WorkforceOS Deployment

**Status:** Draft runbook — local Docker Compose stack is implemented

## 1. Target Shape

The project ships a Docker Compose stack that runs the Spring Boot API behind an nginx-backed Angular frontend, backed by PostgreSQL, Redis, and Kafka:

```text
Client -> frontend (nginx :4200, proxies /api) -> backend API (:8080) -> PostgreSQL (:5432)
                                                        |-> Redis (:6379, caching)
                                                        |-> Kafka (:9092, domain event relay)
```

```text
Client -> TLS / load balancer -> WorkforceOS API -> PostgreSQL
                                      |-> Redis
                                      |-> Kafka (domain event relay)
```

Redis provides read-through caching (report and lookup results, 60-second TTL). The transactional PostgreSQL outbox lives inside the API process; `KafkaRelayer` publishes domain events to Kafka when `workforce.events.kafka.enabled=true` (the Compose stack enables it).

## 2. Environments

- **Local**: `docker compose up --build` or `mvn spring-boot:run` with local PostgreSQL/Redis, or H2 in the `test` profile.
- **CI**: automated tests against an ephemeral database (H2 via the `test` profile today; PostgreSQL is the target); Flyway migrations are validated on startup.
- **Staging**: production-like configuration, security and load validation.
- **Production**: managed dependencies, monitored rollout, backup and recovery controls.

Environment-specific secrets must never be committed. Exact hosting provider, container image, orchestration platform, and registry are TBD.

## 3. Release Process

1. Build an immutable versioned artifact.
2. Run unit, integration, contract, security, and migration checks.
3. Deploy to staging and run smoke, acceptance, and performance checks.
4. Back up or verify database recovery before schema-affecting releases.
5. Apply backward-compatible migrations before application rollout (Flyway runs on startup).
6. Roll out gradually with health checks and observability enabled.
7. Verify core workflows and monitor error/latency signals.
8. Record release evidence and decision.

## 4. Configuration

Configuration is environment-injectable with safe development defaults. The Docker Compose stack wires these values:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `workforceos` | PostgreSQL connection |
| `DB_USERNAME` / `DB_PASSWORD` | `workforceos` / `workforceos` | PostgreSQL credentials |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis cache endpoint |
| `WORKFORCE_AUTH_SIGNING_KEY` | development fallback | HMAC secret used to sign access tokens |
| `WORKFORCE_AUTH_ADMIN_USERNAME` / `WORKFORCE_AUTH_ADMIN_PASSWORD` | `admin` / `admin` | Bootstrapped admin account |
| `WORKFORCE_OUTBOX_FAILED_THRESHOLD` | `100` | Outbox health indicator threshold |
| `WORKFORCE_EVENTS_KAFKA_ENABLED` | `false` (`true` in Compose) | Enables the Kafka event relay |
| `WORKFORCE_EVENTS_KAFKA_BOOTSTRAP` | `localhost:9092` (`kafka:9092` in Compose) | Kafka bootstrap servers |
| `WORKFORCE_EVENTS_KAFKA_TOPIC` | `workforce.domain.events` | Domain event topic |
| `WORKFORCE_EVENTS_KAFKA_POLL_MS` / `WORKFORCE_EVENTS_KAFKA_SEND_TIMEOUT_MS` | `2000` / `5000` | Relay poll interval and send timeout |
| `WORKFORCE_NOTIFICATIONS_DELIVERY_POLL_MS` | `5000` | Notification delivery retry poll interval |
| `WORKFORCE_API_RATE_LIMIT_ENABLED` | `false` (`true` in Compose) | Enables token-bucket API rate limiting (HTTP 429) |
| `WORKFORCE_API_RATE_LIMIT_PER_MINUTE` | `120` | Rate limit capacity per client IP per minute |
| `WORKFORCE_ASSISTANT_AI_*` | falls back to `WORKFORCE_RISK_AI_*` | Optional OpenAI-compatible LLM for the workforce assistant (provider, base URL, model, API key, timeout) |
| `WORKFORCE_ANALYTICS_ABSENTEEISM_WINDOW_DAYS` | `30` | Default lookback window for absenteeism insights |
| `WORKFORCE_ANALYTICS_FORECAST_HORIZON_DAYS` / `WORKFORCE_ANALYTICS_FORECAST_LOOKBACK_WEEKS` | `7` / `4` | Demand forecast horizon and history window |
| `WORKFORCE_AUTO_SCHEDULE_TARGET_HEADCOUNT` | `1` | Staffing target auto-scheduling fills per shift slot |
| `WORKFORCE_RISK_*` | see [FSD §6](FSD.md) | Risk pipeline settings, including optional LLM (`WORKFORCE_RISK_AI_*`) |

Required configuration includes database URL and credentials, token signing configuration, allowed origins, logging/telemetry destination, and environment name. Secrets use a managed secret store where available.

## 5. Database and Recovery

Flyway applies `db/migration/V1__init.sql` through `V6__notifications.sql` on startup; Hibernate runs with `ddl-auto: validate`, so schema drift fails the deployment. Configure automated backups, tested restore procedures, retention, and recovery objectives before production. Never use destructive automatic schema changes in production.

## 6. Rollback

Prefer rolling back the application to the previous compatible artifact. Database migrations must be backward-compatible or have a tested rollback/forward-fix procedure. Disable problematic event consumers or feature flags when possible. Preserve logs and outbox records during recovery.

## 7. Operational Checks

Before declaring success, verify liveness/readiness, authentication, employee read, roster read, attendance command, audit creation, database connectivity, event backlog if enabled, and alert routing. Consult [OBSERVABILITY.md](OBSERVABILITY.md) and [SECURITY.md](SECURITY.md) for release gates.