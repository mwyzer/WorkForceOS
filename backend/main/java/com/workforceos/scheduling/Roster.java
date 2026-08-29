package com.workforceos.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Roster(
        UUID id,
        UUID organizationId,
        String name,
        RosterStatus status,
        OffsetDateTime createdAt,
        boolean active) {
}
