package com.workforceos.scheduling;

import java.util.UUID;

public record AuditLogRequest(
        String actor,
        String action,
        String resource,
        UUID resourceId,
        String details) {
}
