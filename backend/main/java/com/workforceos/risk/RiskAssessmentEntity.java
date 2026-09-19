package com.workforceos.risk;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "risk_assessments")
class RiskAssessmentEntity {

    private static final String EVIDENCE_DELIMITER = " | ";

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_type", nullable = false)
    private RiskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskSeverity severity;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private RiskEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "impact_minutes", nullable = false)
    private long impactMinutes;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private String evidence;

    @Column(name = "dedupe_key", nullable = false, unique = true)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RiskAssessmentEntity() {
    }

    RiskAssessmentEntity(UUID organizationId, RiskAssessment assessment, String dedupeKey) {
        this.id = assessment.id();
        this.organizationId = organizationId;
        this.type = assessment.type();
        this.severity = assessment.severity();
        this.score = assessment.score();
        this.entityType = assessment.entityType();
        this.entityId = assessment.entityId();
        this.teamId = assessment.teamId();
        this.departmentId = assessment.departmentId();
        this.windowStart = assessment.windowStart();
        this.windowEnd = assessment.windowEnd();
        this.impactMinutes = assessment.impactMinutes();
        this.summary = assessment.summary();
        this.evidence = String.join(EVIDENCE_DELIMITER, assessment.evidence());
        this.dedupeKey = dedupeKey;
        this.createdAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    UUID organizationId() {
        return organizationId;
    }

    String dedupeKey() {
        return dedupeKey;
    }

    Instant createdAt() {
        return createdAt;
    }

    RiskSeverity severity() {
        return severity;
    }

    RiskAssessment toRecord() {
        List<String> evidenceList = evidence == null || evidence.isBlank()
                ? List.of()
                : Arrays.stream(evidence.split("\\Q" + EVIDENCE_DELIMITER + "\\E"))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
        return new RiskAssessment(id, type, severity, score, entityType, entityId, teamId, departmentId,
                windowStart, windowEnd, impactMinutes, summary, evidenceList);
    }
}