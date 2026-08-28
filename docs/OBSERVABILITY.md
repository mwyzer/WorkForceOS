# WorkforceOS Observability

**Status:** Draft operational standard

## 1. Goals

Provide enough signal to detect availability and correctness issues, explain an individual request, diagnose slow reports, and monitor background event delivery without exposing sensitive data.

## 2. Logs

Use structured logs with timestamp, level, service, environment, organization scope where safe, request ID, trace ID, actor ID where policy permits, route, status, duration, and error code. Log business outcomes and dependency failures, not secrets or full personal payloads.

Recommended levels:

- `INFO`: lifecycle events and important state changes.
- `WARN`: rejected business operations, retries, degraded dependencies, and approaching limits.
- `ERROR`: failed requests, transaction failures, and exhausted retries.
- `DEBUG`: local diagnostics only and disabled by default in production.

## 3. Metrics

Track request count, error count, latency percentiles, active requests, authentication failures, authorization denials, attendance conflicts, roster conflicts, request decisions, audit write failures, database pool saturation, and report duration.

For events, track outbox backlog, publish latency, retry count, consumer lag, handler failures, and dead-letter count. For infrastructure, track CPU, memory, restarts, database connections, storage, and dependency health.

## 4. Traces

Propagate W3C trace context across HTTP and event boundaries. Include spans for authentication, authorization, application service, database, external dependency, outbox publish, and consumer handling. Correlate traces with API error responses through `traceId`.

## 5. Health and Alerts

Expose separate liveness and readiness checks. Readiness should fail when required dependencies prevent correct operation. Alert on elevated 5xx responses, latency target breaches, failed health checks, authentication attack patterns, database saturation, attendance error spikes, outbox backlog, and dead-letter growth.

Every alert needs an owner, severity, runbook, and escalation path. Exact thresholds require baseline measurements.

## 6. Dashboards and SLOs

Provide service health, API performance, attendance correctness, approval workflow, database, and event-processing dashboards. Start with the PRD targets of 99.9% availability, p95 reads below 500 ms, and p95 writes below 1 second; revise after load testing.

## 7. Data Handling

Apply log redaction and retention policies. Never log passwords, bearer tokens, secrets, or unnecessary personal data. Restrict observability data access by role and audit access to sensitive operational telemetry.
