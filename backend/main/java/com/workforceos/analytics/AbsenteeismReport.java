package com.workforceos.analytics;

import java.time.Instant;
import java.util.List;

public record AbsenteeismReport(
        Instant generatedAt,
        int windowDays,
        long highRisk,
        long mediumRisk,
        long lowRisk,
        double overallAbsenceRate,
        List<AbsenteeismInsight> insights) {
}