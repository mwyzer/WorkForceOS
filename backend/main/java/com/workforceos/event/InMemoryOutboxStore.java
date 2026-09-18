package com.workforceos.event;

import java.time.OffsetDateTime;
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
public class InMemoryOutboxStore implements OutboxStore {

    private final ConcurrentMap<UUID, OutboxEntry> entries = new ConcurrentHashMap<>();

    @Override
    public OutboxEntry save(OutboxEntry entry) {
        entries.put(entry.eventId(), entry);
        return entry;
    }

    @Override
    public Optional<OutboxEntry> find(UUID eventId) {
        return Optional.ofNullable(entries.get(eventId));
    }

    @Override
    public List<OutboxEntry> findAll() {
        return entries.values().stream()
                .sorted(Comparator.comparing(OutboxEntry::createdAt))
                .toList();
    }

    @Override
    public long countByStatus(OutboxStatus status) {
        return entries.values().stream().filter(entry -> entry.status() == status).count();
    }

    @Override
    public List<OutboxEntry> findDue(OffsetDateTime now, int limit) {
        return entries.values().stream()
                .filter(entry -> entry.status() != OutboxStatus.DELIVERED)
                .filter(entry -> !entry.nextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(OutboxEntry::createdAt))
                .limit(limit)
                .toList();
    }

    @Override
    public List<OutboxEntry> findUnpublished(OffsetDateTime now, int limit) {
        return entries.values().stream()
                .filter(entry -> !entry.kafkaPublished())
                .filter(entry -> !entry.nextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(OutboxEntry::createdAt))
                .limit(limit)
                .toList();
    }
}