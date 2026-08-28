package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {

    private final ConcurrentMap<UUID, Employee> employees = new ConcurrentHashMap<>();

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
        boolean numberExists = employees.values().stream()
                .anyMatch(employee -> employee.employeeNumber().equalsIgnoreCase(request.employeeNumber()));
        if (numberExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number already exists");
        }

        Employee employee = new Employee(
                UUID.randomUUID(),
                request.employeeNumber().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.departmentId(),
                request.teamId(),
                true);
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
                || isBlank(request.employeeNumber())
                || isBlank(request.firstName())
                || isBlank(request.lastName())
                || isBlank(request.email())
                || request.departmentId() == null
                || request.teamId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee fields are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
