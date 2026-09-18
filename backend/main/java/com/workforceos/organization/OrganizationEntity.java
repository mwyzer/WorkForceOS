package com.workforceos.organization;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
class OrganizationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OrganizationEntity() {
    }

    OrganizationEntity(UUID id, String name, String timezone, boolean active, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.timezone = timezone;
        this.active = active;
        this.createdAt = createdAt;
    }

    Organization toRecord() {
        return new Organization(id, name, timezone, active, createdAt);
    }
}