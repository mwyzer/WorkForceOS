package com.workforceos.schedule;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RosterAssignment(
        UUID id,
        UUID rosterId,
        UUID employeeId,
        UUID shiftTemplateId,
        OffsetDateTime start,
        OffsetDateTime end,
        boolean active) {
}
