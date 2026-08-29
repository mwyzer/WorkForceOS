package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.UUID;

public record OvertimeRequestRequest(
        UUID employeeId,
        LocalDate date,
        Double hours,
        String reason) {
}
