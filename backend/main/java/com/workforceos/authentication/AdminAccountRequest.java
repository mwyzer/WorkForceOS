package com.workforceos.authentication;

import java.util.Set;
import java.util.UUID;

public record AdminAccountRequest(
        String username,
        String password,
        UUID organizationId,
        Set<String> roles) {
}