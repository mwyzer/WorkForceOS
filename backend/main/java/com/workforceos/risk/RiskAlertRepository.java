package com.workforceos.risk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RiskAlertRepository extends JpaRepository<RiskAlertEntity, UUID> {

    Optional<RiskAlertEntity> findByAssessmentId(UUID assessmentId);

    List<RiskAlertEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(RiskAlertStatus status);
}