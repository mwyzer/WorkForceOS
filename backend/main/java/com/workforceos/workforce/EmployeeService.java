package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class EmployeeService {

    private final ConcurrentMap<UUID, Employee> employees = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> employeeNumberIndex = new ConcurrentHashMap<>();
    private final DepartmentService departmentService;
    private final TeamService teamService;

    public EmployeeService(DepartmentService departmentService, TeamService teamService) {
        this.departmentService = departmentService;
        this.teamService = teamService;
    }

    public List<Employee> findAll() {
        return employees.values().stream().toList();
    }

    public Employee findById(UUID id) {
        Employee employee = employees.get(id);
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        return employee;
    }

    public Employee create(CreateEmployeeRequest request) {
        validate(request);

        Employee employee = new Employee(
                UUID.randomUUID(),
                request.employeeNumber().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.departmentId(),
                request.teamId(),
                true);

        String numberKey = employee.employeeNumber().toLowerCase();
        if (employeeNumberIndex.putIfAbsent(numberKey, employee.id()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number already exists");
        }
        employees.put(employee.id(), employee);
        return employee;
    }

    public Employee deactivate(UUID id) {
        Employee employee = findById(id);
        Employee deactivated = new Employee(
                employee.id(),
                employee.employeeNumber(),
                employee.firstName(),
                employee.lastName(),
                employee.email(),
                employee.departmentId(),
                employee.teamId(),
                false);
        employees.put(id, deactivated);
        return deactivated;
    }

    private void validate(CreateEmployeeRequest request) {
        if (request == null
                || ValidationUtils.isBlank(request.employeeNumber())
                || ValidationUtils.isBlank(request.firstName())
                || ValidationUtils.isBlank(request.lastName())
                || ValidationUtils.isBlank(request.email())
                || request.departmentId() == null
                || request.teamId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee fields are required");
        }
        departmentService.findById(request.departmentId());
        teamService.findById(request.teamId());
    }
}
