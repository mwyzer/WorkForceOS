package com.workforceos.dashboard;

import java.util.UUID;

public record EmployeeSummary(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String departmentName,
        String teamName,
        boolean active) {
}
