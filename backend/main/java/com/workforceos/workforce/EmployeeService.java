package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.event.DomainEvents;
import com.workforceos.event.EventPublisher;
import com.workforceos.event.EventTypes;
import com.workforceos.event.Payloads;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.ValidationUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final EventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentService departmentService,
            TeamService teamService, EventPublisher eventPublisher, CurrentUser currentUser) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    @Cacheable("employees")
    public List<Employee> findAll() {
        return employeeRepository.findAll().stream().map(EmployeeEntity::toRecord).toList();
    }

    @Cacheable(value = "employee", key = "#id")
    public Employee findById(UUID id) {
        return employeeRepository.findById(id)
                .map(EmployeeEntity::toRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @CacheEvict(value = "employees", allEntries = true)
    public Employee create(CreateEmployeeRequest request) {
        validate(request);

        String employeeNumber = request.employeeNumber().trim();
        if (employeeRepository.existsByEmployeeNumberIgnoreCase(employeeNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number already exists");
        }

        EmployeeEntity employee = new EmployeeEntity(
                UUID.randomUUID(),
                employeeNumber,
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.departmentId(),
                request.teamId(),
                true);

        try {
            employeeRepository.save(employee);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number already exists");
        }
        return employee.toRecord();
    }

    @Caching(evict = {
            @CacheEvict(value = "employees", allEntries = true),
            @CacheEvict(value = "employee", key = "#id")
    })
    public Employee update(UUID id, UpdateEmployeeRequest request) {
        validateUpdate(request);
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        employee.update(
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.departmentId(),
                request.teamId());
        employeeRepository.save(employee);
        return employee.toRecord();
    }

    @Caching(evict = {
            @CacheEvict(value = "employees", allEntries = true),
            @CacheEvict(value = "employee", key = "#id")
    })
    public Employee deactivate(UUID id) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        employee.deactivate();
        employeeRepository.save(employee);
        Employee deactivated = employee.toRecord();
        eventPublisher.publish(DomainEvents.of(EventTypes.EMPLOYEE_DEACTIVATED, null, "Employee", id,
                currentUser.username(),
                Payloads.json(java.util.Map.of(
                        "employeeId", id.toString(),
                        "employeeNumber", deactivated.employeeNumber()))));
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

    private void validateUpdate(UpdateEmployeeRequest request) {
        if (request == null
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
