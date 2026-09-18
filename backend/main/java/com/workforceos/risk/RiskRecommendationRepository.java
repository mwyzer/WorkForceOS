package com.workforceos.risk;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RiskRecommendationRepository extends JpaRepository<RiskRecommendationEntity, UUID> {

    List<RiskRecommendationEntity> findAllByAssessmentId(UUID assessmentId);

    void deleteByAssessmentId(UUID assessmentId);
}