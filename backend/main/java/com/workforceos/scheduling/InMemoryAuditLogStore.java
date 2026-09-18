package com.workforceos.scheduling;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryAuditLogStore implements AuditLogStore {

    private final ConcurrentMap<UUID, AuditLog> auditLogs = new ConcurrentHashMap<>();

    @Override
    public List<AuditLog> findAll() {
        return auditLogs.values().stream()
                .sorted(Comparator.comparing(AuditLog::timestamp).reversed())
                .toList();
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return Optional.ofNullable(auditLogs.get(id));
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        auditLogs.put(auditLog.id(), auditLog);
        return auditLog;
    }
}