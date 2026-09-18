package com.workforceos.risk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class HeuristicRiskAdvisor implements RiskAdvisorPort {

    private final MitigationCatalog catalog;

    public HeuristicRiskAdvisor(MitigationCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public RiskAdvice analyze(StructuredRisk structured) {
        RiskImpact impact = structured.impact();

        Map<RiskType, List<RiskAssessment>> byType = new EnumMap<>(RiskType.class);
        structured.assessments().forEach(assessment ->
                byType.computeIfAbsent(assessment.type(), ignored -> new ArrayList<>()).add(assessment));

        StringBuilder explanation = new StringBuilder();
        explanation.append("Workforce risk index is ").append(impact.riskIndex())
                .append(" out of 100 with ").append(String.format("%.0f%%", impact.coveragePercentage()))
                .append(" scheduled slot coverage across ").append(structured.activeEmployees())
                .append(" active employees and ").append(structured.publishedAssignments())
                .append(" published assignments. Risk drivers: ");
        if (byType.isEmpty()) {
            explanation.append("no material risk identified.");
        } else {
            List<String> drivers = new ArrayList<>();
            for (Map.Entry<RiskType, List<RiskAssessment>> entry : byType.entrySet()) {
                drivers.add(entry.getKey().name().replace('_', ' ').toLowerCase() + " (" + entry.getValue().size() + ")");
            }
            explanation.append(String.join(", ", drivers)).append('.');
        }

        List<RiskRecommendation> recommendations = catalog.forAssessments(structured.assessments());
        return new RiskAdvice(explanation.toString(), impact, recommendations, mode());
    }

    @Override
    public String mode() {
        return "heuristic";
    }
}