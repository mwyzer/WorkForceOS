package com.workforceos.risk;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "risk_recommendations")
class RiskRecommendationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "risk_type", nullable = false)
    private String riskType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "action_endpoint", nullable = false)
    private String actionEndpoint;

    protected RiskRecommendationEntity() {
    }

    RiskRecommendationEntity(UUID id, UUID organizationId, UUID assessmentId, RiskRecommendation recommendation) {
        this.id = id;
        this.organizationId = organizationId;
        this.assessmentId = assessmentId;
        this.riskType = recommendation.riskType();
        this.title = recommendation.title();
        this.description = recommendation.description();
        this.actionType = recommendation.actionType();
        this.actionEndpoint = recommendation.actionEndpoint();
    }

    UUID assessmentId() {
        return assessmentId;
    }

    UUID organizationId() {
        return organizationId;
    }

    RiskRecommendation toRecord() {
        return new RiskRecommendation(riskType, title, description, actionType, actionEndpoint);
    }
}