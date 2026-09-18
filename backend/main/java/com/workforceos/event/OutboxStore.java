package com.workforceos.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxStore {

    OutboxEntry save(OutboxEntry entry);

    Optional<OutboxEntry> find(UUID eventId);

    List<OutboxEntry> findAll();

    long countByStatus(OutboxStatus status);

    List<OutboxEntry> findDue(OffsetDateTime now, int limit);

    List<OutboxEntry> findUnpublished(OffsetDateTime now, int limit);
}