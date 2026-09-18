package com.workforceos.event;

import java.time.OffsetDateTime;
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
@Table(name = "outbox_events")
class OutboxEntryEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "causation_id")
    private String causationId;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "kafka_published_at")
    private OffsetDateTime kafkaPublishedAt;

    protected OutboxEntryEntity() {
    }

    OutboxEntryEntity(OutboxEntry entry) {
        DomainEvent event = entry.event();
        this.eventId = entry.eventId();
        this.eventType = event.eventType();
        this.eventVersion = event.eventVersion();
        this.occurredAt = event.occurredAt();
        this.organizationId = event.organizationId();
        this.aggregateType = event.aggregateType();
        this.aggregateId = event.aggregateId();
        this.actorId = event.actorId();
        this.correlationId = event.correlationId();
        this.causationId = event.causationId();
        this.payload = event.payload();
        this.status = entry.status();
        this.attemptCount = entry.attemptCount();
        this.createdAt = entry.createdAt();
        this.nextAttemptAt = entry.nextAttemptAt();
        this.lastError = entry.lastError();
        this.kafkaPublishedAt = entry.kafkaPublishedAt();
    }

    OutboxEntry toRecord() {
        DomainEvent event = new DomainEvent(eventId, eventType, eventVersion, occurredAt, organizationId,
                aggregateType, aggregateId, actorId, correlationId, causationId, payload);
        return new OutboxEntry(eventId, event, status, attemptCount, createdAt, nextAttemptAt, lastError,
                kafkaPublishedAt);
    }
}

interface OutboxEventRepository extends JpaRepository<OutboxEntryEntity, UUID> {

    List<OutboxEntryEntity> findAllByOrderByCreatedAtAsc();

    long countByStatus(OutboxStatus status);

    List<OutboxEntryEntity> findByStatusNotAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status, OffsetDateTime now);

    List<OutboxEntryEntity> findByKafkaPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OffsetDateTime now);
}

@Component
@Profile("!test")
class JpaOutboxStore implements OutboxStore {

    private final OutboxEventRepository repository;

    JpaOutboxStore(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutboxEntry save(OutboxEntry entry) {
        repository.save(new OutboxEntryEntity(entry));
        return entry;
    }

    @Override
    public Optional<OutboxEntry> find(UUID eventId) {
        return repository.findById(eventId).map(OutboxEntryEntity::toRecord);
    }

    @Override
    public List<OutboxEntry> findAll() {
        return repository.findAllByOrderByCreatedAtAsc().stream()
                .map(OutboxEntryEntity::toRecord)
                .toList();
    }

    @Override
    public long countByStatus(OutboxStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    public List<OutboxEntry> findDue(OffsetDateTime now, int limit) {
        return repository.findByStatusNotAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus.DELIVERED, now)
                .stream()
                .limit(limit)
                .map(OutboxEntryEntity::toRecord)
                .toList();
    }

    @Override
    public List<OutboxEntry> findUnpublished(OffsetDateTime now, int limit) {
        return repository
                .findByKafkaPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now)
                .stream()
                .limit(limit)
                .map(OutboxEntryEntity::toRecord)
                .toList();
    }
}