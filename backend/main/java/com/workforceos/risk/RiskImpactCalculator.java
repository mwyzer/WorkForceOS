package com.workforceos.risk;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RiskImpactCalculator {

    private final double laborRatePerHour;

    public RiskImpactCalculator(@Value("${workforce.risk.labor-rate-per-hour:50}") double laborRatePerHour) {
        this.laborRatePerHour = laborRatePerHour;
    }

    public RiskImpact compute(WorkforceRiskData data, List<RiskAssessment> assessments) {
        long totalAssignments = data.assignments().stream()
                .filter(WorkforceRiskData.AssignmentSnapshot::active)
                .count();
        long uncoveredAssignments = assessments.stream()
                .filter(assessment -> assessment.type() == RiskType.COVERAGE_SHORTFALL)
                .count();

        double coveragePercentage = totalAssignments == 0
                ? 100.0
                : Math.max(0, (1.0 - uncoveredAssignments / (double) totalAssignments) * 100);

        double uncoveredHours = assessments.stream()
                .filter(assessment -> assessment.type() == RiskType.COVERAGE_SHORTFALL)
                .mapToLong(RiskAssessment::impactMinutes)
                .sum() / 60.0;

        int riskIndex = riskIndex(assessments);

        String financialImpact = String.format("$%.0f estimated uncovered labor cost (%.1f hours at $%.0f/hour)",
                uncoveredHours * laborRatePerHour, uncoveredHours, laborRatePerHour);
        String operationalImpact = String.format("%.0f%% scheduled slot coverage with %d active risk(s)",
                coveragePercentage, assessments.size());

        List<String> complianceFlags = new ArrayList<>();
        boolean overnightSpof = assessments.stream()
                .anyMatch(assessment -> assessment.type() == RiskType.SINGLE_POINT_OF_FAILURE);
        if (overnightSpof) {
            complianceFlags.add("Safety coverage: single-person overnight shift");
        }
        boolean attendanceIssue = assessments.stream()
                .anyMatch(assessment -> assessment.type() == RiskType.ATTENDANCE_TREND
                        && assessment.severity() == RiskSeverity.HIGH);
        if (attendanceIssue) {
            complianceFlags.add("Absenteeism watch: elevated late or early-departure patterns");
        }
        if (coveragePercentage < 90) {
            complianceFlags.add("Service coverage below 90% target");
        }

        return new RiskImpact(riskIndex, coveragePercentage, uncoveredHours, financialImpact, operationalImpact,
                complianceFlags);
    }

    public int riskIndex(List<RiskAssessment> assessments) {
        if (assessments.isEmpty()) {
            return 0;
        }
        double weightedScore = 0;
        double totalWeight = 0;
        for (RiskAssessment assessment : assessments) {
            double weight = switch (assessment.severity()) {
                case HIGH -> 1.0;
                case MEDIUM -> 0.6;
                case LOW -> 0.3;
            };
            weightedScore += assessment.score() * weight;
            totalWeight += weight;
        }
        return (int) Math.round(weightedScore / totalWeight);
    }
}