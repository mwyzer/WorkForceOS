package com.workforceos.workforce;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
class DepartmentEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected DepartmentEntity() {
    }

    DepartmentEntity(UUID id, UUID organizationId, String name, boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.active = active;
    }

    void deactivate() {
        this.active = false;
    }

    Department toRecord() {
        return new Department(id, organizationId, name, active);
    }

    UUID organizationId() {
        return organizationId;
    }
}
