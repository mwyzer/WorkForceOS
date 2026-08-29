package com.workforceos.bootstrap;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.workforceos.workforce.CreateEmployeeRequest;
import com.workforceos.workforce.Department;
import com.workforceos.workforce.DepartmentRequest;
import com.workforceos.workforce.DepartmentService;
import com.workforceos.workforce.EmployeeService;
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

    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final EmployeeService employeeService;

    DemoDataSeeder(DepartmentService departmentService, TeamService teamService, EmployeeService employeeService) {
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.employeeService = employeeService;
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

        int employeeNumber = 1;
        for (String lastName : LAST_NAMES) {
            for (String firstName : FIRST_NAMES) {
                Team team = teams[(employeeNumber - 1) % teams.length];
                String email = (firstName + "." + lastName + employeeNumber + "@workforceos.local").toLowerCase();
                employeeService.create(new CreateEmployeeRequest(
                        String.format("EMP-%04d", employeeNumber),
                        firstName,
                        lastName,
                        email,
                        team.departmentId(),
                        team.id()));
                employeeNumber++;
            }
        }
    }
}
