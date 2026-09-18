package com.workforceos.analytics;

import java.util.List;
import java.util.UUID;

public record AbsenteeismInsight(
        UUID employeeId,
        String employeeNumber,
        String firstName,
        String lastName,
        long scheduledShifts,
        long absentShifts,
        long lateCount,
        long plannedLeaveDays,
        double absenceRate,
        String trend,
        String riskLevel,
        int score,
        List<String> drivers) {
}