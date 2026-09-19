package com.workforceos.scheduling;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID organizationId,
        UUID recipientId,
        NotificationType type,
        String title,
        String body,
        NotificationChannel channel,
        NotificationStatus status,
        Instant createdAt,
        int attemptCount,
        Instant nextAttemptAt,
        String lastError) {

    Notification delivered() {
        return new Notification(id, organizationId, recipientId, type, title, body, channel,
                NotificationStatus.DELIVERED, createdAt, attemptCount, nextAttemptAt, lastError);
    }

    Notification sent() {
        return new Notification(id, organizationId, recipientId, type, title, body, channel,
                NotificationStatus.SENT, createdAt, attemptCount, nextAttemptAt, lastError);
    }

    Notification failed(String error, Instant nextAttempt) {
        return new Notification(id, organizationId, recipientId, type, title, body, channel,
                NotificationStatus.FAILED, createdAt, attemptCount + 1, nextAttempt, error);
    }
}