package com.workforceos.risk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.workforceos.attendance.AttendanceCalculation;
import com.workforceos.attendance.AttendanceEngine;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.RosterStatus;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.schedule.ShiftTemplate;
import com.workforceos.scheduling.ShiftSwapRequest;
import com.workforceos.scheduling.ShiftSwapRequestStatus;
import com.workforceos.scheduling.ShiftSwapRequestService;
import com.workforceos.workforce.Department;
import com.workforceos.workforce.DepartmentService;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;
import com.workforceos.workforce.OvertimeRequest;
import com.workforceos.workforce.OvertimeRequestService;
import com.workforceos.workforce.OvertimeRequestStatus;
import com.workforceos.workforce.TeamService;
import com.workforceos.workforce.Team;

@Service
public class WorkforceRiskDataExtractor {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final ScheduleEngine scheduleEngine;
    private final AttendanceEngine attendanceEngine;
    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;
    private final ShiftSwapRequestService shiftSwapRequestService;

    public WorkforceRiskDataExtractor(EmployeeService employeeService, DepartmentService departmentService,
            TeamService teamService, ScheduleEngine scheduleEngine, AttendanceEngine attendanceEngine,
            LeaveRequestService leaveRequestService, OvertimeRequestService overtimeRequestService,
            ShiftSwapRequestService shiftSwapRequestService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.scheduleEngine = scheduleEngine;
        this.attendanceEngine = attendanceEngine;
        this.leaveRequestService = leaveRequestService;
        this.overtimeRequestService = overtimeRequestService;
        this.shiftSwapRequestService = shiftSwapRequestService;
    }

    public WorkforceRiskData extract() {
        List<Employee> employees = employeeService.findAll();

        Map<java.util.UUID, String> departmentNames = departmentService.findAll().stream()
                .collect(Collectors.toMap(Department::id, Department::name));
        Map<java.util.UUID, String> teamNames = teamService.findAll().stream()
                .collect(Collectors.toMap(Team::id, Team::name));

        List<WorkforceRiskData.AssignmentSnapshot> assignments = new ArrayList<>();
        scheduleEngine.findAllRosters().stream()
                .filter(roster -> roster.status() == RosterStatus.PUBLISHED)
                .forEach(roster -> scheduleEngine.findAssignments(roster.id()).stream()
                        .map(assignment -> toAssignment(assignment))
                        .forEach(assignments::add));

        List<WorkforceRiskData.LeaveSnapshot> approvedLeaves = leaveRequestService.findAll().stream()
                .filter(request -> request.status() == LeaveRequestStatus.APPROVED)
                .map(request -> new WorkforceRiskData.LeaveSnapshot(request.employeeId(), request.startDate(),
                        request.endDate()))
                .toList();

        List<WorkforceRiskData.OvertimeSnapshot> approvedOvertime = overtimeRequestService.findAll().stream()
                .filter(request -> request.status() == OvertimeRequestStatus.APPROVED)
                .map(request -> new WorkforceRiskData.OvertimeSnapshot(request.employeeId(), request.date(),
                        request.hours() == null ? 0 : request.hours()))
                .toList();

        List<WorkforceRiskData.SwapSnapshot> approvedSwaps = shiftSwapRequestService.findAll().stream()
                .filter(request -> request.status() == ShiftSwapRequestStatus.APPROVED)
                .map(request -> new WorkforceRiskData.SwapSnapshot(request.requestingEmployeeId(),
                        request.targetEmployeeId(), request.offeredDate(), request.requestedDate()))
                .toList();

        List<WorkforceRiskData.AttendanceSnapshot> attendance = groupAttendance();

        Map<java.util.UUID, WorkforceRiskData.ShiftSnapshot> shifts = new LinkedHashMap<>();
        scheduleEngine.findAllShifts().forEach(shift -> shifts.put(shift.id(),
                new WorkforceRiskData.ShiftSnapshot(shift.id(), shift.name(), shift.overnight())));

        return new WorkforceRiskData(
                Instant.now(),
                employees.stream().map(WorkforceRiskDataExtractor::toEmployee).toList(),
                teamNames,
                departmentNames,
                List.copyOf(assignments),
                approvedLeaves,
                approvedOvertime,
                approvedSwaps.stream().collect(Collectors.toSet()),
                attendance,
                shifts);
    }

    private static WorkforceRiskData.EmployeeSnapshot toEmployee(Employee employee) {
        return new WorkforceRiskData.EmployeeSnapshot(employee.id(), employee.employeeNumber(), employee.teamId(),
                employee.departmentId(), employee.active());
    }

    private static WorkforceRiskData.AssignmentSnapshot toAssignment(RosterAssignment assignment) {
        return new WorkforceRiskData.AssignmentSnapshot(assignment.id(), assignment.rosterId(),
                assignment.employeeId(), assignment.shiftTemplateId(),
                assignment.start().toInstant(), assignment.end().toInstant(), assignment.active());
    }

    private List<WorkforceRiskData.AttendanceSnapshot> groupAttendance() {
        Map<java.util.UUID, AttendanceAggregate> aggregates = new HashMap<>();
        attendanceEngine.calculateAll().forEach(calculation -> {
            AttendanceAggregate aggregate = aggregates.computeIfAbsent(
                    calculation.session().employeeId(), ignored -> new AttendanceAggregate());
            aggregate.sessionCount++;
            if (calculation.late()) {
                aggregate.lateCount++;
            }
            if (calculation.earlyLeave()) {
                aggregate.earlyLeaveCount++;
            }
            aggregate.overtimeMinutes += calculation.overtimeMinutes();
        });
        return aggregates.entrySet().stream()
                .map(entry -> new WorkforceRiskData.AttendanceSnapshot(
                        entry.getKey(),
                        entry.getValue().sessionCount,
                        entry.getValue().lateCount,
                        entry.getValue().earlyLeaveCount,
                        entry.getValue().overtimeMinutes))
                .toList();
    }

    private static final class AttendanceAggregate {
        long sessionCount;
        long lateCount;
        long earlyLeaveCount;
        long overtimeMinutes;
    }
}