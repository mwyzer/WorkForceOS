package com.workforceos.scheduling;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryNotificationStore implements NotificationStore {

    private final ConcurrentMap<UUID, Notification> notifications = new ConcurrentHashMap<>();

    @Override
    public List<Notification> findAll() {
        return notifications.values().stream()
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }

    @Override
    public List<Notification> findByRecipientId(UUID recipientId) {
        return notifications.values().stream()
                .filter(notification -> notification.recipientId().equals(recipientId))
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(notifications.get(id));
    }

    @Override
    public Notification save(Notification notification) {
        notifications.put(notification.id(), notification);
        return notification;
    }

    @Override
    public List<Notification> findDue(Instant now, int limit) {
        return notifications.values().stream()
                .filter(n -> n.status() == NotificationStatus.PENDING || n.status() == NotificationStatus.FAILED)
                .filter(n -> !n.nextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(Notification::createdAt))
                .limit(limit)
                .toList();
    }
}