package com.workforceos.bootstrap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.workforceos.workforce.CreateEmployeeRequest;
import com.workforceos.workforce.Department;
import com.workforceos.workforce.DepartmentRequest;
import com.workforceos.workforce.DepartmentService;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.OvertimeRequest;
import com.workforceos.workforce.OvertimeRequestRequest;
import com.workforceos.workforce.OvertimeRequestService;
import com.workforceos.workforce.Team;
import com.workforceos.workforce.TeamRequest;
import com.workforceos.workforce.TeamService;

@Component
@Profile("!test")
class DemoDataSeeder implements CommandLineRunner {

    private static final UUID DEMO_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String[] DEPARTMENTS = {
            "Engineering", "Sales", "Operations", "Human Resources", "Finance"
    };

    private static final String[] FIRST_NAMES = {
            "Ava", "Liam", "Noah", "Emma", "Olivia", "Elijah", "Sophia", "James", "Isabella", "Benjamin"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones"
    };

    private static final String[] LEAVE_REASONS = {
            "Family vacation", "Medical appointment", "Personal time off", "Relocation", "Wedding"
    };

    private static final String[] OVERTIME_REASONS = {
            "Month-end close coverage", "Production incident support", "Peak season coverage", "System migration"
    };

    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final EmployeeService employeeService;
    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;

    DemoDataSeeder(DepartmentService departmentService, TeamService teamService, EmployeeService employeeService,
            LeaveRequestService leaveRequestService, OvertimeRequestService overtimeRequestService) {
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.employeeService = employeeService;
        this.leaveRequestService = leaveRequestService;
        this.overtimeRequestService = overtimeRequestService;
    }

    @Override
    public void run(String... args) {
        if (!departmentService.findAll().isEmpty()) {
            return;
        }

        Team[] teams = new Team[DEPARTMENTS.length * 2];
        for (int d = 0; d < DEPARTMENTS.length; d++) {
            Department department = departmentService.create(new DepartmentRequest(DEMO_ORGANIZATION_ID, DEPARTMENTS[d]));
            teams[d * 2] = teamService.create(new TeamRequest(department.id(), DEPARTMENTS[d] + " - Team A"));
            teams[d * 2 + 1] = teamService.create(new TeamRequest(department.id(), DEPARTMENTS[d] + " - Team B"));
        }

        List<Employee> employees = new ArrayList<>();
        int employeeNumber = 1;
        for (String lastName : LAST_NAMES) {
            for (String firstName : FIRST_NAMES) {
                Team team = teams[(employeeNumber - 1) % teams.length];
                String email = (firstName + "." + lastName + employeeNumber + "@workforceos.local").toLowerCase();
                employees.add(employeeService.create(new CreateEmployeeRequest(
                        String.format("EMP-%04d", employeeNumber),
                        firstName,
                        lastName,
                        email,
                        team.departmentId(),
                        team.id())));
                employeeNumber++;
            }
        }

        seedLeaveRequests(employees);
        seedOvertimeRequests(employees);
    }

    private void seedLeaveRequests(List<Employee> employees) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 12; i++) {
            Employee employee = employees.get(i % employees.size());
            LocalDate start = today.plusDays(3 + i * 4);
            LocalDate end = start.plusDays(1 + (i % 3));
            String reason = LEAVE_REASONS[i % LEAVE_REASONS.length];
            LeaveRequest leaveRequest = leaveRequestService.create(new LeaveRequestRequest(employee.id(), start, end, reason));
            if (i % 4 == 0) {
                leaveRequestService.approve(leaveRequest.id());
            }
        }
    }

    private void seedOvertimeRequests(List<Employee> employees) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            Employee employee = employees.get((i * 3) % employees.size());
            LocalDate date = today.plusDays(2 + i * 3);
            double hours = 2.0 + (i % 4);
            String reason = OVERTIME_REASONS[i % OVERTIME_REASONS.length];
            OvertimeRequest overtimeRequest = overtimeRequestService.create(
                    new OvertimeRequestRequest(employee.id(), date, hours, reason));
            if (i % 5 == 0) {
                overtimeRequestService.approve(overtimeRequest.id());
            } else if (i % 5 == 1) {
                overtimeRequestService.reject(overtimeRequest.id());
            }
        }
    }
}
