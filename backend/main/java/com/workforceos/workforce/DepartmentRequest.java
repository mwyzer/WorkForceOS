package com.workforceos.workforce;

import java.util.UUID;

public record DepartmentRequest(
        UUID organizationId,
        String name) {
}
