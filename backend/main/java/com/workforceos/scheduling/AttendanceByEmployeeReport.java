package com.workforceos.scheduling;

import java.util.UUID;

public record AttendanceByEmployeeReport(
        UUID employeeId,
        String employeeNumber,
        String firstName,
        String lastName,
        long sessionCount,
        long totalMinutes,
        long lateCount,
        long earlyLeaveCount) {
}