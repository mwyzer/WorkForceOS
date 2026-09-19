package com.workforceos.organization;

public record OrganizationUpdateRequest(
        String name,
        String timezone,
        Boolean active) {
}