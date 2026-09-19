package com.workforceos.scheduling;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.workforceos.attendance.AttendanceCalculation;
import com.workforceos.attendance.AttendanceEngine;
import com.workforceos.attendance.AttendanceReport;
import com.workforceos.attendance.AttendanceSession;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;
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

@Service
public class ReportService {

    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;
    private final AuditLogService auditLogService;
    private final AttendanceEngine attendanceEngine;
    private final ScheduleEngine scheduleEngine;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public ReportService(LeaveRequestService leaveRequestService,
            OvertimeRequestService overtimeRequestService,
            AuditLogService auditLogService,
            AttendanceEngine attendanceEngine,
            ScheduleEngine scheduleEngine,
            EmployeeService employeeService,
            DepartmentService departmentService) {
        this.leaveRequestService = leaveRequestService;
        this.overtimeRequestService = overtimeRequestService;
        this.auditLogService = auditLogService;
        this.attendanceEngine = attendanceEngine;
        this.scheduleEngine = scheduleEngine;
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    @Cacheable(value = "leave-request-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public LeaveRequestReport getLeaveRequestReport() {
        Map<LeaveRequestStatus, Long> counts = leaveRequestService.findAll().stream()
                .collect(Collectors.groupingBy(LeaveRequest::status, Collectors.counting()));

        return new LeaveRequestReport(
                counts.getOrDefault(LeaveRequestStatus.PENDING, 0L),
                counts.getOrDefault(LeaveRequestStatus.APPROVED, 0L),
                counts.getOrDefault(LeaveRequestStatus.REJECTED, 0L));
    }

    @Cacheable(value = "overtime-request-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public OvertimeRequestReport getOvertimeRequestReport() {
        List<OvertimeRequest> allRequests = overtimeRequestService.findAll();
        Map<OvertimeRequestStatus, Long> counts = allRequests.stream()
                .collect(Collectors.groupingBy(OvertimeRequest::status, Collectors.counting()));
        double totalHours = allRequests.stream().mapToDouble(OvertimeRequest::hours).sum();

        return new OvertimeRequestReport(
                allRequests.size(),
                counts.getOrDefault(OvertimeRequestStatus.APPROVED, 0L),
                counts.getOrDefault(OvertimeRequestStatus.REJECTED, 0L),
                counts.getOrDefault(OvertimeRequestStatus.PENDING, 0L),
                totalHours);
    }

    @Cacheable(value = "attendance-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public AttendanceReport getAttendanceReport() {
        List<AttendanceCalculation> calculations = attendanceEngine.calculateAll();

        long present = calculations.size();
        long late = calculations.stream().filter(AttendanceCalculation::late).count();
        long earlyLeave = calculations.stream().filter(AttendanceCalculation::earlyLeave).count();
        long overtime = calculations.stream().filter(calculation -> calculation.overtimeMinutes() > 0).count();

        OffsetDateTime now = OffsetDateTime.now();
        long absent = scheduleEngine.findAllAssignments().stream()
                .filter(RosterAssignment::active)
                .filter(assignment -> assignment.end().isBefore(now))
                .filter(assignment -> calculations.stream()
                        .noneMatch(calculation -> matchesAssignment(calculation.session(), assignment)))
                .count();

        return new AttendanceReport(present, late, absent, earlyLeave, overtime);
    }

    private boolean matchesAssignment(AttendanceSession session, RosterAssignment assignment) {
        return session.rosterId().equals(assignment.rosterId())
                && session.employeeId().equals(assignment.employeeId())
                && session.shiftTemplateId().equals(assignment.shiftTemplateId());
    }

    @Cacheable(value = "audit-log-summary", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public AuditLogSummary getAuditLogSummary() {
        List<AuditLog> allLogs = auditLogService.findAll();

        Set<String> distinctActors = allLogs.stream().map(AuditLog::actor).collect(Collectors.toSet());
        Set<String> distinctActions = allLogs.stream().map(AuditLog::action).collect(Collectors.toSet());
        Set<String> distinctResources = allLogs.stream().map(AuditLog::resource).collect(Collectors.toSet());

        return new AuditLogSummary(allLogs.size(), distinctActors.size(), distinctActions.size(), distinctResources.size());
    }

    @Cacheable(value = "attendance-by-employee-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public List<AttendanceByEmployeeReport> getAttendanceByEmployeeReport() {
        Map<UUID, Employee> employeesById = employeeService.findAll().stream()
                .collect(Collectors.toMap(Employee::id, employee -> employee));

        return attendanceEngine.calculateAll().stream()
                .collect(Collectors.groupingBy(calculation -> calculation.session().employeeId()))
                .entrySet().stream()
                .map(entry -> toAttendanceReport(employeeById(employeesById, entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(AttendanceByEmployeeReport::employeeNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private AttendanceByEmployeeReport toAttendanceReport(Employee employee, List<AttendanceCalculation> calculations) {
        return new AttendanceByEmployeeReport(
                employee != null ? employee.id() : null,
                employee != null ? employee.employeeNumber() : null,
                employee != null ? employee.firstName() : null,
                employee != null ? employee.lastName() : null,
                calculations.size(),
                calculations.stream().mapToLong(AttendanceCalculation::workedMinutes).sum(),
                calculations.stream().filter(AttendanceCalculation::late).count(),
                calculations.stream().filter(AttendanceCalculation::earlyLeave).count());
    }

    @Cacheable(value = "overtime-by-employee-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public List<OvertimeByEmployeeReport> getOvertimeByEmployeeReport() {
        Map<UUID, Employee> employeesById = employeeService.findAll().stream()
                .collect(Collectors.toMap(Employee::id, employee -> employee));

        return overtimeRequestService.findAll().stream()
                .collect(Collectors.groupingBy(OvertimeRequest::employeeId))
                .entrySet().stream()
                .map(entry -> toOvertimeReport(employeeById(employeesById, entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(OvertimeByEmployeeReport::employeeNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private OvertimeByEmployeeReport toOvertimeReport(Employee employee, List<OvertimeRequest> requests) {
        Map<OvertimeRequestStatus, Long> counts = requests.stream()
                .collect(Collectors.groupingBy(OvertimeRequest::status, Collectors.counting()));
        double totalApprovedHours = requests.stream()
                .filter(request -> request.status() == OvertimeRequestStatus.APPROVED)
                .mapToDouble(OvertimeRequest::hours)
                .sum();
        return new OvertimeByEmployeeReport(
                employee != null ? employee.id() : null,
                employee != null ? employee.employeeNumber() : null,
                employee != null ? employee.firstName() : null,
                employee != null ? employee.lastName() : null,
                counts.getOrDefault(OvertimeRequestStatus.APPROVED, 0L),
                counts.getOrDefault(OvertimeRequestStatus.PENDING, 0L),
                counts.getOrDefault(OvertimeRequestStatus.REJECTED, 0L),
                totalApprovedHours);
    }

    @Cacheable(value = "department-staffing-report", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public List<DepartmentStaffingReport> getDepartmentStaffingReport() {
        Map<UUID, String> departmentNames = departmentService.findAll().stream()
                .collect(Collectors.toMap(Department::id, Department::name, (first, second) -> first));

        return employeeService.findAll().stream()
                .collect(Collectors.groupingBy(Employee::departmentId))
                .entrySet().stream()
                .map(entry -> new DepartmentStaffingReport(
                        entry.getKey(),
                        departmentNames.getOrDefault(entry.getKey(), "Unassigned"),
                        entry.getValue().size(),
                        entry.getValue().stream().filter(Employee::active).count()))
                .sorted(Comparator.comparingLong(DepartmentStaffingReport::headcount).reversed())
                .toList();
    }

    private Employee employeeById(Map<UUID, Employee> employeesById, UUID employeeId) {
        return employeeId == null ? null : employeesById.get(employeeId);
    }
}
