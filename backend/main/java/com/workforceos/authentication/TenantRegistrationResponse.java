package com.workforceos.authentication;

import java.util.UUID;

public record TenantRegistrationResponse(
        UUID organizationId,
        String organizationName,
        String username,
        String accessToken,
        String tokenType) {
}
