package com.workforceos.risk;

import java.util.List;

public record RiskImpact(
        int riskIndex,
        double coveragePercentage,
        double uncoveredHours,
        String financialImpact,
        String operationalImpact,
        List<String> complianceFlags) {
}