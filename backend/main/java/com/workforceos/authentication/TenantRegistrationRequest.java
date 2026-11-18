package com.workforceos.authentication;

public record TenantRegistrationRequest(
        String organizationName,
        String timezone,
        String adminUsername,
        String adminPassword) {
}
