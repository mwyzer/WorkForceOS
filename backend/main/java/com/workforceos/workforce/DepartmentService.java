package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Cacheable("departments")
    public List<Department> findAll() {
        return departmentRepository.findAll().stream().map(DepartmentEntity::toRecord).toList();
    }

    @Cacheable(value = "department", key = "#id")
    public Department findById(UUID id) {
        return departmentRepository.findById(id)
                .map(DepartmentEntity::toRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    }

    @CacheEvict(value = "departments", allEntries = true)
    public Department create(DepartmentRequest request) {
        validate(request);

        DepartmentEntity department = new DepartmentEntity(UUID.randomUUID(), request.organizationId(), request.name().trim(), true);
        departmentRepository.save(department);
        return department.toRecord();
    }

    @Caching(evict = {
            @CacheEvict(value = "departments", allEntries = true),
            @CacheEvict(value = "department", key = "#id")
    })
    public Department deactivate(UUID id) {
        DepartmentEntity department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        department.deactivate();
        departmentRepository.save(department);
        return department.toRecord();
    }

    private void validate(DepartmentRequest request) {
        if (request == null || request.organizationId() == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization and department name are required");
        }
    }
}
