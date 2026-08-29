package com.workforceos.dashboard;

import java.util.List;

public record DashboardSummary(
        long totalEmployees,
        long activeEmployees,
        long totalDepartments,
        long totalTeams,
        long pendingLeaveRequests,
        long pendingOvertimeRequests,
        List<DepartmentHeadcount> departmentBreakdown,
        List<EmployeeSummary> recentEmployees) {
}
