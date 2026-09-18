package com.workforceos.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEntry(
        UUID eventId,
        DomainEvent event,
        OutboxStatus status,
        int attemptCount,
        OffsetDateTime createdAt,
        OffsetDateTime nextAttemptAt,
        String lastError,
        OffsetDateTime kafkaPublishedAt) {

    public boolean kafkaPublished() {
        return kafkaPublishedAt != null;
    }

    public OutboxEntry delivered() {
        return new OutboxEntry(eventId, event, OutboxStatus.DELIVERED, attemptCount, createdAt, nextAttemptAt,
                lastError, kafkaPublishedAt);
    }

    public OutboxEntry failed(String error, OffsetDateTime nextAttemptAt) {
        return new OutboxEntry(eventId, event, OutboxStatus.FAILED, attemptCount + 1, createdAt, nextAttemptAt, error,
                kafkaPublishedAt);
    }

    public OutboxEntry retried() {
        return new OutboxEntry(eventId, event, OutboxStatus.PENDING, attemptCount, createdAt, nextAttemptAt,
                lastError, kafkaPublishedAt);
    }

    public OutboxEntry kafkaPublished(OffsetDateTime publishedAt) {
        return new OutboxEntry(eventId, event, status, attemptCount, createdAt, nextAttemptAt, lastError, publishedAt);
    }

    public OutboxEntry relayFailed(String error, OffsetDateTime nextAttemptAt) {
        return new OutboxEntry(eventId, event, status, attemptCount + 1, createdAt, nextAttemptAt, error,
                kafkaPublishedAt);
    }
}