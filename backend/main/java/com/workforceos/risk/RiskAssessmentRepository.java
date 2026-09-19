package com.workforceos.risk;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RiskAssessmentRepository extends JpaRepository<RiskAssessmentEntity, UUID> {

    Optional<RiskAssessmentEntity> findByDedupeKey(String dedupeKey);

    List<RiskAssessmentEntity> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<RiskAssessmentEntity> findAllByOrderByCreatedAtDesc();
}