package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.TenantScope;
import com.workforceos.shared.ValidationUtils;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Cacheable(value = "departments", key = "T(com.workforceos.organization.TenantContext).require().toString()")
    public List<Department> findAll() {
        UUID tenantId = TenantScope.require();
        return departmentRepository.findAll().stream()
                .filter(department -> department.organizationId() != null
                        && department.organizationId().equals(tenantId))
                .map(DepartmentEntity::toRecord)
                .toList();
    }

    @Cacheable(value = "department",
            key = "T(com.workforceos.organization.TenantContext).require().toString() + ':' + #id")
    public Department findById(UUID id) {
        DepartmentEntity department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        TenantScope.assertAccess(department.organizationId());
        return department.toRecord();
    }

    @CacheEvict(value = "departments", allEntries = true)
    public Department create(DepartmentRequest request) {
        validate(request);

        DepartmentEntity department = new DepartmentEntity(UUID.randomUUID(), TenantScope.require(),
                request.name().trim(), true);
        departmentRepository.save(department);
        return department.toRecord();
    }

    @Caching(evict = {
            @CacheEvict(value = "departments", allEntries = true),
            @CacheEvict(value = "department",
                    key = "T(com.workforceos.organization.TenantContext).require().toString() + ':' + #id")
    })
    public Department deactivate(UUID id) {
        DepartmentEntity department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        TenantScope.assertAccess(department.organizationId());
        department.deactivate();
        departmentRepository.save(department);
        return department.toRecord();
    }

    private void validate(DepartmentRequest request) {
        if (request == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department name is required");
        }
    }
}
