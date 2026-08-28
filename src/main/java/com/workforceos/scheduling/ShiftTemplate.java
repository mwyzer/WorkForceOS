package com.workforceos.scheduling;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ShiftTemplate(
        UUID id,
        UUID organizationId,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean overnight,
        List<BreakPeriod> breaks,
        boolean active) {
}
