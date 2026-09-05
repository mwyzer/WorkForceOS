package com.workforceos.schedule;

import java.util.UUID;

public record RosterRequest(
        UUID organizationId,
        String name) {
}
