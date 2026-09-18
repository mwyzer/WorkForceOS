package com.workforceos.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogStore {

    List<AuditLog> findAll();

    Optional<AuditLog> findById(UUID id);

    AuditLog save(AuditLog auditLog);
}