package com.workforceos.scheduling;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> findAll() {
        return auditLogService.findAll();
    }

    @GetMapping("/audit-logs/{id}")
    public AuditLog findById(@PathVariable UUID id) {
        return auditLogService.findById(id);
    }

    @PostMapping("/audit-logs")
    public ResponseEntity<AuditLog> create(@RequestBody AuditLogRequest request) {
        AuditLog auditLog = auditLogService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/audit-logs/" + auditLog.id()))
                .body(auditLog);
    }

    @PostMapping("/audit-logs/{id}")
    public AuditLog rejectPost(@PathVariable UUID id) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED,
                "POST is not allowed for a specific audit log record");
    }
}
