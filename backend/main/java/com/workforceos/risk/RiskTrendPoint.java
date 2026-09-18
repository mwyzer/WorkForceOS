package com.workforceos.risk;

import java.time.LocalDate;

public record RiskTrendPoint(
        LocalDate date,
        long newAssessments,
        long high,
        long medium,
        long low) {
}