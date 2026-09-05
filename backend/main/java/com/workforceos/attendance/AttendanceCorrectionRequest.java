package com.workforceos.attendance;

import java.util.UUID;

public record AttendanceCorrectionRequest(
        UUID employeeId,
        UUID attendanceId,
        AttendanceCorrectionType type,
        String details) {
}
