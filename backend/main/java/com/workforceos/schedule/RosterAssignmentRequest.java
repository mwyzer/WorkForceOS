package com.workforceos.schedule;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RosterAssignmentRequest(
        UUID employeeId,
        UUID shiftTemplateId,
        OffsetDateTime start,
        OffsetDateTime end) {
}
