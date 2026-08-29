package com.workforceos.scheduling;

import java.time.LocalDate;
import java.util.UUID;

public record ShiftSwapRequestRequest(
        UUID requestingEmployeeId,
        UUID targetEmployeeId,
        LocalDate offeredDate,
        LocalDate requestedDate,
        String reason) {
}
