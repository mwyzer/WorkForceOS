package com.workforceos.workforce;

import java.util.UUID;

public record TeamRequest(
        UUID departmentId,
        String name) {
}
