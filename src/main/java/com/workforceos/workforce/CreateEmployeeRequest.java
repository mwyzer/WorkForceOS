package com.workforceos.workforce;

import java.util.UUID;

public record CreateEmployeeRequest(
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        UUID departmentId,
        UUID teamId) {
}
