package com.workforceos.integration;

public record IntegrationStatus(
        String name,
        IntegrationType type,
        boolean enabled) {
}