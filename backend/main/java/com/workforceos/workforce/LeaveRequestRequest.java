package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestRequest(
        UUID employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String reason) {
}
