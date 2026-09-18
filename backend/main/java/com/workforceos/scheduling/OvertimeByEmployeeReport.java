package com.workforceos.scheduling;

import java.util.UUID;

public record OvertimeByEmployeeReport(
        UUID employeeId,
        String employeeNumber,
        String firstName,
        String lastName,
        long approvedCount,
        long pendingCount,
        long rejectedCount,
        Double totalApprovedHours) {
}