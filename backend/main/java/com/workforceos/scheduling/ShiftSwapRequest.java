package com.workforceos.scheduling;

import java.time.LocalDate;
import java.util.UUID;

public record ShiftSwapRequest(
        UUID id,
        UUID organizationId,
        UUID requestingEmployeeId,
        UUID targetEmployeeId,
        LocalDate offeredDate,
        LocalDate requestedDate,
        String reason,
        ShiftSwapRequestStatus status) {
}
