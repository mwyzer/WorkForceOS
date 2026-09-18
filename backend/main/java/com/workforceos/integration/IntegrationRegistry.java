package com.workforceos.integration;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Discovers all registered outbound integrations and routes events to every integration of a given
 * type. Disabled integrations are reported as {@code SKIPPED} so callers can see what was bypassed.
 */
@Service
public class IntegrationRegistry {

    private final List<OutboundIntegration> integrations;

    public IntegrationRegistry(List<OutboundIntegration> integrations) {
        this.integrations = List.copyOf(integrations);
    }

    public List<IntegrationStatus> status() {
        return integrations.stream()
                .map(integration -> new IntegrationStatus(integration.name(), integration.type(),
                        integration.enabled()))
                .toList();
    }

    public List<IntegrationDelivery> dispatch(IntegrationType type, IntegrationEvent event) {
        return integrations.stream()
                .filter(integration -> integration.type() == type)
                .map(integration -> integration.deliver(event))
                .toList();
    }
}