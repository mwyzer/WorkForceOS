# WorkforceOS Deployment

**Status:** Draft deployment runbook

## 1. Target Shape

The MVP runs as a stateless Spring Boot API behind a reverse proxy or load balancer, connected to PostgreSQL. Configuration and secrets are injected by the environment. Optional Redis and Kafka services are introduced when their P1 capabilities are enabled.

```text
Client -> TLS / load balancer -> WorkforceOS API -> PostgreSQL
                                      |-> Redis (optional)
                                      |-> Kafka / outbox worker (optional)
```

## 2. Environments

- **Local**: developer-controlled dependencies and safe test data.
- **CI**: automated tests, ephemeral database, migration verification.
- **Staging**: production-like configuration, security and load validation.
- **Production**: managed dependencies, monitored rollout, backup and recovery controls.

Environment-specific secrets must never be committed. Exact hosting provider, container image, orchestration platform, and registry are TBD.

## 3. Release Process

1. Build an immutable versioned artifact.
2. Run unit, integration, contract, security, and migration checks.
3. Deploy to staging and run smoke, acceptance, and performance checks.
4. Back up or verify database recovery before schema-affecting releases.
5. Apply backward-compatible migrations before application rollout.
6. Roll out gradually with health checks and observability enabled.
7. Verify core workflows and monitor error/latency signals.
8. Record release evidence and decision.

## 4. Configuration

Required configuration includes database URL and credentials, JWT issuer/signing configuration, allowed origins, logging/telemetry destination, and environment name. Optional configuration covers Redis, Kafka, notification providers, rate limits, and retry policies. Secrets use a managed secret store where available.

## 5. Database and Recovery

Use versioned migrations and fail deployment if migrations cannot be applied safely. Configure automated backups, tested restore procedures, retention, and recovery objectives before production. Never use destructive automatic schema changes in production.

## 6. Rollback

Prefer rolling back the application to the previous compatible artifact. Database migrations must be backward-compatible or have a tested rollback/forward-fix procedure. Disable problematic event consumers or feature flags when possible. Preserve logs and outbox records during recovery.

## 7. Operational Checks

Before declaring success, verify liveness/readiness, authentication, employee read, roster read, attendance command, audit creation, database connectivity, event backlog if enabled, and alert routing. Consult [OBSERVABILITY.md](OBSERVABILITY.md) and [SECURITY.md](SECURITY.md) for release gates.
