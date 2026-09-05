package com.workforceos.attendance;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceRequest(
        UUID employeeId,
        OffsetDateTime occurredAt) {
}
