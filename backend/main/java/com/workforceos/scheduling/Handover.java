package com.workforceos.scheduling;

import java.util.List;
import java.util.UUID;

public record Handover(
        UUID id,
        UUID employeeId,
        List<HandoverItem> items,
        HandoverStatus status) {
}
