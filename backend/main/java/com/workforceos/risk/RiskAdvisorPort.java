package com.workforceos.risk;

public interface RiskAdvisorPort {

    RiskAdvice analyze(StructuredRisk structured);

    String mode();
}