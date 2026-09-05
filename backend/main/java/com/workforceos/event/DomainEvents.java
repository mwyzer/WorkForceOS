package com.workforceos.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class DomainEvents {

    private DomainEvents() {
    }

    public static DomainEvent of(String eventType, UUID organizationId, String aggregateType, UUID aggregateId,
            String actorId, String payload) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                1,
                OffsetDateTime.now(),
                organizationId,
                aggregateType,
                aggregateId,
                actorId,
                UUID.randomUUID().toString(),
                null,
                payload);
    }
}