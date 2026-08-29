package com.workforceos.scheduling;

public record AuditLogSummary(
        long totalLogs,
        long distinctActors,
        long distinctActions,
        long distinctResources) {
}
