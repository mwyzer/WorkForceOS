package com.workforceos.risk;

import java.util.List;
import java.util.Map;

public record WorkforceRiskSummary(
        int riskIndex,
        double coveragePercentage,
        long totalAssessments,
        long highCount,
        long mediumCount,
        long lowCount,
        long openAlerts,
        String latestAnalysis,
        String advisorMode,
        Map<RiskType, Long> risksByType,
        List<RiskAssessment> topAssessments) {
}