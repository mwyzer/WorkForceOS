package com.workforceos.integration;

import java.time.Instant;
import java.util.Map;

public record IntegrationEvent(
        IntegrationType type,
        String externalId,
        Map<String, Object> payload,
        Instant occurredAt) {
}