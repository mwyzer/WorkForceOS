package com.workforceos.organization;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.workforceos.authentication.UserAccount;

/**
 * Maps the authenticated principal to the organization it belongs to. In the current
 * single-organization deployment every account resolves to the seeded default
 * organization; the seam exists so real multi-tenant resolution can be wired in without
 * touching callers.
 */
@Component
public class TenantResolver {

    public UUID resolve(UserAccount user) {
        if (user != null && user.organizationId() != null) {
            return user.organizationId();
        }
        return OrganizationService.DEFAULT_ORGANIZATION_ID;
    }
}