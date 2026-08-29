package com.workforceos.workforce;

import java.util.UUID;

public record Department(
        UUID id,
        UUID organizationId,
        String name,
        boolean active) {
}
