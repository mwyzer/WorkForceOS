package com.workforceos.authentication;

import java.util.Set;
import java.util.UUID;

public record AdminAccountResponse(
        String username,
        UUID organizationId,
        Set<String> roles) {

    static AdminAccountResponse from(UserAccount userAccount) {
        return new AdminAccountResponse(userAccount.username(), userAccount.organizationId(), userAccount.roles());
    }
}