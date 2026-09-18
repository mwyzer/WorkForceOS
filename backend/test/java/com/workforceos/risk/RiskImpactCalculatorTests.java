package com.workforceos.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RiskImpactCalculatorTests {

    private final RiskImpactCalculator calculator = new RiskImpactCalculator(50);

    private final UUID departmentId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID rosterId = UUID.randomUUID();
    private final UUID shiftTemplateId = UUID.randomUUID();

    @Test
    void reportsFullCoverageWhenNoUncoveredAssignments() {
        WorkforceRiskData data = dataWithTotalAssignments(2);
        RiskImpact impact = calculator.compute(data, List.of());

        assertThat(impact.coveragePercentage()).isEqualTo(100.0);
        assertThat(impact.riskIndex()).isZero();
    }

    @Test
    void reducesCoverageForEachUncoveredAssignment() {
        WorkforceRiskData data = dataWithTotalAssignments(2);
        RiskAssessment uncovered = assessment(RiskType.COVERAGE_SHORTFALL, RiskSeverity.HIGH, 85, 480);

        RiskImpact impact = calculator.compute(data, List.of(uncovered));

        assertThat(impact.coveragePercentage()).isEqualTo(50.0);
        assertThat(impact.uncoveredHours()).isEqualTo(8.0);
        assertThat(impact.financialImpact()).contains("$400");
        assertThat(impact.complianceFlags()).contains("Service coverage below 90% target");
    }

    @Test
    void riskIndexWeightsHighSeverityAboveMedium() {
        RiskAssessment high = assessment(RiskType.COVERAGE_SHORTFALL, RiskSeverity.HIGH, 85, 0);
        RiskAssessment medium = assessment(RiskType.OVERTIME_DEPENDENCY, RiskSeverity.MEDIUM, 60, 0);

        int index = calculator.riskIndex(List.of(high, medium));

        assertThat(index).isEqualTo(76);
    }

    @Test
    void flagsSafetyCoverageForOvernightSpof() {
        RiskAssessment spof = assessment(RiskType.SINGLE_POINT_OF_FAILURE, RiskSeverity.MEDIUM, 65, 600);
        RiskImpact impact = calculator.compute(dataWithTotalAssignments(1), List.of(spof));

        assertThat(impact.complianceFlags()).contains("Safety coverage: single-person overnight shift");
    }

    private WorkforceRiskData dataWithTotalAssignments(int count) {
        List<WorkforceRiskData.AssignmentSnapshot> assignments = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            Instant start = Instant.now().plusSeconds(86_400 + index);
            assignments.add(new WorkforceRiskData.AssignmentSnapshot(UUID.randomUUID(), rosterId, employeeId,
                    shiftTemplateId, start, start.plusSeconds(28_800), true));
        }
        return new WorkforceRiskData(
                Instant.now(),
                List.of(new WorkforceRiskData.EmployeeSnapshot(employeeId, "EMP-001", teamId, departmentId, true)),
                java.util.Map.of(teamId, "Ops"),
                java.util.Map.of(departmentId, "Operations"),
                assignments,
                List.of(),
                List.of(),
                java.util.Set.of(),
                List.of(),
                java.util.Map.of());
    }

    private RiskAssessment assessment(RiskType type, RiskSeverity severity, int score, long impactMinutes) {
        Instant window = Instant.now();
        return new RiskAssessment(UUID.randomUUID(), type, severity, score, RiskEntityType.EMPLOYEE, employeeId,
                teamId, departmentId, window, window.plusSeconds(28_800), impactMinutes, "summary", List.of());
    }
}