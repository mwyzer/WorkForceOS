package com.workforceos.risk;

import java.time.Instant;
import java.util.UUID;

public record RiskAlert(
        UUID id,
        UUID assessmentId,
        RiskSeverity severity,
        RiskAlertStatus status,
        String summary,
        Instant createdAt,
        Instant resolvedAt) {
}