package com.workforceos.risk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RiskAdvisorService implements RiskAdvisorPort {

    private static final Logger LOG = LoggerFactory.getLogger(RiskAdvisorService.class);

    private final HeuristicRiskAdvisor heuristic;
    private final LlmRiskAdvisor llm;
    private final MitigationCatalog catalog;

    public RiskAdvisorService(HeuristicRiskAdvisor heuristic, LlmRiskAdvisor llm, MitigationCatalog catalog) {
        this.heuristic = heuristic;
        this.llm = llm;
        this.catalog = catalog;
    }

    @Override
    public RiskAdvice analyze(StructuredRisk structured) {
        RiskAdvice advice;
        if (llm.available()) {
            try {
                advice = llm.analyze(structured);
            } catch (RuntimeException ex) {
                LOG.warn("LLM risk advisor failed, falling back to heuristic analysis", ex);
                advice = heuristic.analyze(structured);
            }
        } else {
            advice = heuristic.analyze(structured);
        }
        return mergeCatalogLinks(advice, structured.assessments());
    }

    @Override
    public String mode() {
        return llm.available() ? "llm" : "heuristic";
    }

    public String currentMode() {
        return mode();
    }

    private RiskAdvice mergeCatalogLinks(RiskAdvice advice, List<RiskAssessment> assessments) {
        List<RiskRecommendation> merged = new ArrayList<>(advice.recommendations());
        Set<String> present = new HashSet<>();
        merged.forEach(recommendation -> present.add(recommendation.actionType()));

        for (RiskAssessment assessment : assessments) {
            for (RiskRecommendation recommendation : catalog.forType(assessment.type())) {
                if (present.add(recommendation.actionType())) {
                    merged.add(recommendation);
                }
            }
        }
        return new RiskAdvice(advice.explanation(), advice.impact(), merged, advice.advisorMode());
    }
}