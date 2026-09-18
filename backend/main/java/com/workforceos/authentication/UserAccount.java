package com.workforceos.authentication;

import java.util.Set;
import java.util.UUID;

public record UserAccount(String username, UUID organizationId, String passwordHash, Set<String> roles,
        boolean active) {
}
