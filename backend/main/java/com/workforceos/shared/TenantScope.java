package com.workforceos.shared;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.organization.TenantContext;

/**
 * Central enforcement helper for tenant isolation. Every request-scoped read and write that
 * owns an organization must pass through here so the current tenant (from
 * {@link TenantContext}) is the only authority, never client-supplied identifiers.
 */
public final class TenantScope {

    private TenantScope() {
    }

    public static UUID require() {
        return TenantContext.require();
    }

    /**
     * Forces writes to the current tenant, ignoring any organization claimed by the caller.
     */
    public static UUID force(UUID claimedOrganizationId) {
        return TenantContext.require();
    }

    /**
     * Rejects access to a resource owned by a different organization, treating it as
     * not-found so tenants cannot probe for each other's data.
     */
    public static void assertAccess(UUID ownerOrganizationId) {
        UUID current = TenantContext.require();
        if (ownerOrganizationId == null || !ownerOrganizationId.equals(current)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }
}