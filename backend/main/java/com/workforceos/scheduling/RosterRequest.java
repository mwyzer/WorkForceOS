package com.workforceos.scheduling;

import java.util.UUID;

public record RosterRequest(
        UUID organizationId,
        String name) {
}
