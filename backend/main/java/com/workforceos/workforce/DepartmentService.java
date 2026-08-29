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
public class DepartmentService {

    private final ConcurrentMap<UUID, Department> departments = new ConcurrentHashMap<>();

    public List<Department> findAll() {
        return departments.values().stream().toList();
    }

    public Department findById(UUID id) {
        Department department = departments.get(id);
        if (department == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found");
        }
        return department;
    }

    public Department create(DepartmentRequest request) {
        validate(request);

        Department department = new Department(UUID.randomUUID(), request.organizationId(), request.name().trim(), true);
        departments.put(department.id(), department);
        return department;
    }

    public Department deactivate(UUID id) {
        Department department = findById(id);
        Department deactivated = new Department(
                department.id(),
                department.organizationId(),
                department.name(),
                false);
        departments.put(id, deactivated);
        return deactivated;
    }

    private void validate(DepartmentRequest request) {
        if (request == null || request.organizationId() == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization and department name are required");
        }
    }
}
