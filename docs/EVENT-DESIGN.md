# WorkforceOS Event Design

**Status:** Draft | **Delivery:** Transactional outbox first; Kafka is P1

## 1. Purpose

Domain events communicate completed business facts to notifications, audit projections, analytics, and future integrations without coupling those consumers to request handling.

## 2. Event Lifecycle

```text
Command -> domain transaction -> business data + outbox row
        -> publisher -> broker/consumer -> acknowledgement/retry
```

An event is published only after the transaction that produced it commits. Consumers must be idempotent because delivery is at-least-once.

## 3. Initial Event Catalogue

- `RosterPublished`
- `AttendanceClockedIn`
- `AttendanceClockedOut`
- `LeaveApproved`
- `LeaveRejected`
- `OvertimeApproved`
- `HandoverSubmitted`
- `HandoverAcknowledged`

Break events, request submission, shift swaps, corrections, and notification events may be added as consumers require them.

## 4. Envelope

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
  "causationId": "command-id",
  "payload": {}
}
```

Do not place passwords, access tokens, or unnecessary personal data in event payloads.

## 5. Reliability

Outbox rows need status, attempt count, next-attempt time, and last error. Publishers retry transient failures with bounded backoff. Poison messages move to a dead-letter path after the configured threshold. Consumers record processed event IDs or use an equivalent idempotency mechanism.

## 6. Ordering and Versioning

Ordering is guaranteed only where required, typically per aggregate. Events are immutable. Additive payload changes are preferred; incompatible changes require a new event version and migration period. Consumers must tolerate unknown fields.

## 7. Observability

Every event carries correlation and causation identifiers. Measure publish latency, consumer lag, retry count, dead-letter count, and handler duration. Alert on sustained backlog, failed delivery, and dead-letter growth.

## 8. Open Decisions

Broker topic naming, schema registry, retention, partition keys, serialization format, and exact retry thresholds are TBD before Kafka adoption.
