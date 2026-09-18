package com.workforceos.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AutoScheduleProposal(
        UUID shiftTemplateId,
        String shiftName,
        UUID employeeId,
        String employeeNumber,
        String firstName,
        String lastName,
        OffsetDateTime start,
        OffsetDateTime end,
        String rationale) {
}