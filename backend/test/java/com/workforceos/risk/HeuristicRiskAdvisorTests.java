package com.workforceos.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HeuristicRiskAdvisorTests {

    private final HeuristicRiskAdvisor advisor = new HeuristicRiskAdvisor(new MitigationCatalog());

    @Test
    void explainsRiskIndexAndDrivers() {
        RiskAssessment assessment = assessment(RiskType.COVERAGE_SHORTFALL, RiskSeverity.HIGH, 85, 480);
        StructuredRisk structured = structured(List.of(assessment));

        RiskAdvice advice = advisor.analyze(structured);

        assertThat(advice.explanation()).contains("risk index is 85");
        assertThat(advice.explanation()).contains("coverage shortfall");
        assertThat(advice.advisorMode()).isEqualTo("heuristic");
    }

    @Test
    void providesCatalogRecommendationsForDetectedRiskTypes() {
        StructuredRisk structured = structured(List.of(
                assessment(RiskType.COVERAGE_SHORTFALL, RiskSeverity.HIGH, 85, 480),
                assessment(RiskType.STAFFING_LIQUIDITY, RiskSeverity.MEDIUM, 60, 0)));

        RiskAdvice advice = advisor.analyze(structured);

        assertThat(advice.recommendations())
                .extracting(RiskRecommendation::actionType)
                .contains("CREATE_SHIFT_SWAP", "CREATE_OVERTIME", "NOTIFY_MANAGER");
    }

    @Test
    void reportsNoMaterialRiskWhenAssessmentsEmpty() {
        RiskAdvice advice = advisor.analyze(structured(List.of()));

        assertThat(advice.explanation()).contains("no material risk identified");
        assertThat(advice.recommendations()).isEmpty();
    }

    private StructuredRisk structured(List<RiskAssessment> assessments) {
        RiskImpact impact = new RiskImpact(85, 50.0, 8.0, "$400 estimated uncovered labor cost",
                "50% scheduled slot coverage with 1 active risk(s)", List.of());
        return new StructuredRisk(Instant.now(), 10, 100, impact, assessments);
    }

    private RiskAssessment assessment(RiskType type, RiskSeverity severity, int score, long impactMinutes) {
        UUID id = UUID.randomUUID();
        Instant window = Instant.now();
        return new RiskAssessment(id, type, severity, score, RiskEntityType.EMPLOYEE, id, UUID.randomUUID(),
                UUID.randomUUID(), window, window.plusSeconds(28_800), impactMinutes, "summary", List.of());
    }
}