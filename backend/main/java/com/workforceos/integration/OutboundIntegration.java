package com.workforceos.integration;

/**
 * Vendor-neutral outbound integration port. Implementations are registered as Spring beans and are
 * discovered by {@link IntegrationRegistry}; each declares whether it is currently enabled so that
 * missing provider credentials degrade to a no-op instead of failing requests.
 */
public interface OutboundIntegration {

    String name();

    IntegrationType type();

    boolean enabled();

    IntegrationDelivery deliver(IntegrationEvent event);
}