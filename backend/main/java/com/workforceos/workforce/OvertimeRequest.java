package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.UUID;

public record OvertimeRequest(
        UUID id,
        UUID employeeId,
        LocalDate date,
        Double hours,
        String reason,
        OvertimeRequestStatus status) {
}
