package com.workforceos.scheduling;

import java.util.List;
import java.util.UUID;

public record AutoSchedulePlan(
        UUID rosterId,
        String rosterName,
        int targetHeadcount,
        int understaffedSlots,
        List<AutoScheduleProposal> proposals) {
}