package com.workforceos.risk;

public record RiskRecommendation(
        String riskType,
        String title,
        String description,
        String actionType,
        String actionEndpoint) {
}