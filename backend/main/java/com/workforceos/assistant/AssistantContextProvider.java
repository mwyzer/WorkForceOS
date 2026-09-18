package com.workforceos.assistant;

import org.springframework.stereotype.Component;

import com.workforceos.attendance.AttendanceReport;
import com.workforceos.dashboard.DashboardOperations;
import com.workforceos.dashboard.DashboardService;
import com.workforceos.risk.RiskAnalysisService;
import com.workforceos.risk.WorkforceRiskSummary;
import com.workforceos.scheduling.LeaveRequestReport;
import com.workforceos.scheduling.OvertimeRequestReport;
import com.workforceos.scheduling.ReportService;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.workforce.DepartmentService;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.TeamService;

/**
 * Assembles a grounded, read-only snapshot of the current workforce state from the existing query
 * services. The assistant never mutates data; it only reports what these services already expose.
 */
@Component
public class AssistantContextProvider {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final ReportService reportService;
    private final DashboardService dashboardService;
    private final RiskAnalysisService riskAnalysisService;
    private final ScheduleEngine scheduleEngine;

    public AssistantContextProvider(EmployeeService employeeService, DepartmentService departmentService,
            TeamService teamService, ReportService reportService, DashboardService dashboardService,
            RiskAnalysisService riskAnalysisService, ScheduleEngine scheduleEngine) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.reportService = reportService;
        this.dashboardService = dashboardService;
        this.riskAnalysisService = riskAnalysisService;
        this.scheduleEngine = scheduleEngine;
    }

    public AssistantSnapshot snapshot() {
        var employees = employeeService.findAll();
        LeaveRequestReport leave = reportService.getLeaveRequestReport();
        OvertimeRequestReport overtime = reportService.getOvertimeRequestReport();
        AttendanceReport attendance = reportService.getAttendanceReport();
        DashboardOperations operations = dashboardService.getOperations();
        WorkforceRiskSummary risk = riskAnalysisService.summary();

        return new AssistantSnapshot(
                employees.size(),
                employees.stream().filter(employee -> employee.active()).count(),
                departmentService.findAll().size(),
                teamService.findAll().size(),
                operations.pendingLeaveRequests(),
                operations.pendingOvertimeRequests(),
                operations.pendingHandovers(),
                operations.publishedRosters(),
                attendance.present(),
                attendance.late(),
                attendance.absent(),
                attendance.overtime(),
                overtime.totalHours() == null ? 0.0 : overtime.totalHours(),
                overtime.approved(),
                overtime.pending(),
                leave.pending(),
                leave.approved(),
                leave.rejected(),
                risk.riskIndex(),
                risk.openAlerts(),
                risk.highCount(),
                scheduleEngine.findConflicts().size());
    }
}