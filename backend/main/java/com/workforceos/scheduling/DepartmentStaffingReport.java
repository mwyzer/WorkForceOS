package com.workforceos.scheduling;

import java.util.UUID;

public record DepartmentStaffingReport(
        UUID departmentId,
        String departmentName,
        long headcount,
        long activeHeadcount) {
}