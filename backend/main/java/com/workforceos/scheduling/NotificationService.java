package com.workforceos.scheduling;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final ConcurrentMap<UUID, Notification> notifications = new ConcurrentHashMap<>();

    public List<Notification> findAll() {
        return notifications.values().stream()
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }

    public List<Notification> findByRecipientId(UUID recipientId) {
        return notifications.values().stream()
                .filter(n -> n.recipientId().equals(recipientId))
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }

    public Notification create(NotificationRequest request) {
        validateRequest(request);

        Notification notification = new Notification(
                UUID.randomUUID(),
                request.recipientId(),
                request.type(),
                request.title().trim(),
                request.body().trim(),
                request.channel(),
                NotificationStatus.PENDING,
                Instant.now());

        notifications.put(notification.id(), notification);
        return notification;
    }

    public Notification findById(UUID id) {
        Notification notification = notifications.get(id);
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        return notification;
    }

    public Notification markAsRead(UUID id) {
        Notification notification = findById(id);
        Notification read = new Notification(
                notification.id(),
                notification.recipientId(),
                notification.type(),
                notification.title(),
                notification.body(),
                notification.channel(),
                NotificationStatus.READ,
                notification.createdAt());
        notifications.put(id, read);
        return read;
    }

    private void validateRequest(NotificationRequest request) {
        if (request == null
                || request.recipientId() == null
                || request.type() == null
                || isBlank(request.title())
                || isBlank(request.body())
                || request.channel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recipient, type, title, body, and channel are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
