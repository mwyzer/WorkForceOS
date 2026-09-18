package com.workforceos.risk;

import java.util.List;

public record RiskAdvice(
        String explanation,
        RiskImpact impact,
        List<RiskRecommendation> recommendations,
        String advisorMode) {
}