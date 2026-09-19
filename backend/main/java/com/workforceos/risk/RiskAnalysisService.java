package com.workforceos.risk;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.event.DomainEvents;
import com.workforceos.event.EventPublisher;
import com.workforceos.event.EventTypes;
import com.workforceos.event.Payloads;
import com.workforceos.shared.TenantScope;

@Service
public class RiskAnalysisService {

    private static final int TOP_ASSESSMENTS = 10;

    private final WorkforceRiskDataExtractor extractor;
    private final RiskEngine engine;
    private final RiskImpactCalculator impactCalculator;
    private final RiskAdvisorService advisor;
    private final HeuristicRiskAdvisor heuristic;
    private final RiskAssessmentRepository assessmentRepository;
    private final RiskRecommendationRepository recommendationRepository;
    private final RiskAlertRepository alertRepository;
    private final EventPublisher eventPublisher;

    public RiskAnalysisService(WorkforceRiskDataExtractor extractor, RiskEngine engine,
            RiskImpactCalculator impactCalculator, RiskAdvisorService advisor, HeuristicRiskAdvisor heuristic,
            RiskAssessmentRepository assessmentRepository, RiskRecommendationRepository recommendationRepository,
            RiskAlertRepository alertRepository, EventPublisher eventPublisher) {
        this.extractor = extractor;
        this.engine = engine;
        this.impactCalculator = impactCalculator;
        this.advisor = advisor;
        this.heuristic = heuristic;
        this.assessmentRepository = assessmentRepository;
        this.recommendationRepository = recommendationRepository;
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
    }

    public synchronized WorkforceRiskSummary analyze() {
        WorkforceRiskData data = extractor.extract();
        List<RiskAssessment> fresh = engine.evaluate(data);
        RiskImpact impact = impactCalculator.compute(data, fresh);
        StructuredRisk structured = structuredRisk(data, fresh, impact);
        RiskAdvice advice = advisor.analyze(structured);
        persist(fresh, advice.recommendations());
        return buildSummary(data, fresh);
    }

    public WorkforceRiskSummary summary() {
        UUID tenantId = TenantScope.require();
        WorkforceRiskData data = extractor.extract();
        List<RiskAssessment> stored = assessmentRepository.findAllByOrganizationIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(RiskAssessmentEntity::toRecord)
                .toList();
        RiskImpact impact = impactCalculator.compute(data, stored);
        long openAlerts = alertRepository.countByOrganizationIdAndStatus(tenantId, RiskAlertStatus.OPEN);
        Map<RiskType, Long> byType = new LinkedHashMap<>();
        for (RiskType type : RiskType.values()) {
            long count = stored.stream().filter(assessment -> assessment.type() == type).count();
            if (count > 0) {
                byType.put(type, count);
            }
        }
        String analysis = heuristic.analyze(structuredRisk(data, stored, impact)).explanation();
        List<RiskAssessment> top = stored.stream().limit(TOP_ASSESSMENTS).toList();

        return new WorkforceRiskSummary(
                impact.riskIndex(),
                impact.coveragePercentage(),
                stored.size(),
                severityCount(stored, RiskSeverity.HIGH),
                severityCount(stored, RiskSeverity.MEDIUM),
                severityCount(stored, RiskSeverity.LOW),
                openAlerts,
                analysis,
                advisor.currentMode(),
                byType,
                top);
    }

    public List<RiskAssessment> assessments() {
        return assessmentRepository.findAllByOrganizationIdOrderByCreatedAtDesc(TenantScope.require()).stream()
                .map(RiskAssessmentEntity::toRecord)
                .toList();
    }

    public List<RiskRecommendation> recommendations(UUID assessmentId) {
        RiskAssessmentEntity assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk assessment not found"));
        TenantScope.assertAccess(assessment.organizationId());
        return recommendationRepository.findAllByAssessmentId(assessmentId).stream()
                .map(RiskRecommendationEntity::toRecord)
                .toList();
    }

