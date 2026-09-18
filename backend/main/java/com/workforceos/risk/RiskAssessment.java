package com.workforceos.risk;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessment(
        UUID id,
        RiskType type,
        RiskSeverity severity,
        int score,
        RiskEntityType entityType,
        UUID entityId,
        UUID teamId,
        UUID departmentId,
        Instant windowStart,
        Instant windowEnd,
        long impactMinutes,
        String summary,
        List<String> evidence) {
}