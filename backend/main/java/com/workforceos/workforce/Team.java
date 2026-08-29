package com.workforceos.workforce;

import java.util.UUID;

public record Team(
        UUID id,
        UUID departmentId,
        String name,
        boolean active) {
}