    public List<RiskTrendPoint> trend() {
        UUID tenantId = TenantScope.require();
        Map<LocalDate, long[]> byDay = new TreeMap<>();
        for (RiskAssessmentEntity entity : assessmentRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(tenantId)) {
            LocalDate day = entity.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
            long[] counts = byDay.computeIfAbsent(day, ignored -> new long[4]);
            counts[0]++;
            switch (entity.severity()) {
                case HIGH -> counts[1]++;
                case MEDIUM -> counts[2]++;
                case LOW -> counts[3]++;
            }
        }
        return byDay.entrySet().stream()
                .map(entry -> new RiskTrendPoint(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2],
                        entry.getValue()[3]))
                .toList();
    }

    private void persist(List<RiskAssessment> fresh, List<RiskRecommendation> adviceRecommendations) {
        UUID tenantId = TenantScope.require();
        for (RiskAssessment assessment : fresh) {
            String key = dedupeKey(tenantId, assessment);
            if (assessmentRepository.findByDedupeKey(key).isPresent()) {
                continue;
            }
            RiskAssessmentEntity entity = new RiskAssessmentEntity(tenantId, assessment, key);
            assessmentRepository.save(entity);
            saveRecommendations(entity, tenantId, assessment, adviceRecommendations);
            publishAssessmentEvents(assessment, tenantId);
        }
    }

    private void saveRecommendations(RiskAssessmentEntity entity, UUID tenantId, RiskAssessment assessment,
            List<RiskRecommendation> adviceRecommendations) {
        for (RiskRecommendation recommendation : adviceRecommendations) {
            if (recommendation.riskType() == null || !recommendation.riskType().equals(assessment.type().name())) {
                continue;
            }
            recommendationRepository
                    .save(new RiskRecommendationEntity(UUID.randomUUID(), tenantId, entity.id(), recommendation));
        }
    }

    private void publishAssessmentEvents(RiskAssessment assessment, UUID tenantId) {
        Map<String, String> payload = java.util.Map.of(
                "assessmentId", assessment.id().toString(),
                "severity", assessment.severity().name(),
                "summary", assessment.summary());
        eventPublisher.publish(DomainEvents.of(EventTypes.COVERAGE_RISK_DETECTED, tenantId, "RiskAssessment",
                assessment.id(), null, Payloads.json(payload)));
        if (assessment.severity() == RiskSeverity.HIGH) {
            eventPublisher.publish(DomainEvents.of(EventTypes.WORKFORCE_RISK_ELEVATED, tenantId, "RiskAssessment",
                    assessment.id(), null, Payloads.json(payload)));
        }
    }

    private WorkforceRiskSummary buildSummary(WorkforceRiskData data, List<RiskAssessment> assessments) {
        RiskImpact impact = impactCalculator.compute(data, assessments);
        Map<RiskType, Long> byType = new LinkedHashMap<>();
        for (RiskType type : RiskType.values()) {
            long count = assessments.stream().filter(assessment -> assessment.type() == type).count();
            if (count > 0) {
                byType.put(type, count);
            }
        }
        return new WorkforceRiskSummary(
                impact.riskIndex(),
                impact.coveragePercentage(),
                assessments.size(),
                severityCount(assessments, RiskSeverity.HIGH),
                severityCount(assessments, RiskSeverity.MEDIUM),
                severityCount(assessments, RiskSeverity.LOW),
                alertRepository.countByOrganizationIdAndStatus(TenantScope.require(), RiskAlertStatus.OPEN),
                heuristic.analyze(structuredRisk(data, assessments, impact)).explanation(),
                advisor.currentMode(),
                byType,
                assessments.stream().limit(TOP_ASSESSMENTS).toList());
    }

    private StructuredRisk structuredRisk(WorkforceRiskData data, List<RiskAssessment> assessments, RiskImpact impact) {
        return new StructuredRisk(data.generatedAt(), activeCount(data), assignmentCount(data), impact,
                List.copyOf(assessments));
    }

    private long activeCount(WorkforceRiskData data) {
        return data.employees().stream().filter(WorkforceRiskData.EmployeeSnapshot::active).count();
    }

    private long assignmentCount(WorkforceRiskData data) {
        return data.assignments().stream().filter(WorkforceRiskData.AssignmentSnapshot::active).count();
    }

    private long severityCount(List<RiskAssessment> assessments, RiskSeverity severity) {
        return assessments.stream().filter(assessment -> assessment.severity() == severity).count();
    }

    private String dedupeKey(UUID tenantId, RiskAssessment assessment) {
        return tenantId + ":" + assessment.type().name() + ":" + assessment.entityId() + ":"
                + assessment.windowStart().toEpochMilli();
    }
}