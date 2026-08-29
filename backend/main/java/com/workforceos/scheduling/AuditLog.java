package com.workforceos.scheduling;

import java.time.Instant;
import java.util.UUID;

public record AuditLog(
        UUID id,
        String actor,
        String action,
        String resource,
        UUID resourceId,
        String details,
        Instant timestamp) {
}
