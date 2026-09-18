package com.workforceos.assistant;

public record AssistantSnapshot(
        int employeeCount,
        long activeEmployees,
        int departmentCount,
        int teamCount,
        long pendingLeaveRequests,
        long pendingOvertimeRequests,
        long pendingHandovers,
        long publishedRosters,
        long attendancePresent,
        long attendanceLate,
        long attendanceAbsent,
        long attendanceOvertime,
        double overtimeTotalHours,
        long overtimeApproved,
        long overtimePending,
        long leavePending,
        long leaveApproved,
        long leaveRejected,
        int riskIndex,
        long openAlerts,
        long highRiskAssessments,
        int scheduleConflicts) {
}