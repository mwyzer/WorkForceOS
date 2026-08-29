package com.workforceos.scheduling;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID recipientId,
        NotificationType type,
        String title,
        String body,
        NotificationChannel channel,
        NotificationStatus status,
        Instant createdAt) {
}
