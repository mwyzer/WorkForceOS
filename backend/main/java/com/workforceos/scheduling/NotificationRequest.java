package com.workforceos.scheduling;

import java.util.UUID;

public record NotificationRequest(
        UUID recipientId,
        NotificationType type,
        String title,
        String body,
        NotificationChannel channel) {
}
