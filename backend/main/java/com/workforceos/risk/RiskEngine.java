package com.workforceos.risk;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RiskEngine {

    private final List<RiskRule> rules;

    public RiskEngine(List<RiskRule> rules) {
        this.rules = rules;
    }

    public List<RiskAssessment> evaluate(WorkforceRiskData data) {
        return rules.stream()
                .flatMap(rule -> rule.detect(data).stream())
                .toList();
    }
}