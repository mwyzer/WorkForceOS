package com.workforceos.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private String details;

    @Column(nullable = false)
    private Instant timestamp;

    protected AuditLogEntity() {
    }

    AuditLogEntity(AuditLog auditLog) {
        this.id = auditLog.id();
        this.organizationId = auditLog.organizationId();
        this.actor = auditLog.actor();
        this.action = auditLog.action();
        this.resource = auditLog.resource();
        this.resourceId = auditLog.resourceId();
        this.details = auditLog.details();
        this.timestamp = auditLog.timestamp();
    }

    AuditLog toRecord() {
        return new AuditLog(id, organizationId, actor, action, resource, resourceId, details, timestamp);
    }
}

interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findAllByOrderByTimestampDesc();
}

@Component
@Profile("!test")
class JpaAuditLogStore implements AuditLogStore {

    private final AuditLogRepository repository;

    JpaAuditLogStore(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AuditLog> findAll() {
        return repository.findAllByOrderByTimestampDesc().stream()
                .map(AuditLogEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return repository.findById(id).map(AuditLogEntity::toRecord);
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        repository.save(new AuditLogEntity(auditLog));
        return auditLog;
    }
}