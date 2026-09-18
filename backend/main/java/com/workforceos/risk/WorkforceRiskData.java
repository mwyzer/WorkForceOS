package com.workforceos.risk;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record WorkforceRiskData(
        Instant generatedAt,
        List<EmployeeSnapshot> employees,
        Map<UUID, String> teamNames,
        Map<UUID, String> departmentNames,
        List<AssignmentSnapshot> assignments,
        List<LeaveSnapshot> approvedLeaves,
        List<OvertimeSnapshot> approvedOvertime,
        Set<SwapSnapshot> approvedSwaps,
        List<AttendanceSnapshot> attendance,
        Map<UUID, ShiftSnapshot> shifts) {

    public record EmployeeSnapshot(
            UUID id,
            String employeeNumber,
            UUID teamId,
            UUID departmentId,
            boolean active) {
    }

    public record AssignmentSnapshot(
            UUID id,
            UUID rosterId,
            UUID employeeId,
            UUID shiftTemplateId,
            Instant start,
            Instant end,
            boolean active) {
    }

    public record LeaveSnapshot(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate) {
    }

    public record OvertimeSnapshot(
            UUID employeeId,
            LocalDate date,
            double hours) {
    }

    public record SwapSnapshot(
            UUID requestingEmployeeId,
            UUID targetEmployeeId,
            LocalDate offeredDate,
            LocalDate requestedDate) {
    }

    public record AttendanceSnapshot(
            UUID employeeId,
            long sessionCount,
            long lateCount,
            long earlyLeaveCount,
            long overtimeMinutes) {
    }

    public record ShiftSnapshot(
            UUID id,
            String name,
            boolean overnight) {
    }
}