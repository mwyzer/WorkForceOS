package com.workforceos.risk;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "risk_alerts")
class RiskAlertEntity {

    @Id
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskAlertStatus status;

    @Column(nullable = false)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected RiskAlertEntity() {
    }

    RiskAlertEntity(UUID id, UUID assessmentId, RiskSeverity severity, String summary) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.severity = severity;
        this.status = RiskAlertStatus.OPEN;
        this.summary = summary;
        this.createdAt = Instant.now();
    }

    RiskAlertStatus status() {
        return status;
    }

    void resolve() {
        this.status = RiskAlertStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    RiskAlert toRecord() {
        return new RiskAlert(id, assessmentId, severity, status, summary, createdAt, resolvedAt);
    }
}