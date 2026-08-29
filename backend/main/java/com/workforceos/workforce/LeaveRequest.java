package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequest(
        UUID id,
        UUID employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveRequestStatus status) {
}
