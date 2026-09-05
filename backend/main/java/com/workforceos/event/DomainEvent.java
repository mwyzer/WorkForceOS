package com.workforceos.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        OffsetDateTime occurredAt,
        UUID organizationId,
        String aggregateType,
        UUID aggregateId,
        String actorId,
        String correlationId,
        String causationId,
        String payload) {
}