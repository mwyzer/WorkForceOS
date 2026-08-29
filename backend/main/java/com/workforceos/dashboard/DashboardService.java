package com.workforceos.dashboard;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.workforceos.scheduling.ReportService;
import com.workforceos.workforce.Department;
import com.workforceos.workforce.DepartmentService;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.Team;
import com.workforceos.workforce.TeamService;

@Service
public class DashboardService {

    private static final int RECENT_EMPLOYEE_LIMIT = 8;
    private static final String UNASSIGNED = "Unassigned";

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final ReportService reportService;

    public DashboardService(EmployeeService employeeService, DepartmentService departmentService,
            TeamService teamService, ReportService reportService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.reportService = reportService;
    }

    @Cacheable("dashboard-summary")
    public DashboardSummary getSummary() {
        List<Employee> employees = employeeService.findAll();
        List<Department> departments = departmentService.findAll();
        List<Team> teams = teamService.findAll();

        Map<UUID, String> departmentNames = departments.stream()
                .collect(Collectors.toMap(Department::id, Department::name));
        Map<UUID, String> teamNames = teams.stream()
                .collect(Collectors.toMap(Team::id, Team::name));

        long activeEmployees = employees.stream().filter(Employee::active).count();

        List<DepartmentHeadcount> departmentBreakdown = employees.stream()
                .collect(Collectors.groupingBy(
                        employee -> departmentNames.getOrDefault(employee.departmentId(), UNASSIGNED),
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new DepartmentHeadcount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(DepartmentHeadcount::employeeCount).reversed())
                .toList();

        List<EmployeeSummary> recentEmployees = employees.stream()
                .sorted(Comparator.comparing(Employee::employeeNumber).reversed())
                .limit(RECENT_EMPLOYEE_LIMIT)
                .map(employee -> new EmployeeSummary(
                        employee.id(),
                        employee.employeeNumber(),
                        employee.firstName(),
                        employee.lastName(),
                        employee.email(),
                        departmentNames.getOrDefault(employee.departmentId(), UNASSIGNED),
                        teamNames.getOrDefault(employee.teamId(), UNASSIGNED),
                        employee.active()))
                .toList();

        return new DashboardSummary(
                employees.size(),
                activeEmployees,
                departments.size(),
                teams.size(),
                reportService.getLeaveRequestReport().pending(),
                reportService.getOvertimeRequestReport().pending(),
                departmentBreakdown,
                recentEmployees);
    }
}
