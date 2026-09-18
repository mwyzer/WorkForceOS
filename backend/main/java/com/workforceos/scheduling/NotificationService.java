package com.workforceos.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class NotificationService {

    private final NotificationStore notificationStore;
    private final NotificationDeliveryService deliveryService;

    public NotificationService(NotificationStore notificationStore, NotificationDeliveryService deliveryService) {
        this.notificationStore = notificationStore;
        this.deliveryService = deliveryService;
    }

    public List<Notification> findAll() {
        return notificationStore.findAll();
    }

    public List<Notification> findByRecipientId(UUID recipientId) {
        return notificationStore.findByRecipientId(recipientId);
    }

    @Transactional
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
                Instant.now(),
                0,
                Instant.now(),
                null);

        Notification saved = notificationStore.save(notification);
        return deliveryService.deliver(saved);
    }

    public Notification findById(UUID id) {
        return notificationStore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    @Transactional
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
                notification.createdAt(),
                notification.attemptCount(),
                notification.nextAttemptAt(),
                notification.lastError());
        return notificationStore.save(read);
    }

    private void validateRequest(NotificationRequest request) {
        if (request == null
                || request.recipientId() == null
                || request.type() == null
                || ValidationUtils.isBlank(request.title())
                || ValidationUtils.isBlank(request.body())
                || request.channel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recipient, type, title, body, and channel are required");
        }
    }
}