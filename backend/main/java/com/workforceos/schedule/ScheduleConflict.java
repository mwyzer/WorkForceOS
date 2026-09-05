package com.workforceos.schedule;

import java.util.UUID;

public record ScheduleConflict(
        UUID firstAssignmentId,
        UUID secondAssignmentId,
        UUID rosterId,
        UUID employeeId,
        String reason) {
}