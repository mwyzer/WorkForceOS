package com.workforceos.workforce;

import java.util.UUID;

public record Employee(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        UUID departmentId,
        UUID teamId,
        boolean active) {
}
