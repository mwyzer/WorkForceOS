package com.workforceos.workforce;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
class TeamEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected TeamEntity() {
    }

    TeamEntity(UUID id, UUID organizationId, UUID departmentId, String name, boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.departmentId = departmentId;
        this.name = name;
        this.active = active;
    }

    void deactivate() {
        this.active = false;
    }

    UUID organizationId() {
        return organizationId;
    }

    Team toRecord() {
        return new Team(id, departmentId, name, active);
    }
}
