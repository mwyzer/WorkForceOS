package com.workforceos.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    protected NotificationEntity() {
    }

    NotificationEntity(Notification notification) {
        this.id = notification.id();
        this.organizationId = notification.organizationId();
        this.recipientId = notification.recipientId();
        this.type = notification.type();
        this.title = notification.title();
        this.body = notification.body();
        this.channel = notification.channel();
        this.status = notification.status();
        this.createdAt = notification.createdAt();
        this.attemptCount = notification.attemptCount();
        this.nextAttemptAt = notification.nextAttemptAt();
        this.lastError = notification.lastError();
    }

    Notification toRecord() {
        return new Notification(id, organizationId, recipientId, type, title, body, channel, status, createdAt,
                attemptCount,
                nextAttemptAt, lastError);
    }
}

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findAllByOrderByCreatedAtDesc();

    List<NotificationEntity> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    List<NotificationEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List<NotificationStatus> statuses, Instant now);
}

@Component
@Profile("!test")
class JpaNotificationStore implements NotificationStore {

    private final NotificationRepository repository;

    JpaNotificationStore(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Notification> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationEntity::toRecord)
                .toList();
    }

    @Override
    public List<Notification> findByRecipientId(UUID recipientId) {
        return repository.findAllByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(NotificationEntity::toRecord);
    }

    @Override
    public Notification save(Notification notification) {
        repository.save(new NotificationEntity(notification));
        return notification;
    }

    @Override
    public List<Notification> findDue(Instant now, int limit) {
        return repository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), now)
                .stream()
                .limit(limit)
                .map(NotificationEntity::toRecord)
                .toList();
    }
}