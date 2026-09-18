package com.workforceos.organization;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the resolved tenant (organization id) for the current request. Set by
 * {@link TenantContextFilter} and cleared after the request completes so tenant state
 * never leaks across requests.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID organizationId) {
        CURRENT.set(organizationId);
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        return current().orElse(OrganizationService.DEFAULT_ORGANIZATION_ID);
    }

    public static void clear() {
        CURRENT.remove();
    }
}