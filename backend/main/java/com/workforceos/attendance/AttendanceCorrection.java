package com.workforceos.attendance;

import java.util.UUID;

public record AttendanceCorrection(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        UUID attendanceId,
        AttendanceCorrectionType type,
        String details,
        AttendanceCorrectionStatus status) {
}
