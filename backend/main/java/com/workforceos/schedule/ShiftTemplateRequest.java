package com.workforceos.schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ShiftTemplateRequest(
        UUID organizationId,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        List<BreakPeriod> breaks) {
}
