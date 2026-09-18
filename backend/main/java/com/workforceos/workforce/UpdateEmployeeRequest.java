package com.workforceos.workforce;

import java.util.UUID;

public record UpdateEmployeeRequest(
        String firstName,
        String lastName,
        String email,
        UUID departmentId,
        UUID teamId) {
}