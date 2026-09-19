package com.workforceos.attendance;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceSession(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        UUID rosterId,
        UUID shiftTemplateId,
        OffsetDateTime clockInAt,
        OffsetDateTime clockOutAt,
        boolean active) {
}
