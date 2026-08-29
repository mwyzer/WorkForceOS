package com.workforceos.scheduling;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class AuditLogService {

    private final ConcurrentMap<UUID, AuditLog> auditLogs = new ConcurrentHashMap<>();

    public List<AuditLog> findAll() {
        return auditLogs.values().stream()
                .sorted(Comparator.comparing(AuditLog::timestamp).reversed())
                .toList();
    }

    public AuditLog create(AuditLogRequest request) {
        validateRequest(request);

        AuditLog auditLog = new AuditLog(
                UUID.randomUUID(),
                request.actor().trim(),
                request.action().trim().toUpperCase(Locale.ROOT),
                request.resource().trim(),
                request.resourceId(),
                normalizeDetails(request.details()),
                Instant.now());

        auditLogs.put(auditLog.id(), auditLog);
        return auditLog;
    }

    public AuditLog findById(UUID id) {
        AuditLog auditLog = auditLogs.get(id);
        if (auditLog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found");
        }
        return auditLog;
    }

    private void validateRequest(AuditLogRequest request) {
        if (request == null
                || ValidationUtils.isBlank(request.actor())
                || ValidationUtils.isBlank(request.action())
                || ValidationUtils.isBlank(request.resource())
                || request.resourceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Actor, action, resource, and resourceId are required");
        }
    }

    private String normalizeDetails(String details) {
        return details == null ? "" : details.trim();
    }
}
