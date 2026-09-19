# WorkforceOS Event Design

**Status:** Implemented transactional PostgreSQL outbox with in-process handlers and optional Kafka delivery (`workforce.events.kafka.enabled`)

## 1. Purpose

Domain events communicate completed business facts to notifications, audit projections, analytics, and future integrations without coupling those consumers to request handling.

## 2. Event Lifecycle

```text
Command -> domain operation -> EventPublisher -> outbox row (PostgreSQL, same transaction)
        -> handlers (synchronous) -> PENDING/DELIVERED/FAILED
        -> OutboxProcessor retry poll (5 s, bounded backoff)
        -> KafkaRelayer poll -> Kafka topic (at-least-once, idempotent producer)
```

An event is recorded in the outbox in the same transaction as the triggering operation and handed to registered handlers as part of the caller's operation. Delivery is at-least-once; consumers must be idempotent. Production persistence is the `outbox_events` PostgreSQL table (`V3`, extended by `V5`); the `test` profile keeps an in-memory `ConcurrentHashMap` store for isolation. After local handler delivery, the `KafkaRelayer` publishes entries that have not yet been relayed (tracked by `kafka_published_at`) to the configured Kafka topic, with relay-level retry and backoff.

## 3. Event Catalogue (implemented)

Defined in `EventTypes` and published by domain services:

- `RosterPublished` — roster publication
- `AttendanceClockedIn`
- `AttendanceClockedOut`
- `LeaveApproved`
- `OvertimeApproved`
- `OvertimeRejected`
- `HandoverSubmitted`
- `HandoverAcknowledged`
- `EmployeeDeactivated` — triggers risk recompute
- `CoverageRiskDetected` — risk assessment persisted
- `WorkforceRiskElevated` — HIGH-severity risk assessment

`LeaveRejected` is defined but has no publishing path yet (leave has no reject endpoint). Consumers include the notification projector (`LeaveApproved`, `OvertimeApproved`, `OvertimeRejected`, `HandoverSubmitted`, `HandoverAcknowledged`, `RosterPublished`), the risk analysis scheduler (`RosterPublished`, `LeaveApproved`, `OvertimeApproved`, `EmployeeDeactivated`), the risk alert service (`CoverageRiskDetected`, `WorkforceRiskElevated`), and a demo consumer that captures events for inspection.

Event-driven consumers resolve the tenant from the event's `organizationId` so their writes land in the correct organization even when the outbox is replayed outside a request context (`RiskAnalysisScheduler` and `NotificationProjector`). Global background processes that must stay unscoped (`OutboxProcessor`, `KafkaRelayer`, `NotificationDeliveryProcessor`) are explicitly global by design — see [DATABASE-DESIGN.md §4](DATABASE-DESIGN.md).

## 4. Envelope

The `DomainEvent` record matches the designed envelope:

```json
{
  "eventId": "uuid",
  "eventType": "AttendanceClockedIn",
  "eventVersion": 1,
  "occurredAt": "2026-08-28T08:00:00Z",
  "organizationId": "uuid",
  "aggregateType": "AttendanceSession",
  "aggregateId": "uuid",
  "actorId": "uuid",
  "correlationId": "request-id",
  "payload": {}
}
```

Do not place passwords, access tokens, or unnecessary personal data in event payloads.

## 5. Reliability

Outbox entries carry status (`PENDING`, `DELIVERED`, `FAILED`), attempt count, next-attempt time, last error, and `kafka_published_at`. `EventPublisher` delivers events to registered handlers synchronously on publish, deduplicates by `eventId:handlerClass`, and retries failed handlers with bounded exponential backoff (1, 2, 4, 8, 16, 32, capped at 60 s). `OutboxProcessor` polls every 5 s for due entries. `GET /api/v1/events/outbox` exposes the outbox snapshot and `GET /api/v1/events/received` exposes captured events for operational inspection.

Kafka delivery runs in `KafkaRelayer`, enabled by `workforce.events.kafka.enabled=true` (default false, so broker-less environments are unaffected). It publishes each unrelayed entry to `workforce.events.kafka.topic` (default `workforce.domain.events`) with the `eventId` as the message key, an ACKS=all idempotent producer, and a bounded send timeout; `kafka_published_at` marks success and failures are retried with the same bounded backoff. The value is a JSON envelope (event metadata plus the original `payload`) and routing metadata is duplicated in Kafka headers. Poison-message and dead-letter handling for the broker path are TBD.

## 6. Ordering and Versioning

Ordering is guaranteed only where required, typically per aggregate. Events are immutable. Additive payload changes are preferred; incompatible changes require a new event version and migration period. Consumers must tolerate unknown fields.

## 7. Observability

Every event carries correlation identifiers. The outbox exposes Micrometer gauges (`workforceos.outbox.entries` by status and `workforceos.events.handled`), a health indicator that reports `DOWN` when the failed count reaches `workforce.observability.outbox-failed-threshold` (default 100), and structured logs with `requestId`/`traceId`. Measure publish latency, consumer lag, retry count, dead-letter count, and handler duration once a broker is adopted; alert on sustained backlog, failed delivery, and dead-letter growth.

## 8. Open Decisions

Broker topic naming (default `workforce.domain.events`), schema registry, retention, partition keys, and serialization format for downstream consumers remain TBD. Dead-letter handling for the Kafka relay path is not yet implemented.