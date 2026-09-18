package com.workforceos.risk;

import java.time.Instant;
import java.util.List;

public record StructuredRisk(
        Instant generatedAt,
        long activeEmployees,
        long publishedAssignments,
        RiskImpact impact,
        List<RiskAssessment> assessments) {
}