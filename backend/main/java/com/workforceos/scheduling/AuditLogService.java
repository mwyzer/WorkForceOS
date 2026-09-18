package com.workforceos.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class AuditLogService {

    private final AuditLogStore auditLogStore;

    public AuditLogService(AuditLogStore auditLogStore) {
        this.auditLogStore = auditLogStore;
    }

    public List<AuditLog> findAll() {
        return auditLogStore.findAll();
    }

    @Transactional
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

        return auditLogStore.save(auditLog);
    }

    public AuditLog findById(UUID id) {
        return auditLogStore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));
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