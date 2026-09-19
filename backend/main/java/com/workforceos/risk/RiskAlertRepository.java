package com.workforceos.risk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RiskAlertRepository extends JpaRepository<RiskAlertEntity, UUID> {

    Optional<RiskAlertEntity> findByOrganizationIdAndAssessmentId(UUID organizationId, UUID assessmentId);

    List<RiskAlertEntity> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<RiskAlertEntity> findAllByOrderByCreatedAtDesc();

    long countByOrganizationIdAndStatus(UUID organizationId, RiskAlertStatus status);
}