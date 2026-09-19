package com.workforceos.approval;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApprovalAction(
        UUID organizationId,
        UUID requestId,
        String requestType,
        ApprovalDecision decision,
        String actorId,
        OffsetDateTime decidedAt,
        String reason) {
}