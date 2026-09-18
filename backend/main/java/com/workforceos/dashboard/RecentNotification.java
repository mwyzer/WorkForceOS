package com.workforceos.dashboard;

import java.time.Instant;
import java.util.UUID;

import com.workforceos.scheduling.NotificationStatus;
import com.workforceos.scheduling.NotificationType;

public record RecentNotification(
        UUID id,
        UUID recipientId,
        NotificationType type,
        String title,
        NotificationStatus status,
        Instant createdAt) {
}