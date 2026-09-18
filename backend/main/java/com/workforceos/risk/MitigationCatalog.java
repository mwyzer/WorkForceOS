package com.workforceos.risk;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MitigationCatalog {

    private final Map<RiskType, List<RiskRecommendation>> templates = Map.ofEntries(
            Map.entry(RiskType.COVERAGE_SHORTFALL, List.of(
                    new RiskRecommendation(RiskType.COVERAGE_SHORTFALL.name(), "Propose a shift swap",
                            "Find another employee to cover the exposed shift",
                            "CREATE_SHIFT_SWAP", "/api/v1/shift-swap-requests"),
                    new RiskRecommendation(RiskType.COVERAGE_SHORTFALL.name(), "Cover with overtime",
                            "Request overtime to cover the shift gap",
                            "CREATE_OVERTIME", "/api/v1/overtime-requests"))),
            Map.entry(RiskType.STAFFING_LIQUIDITY, List.of(
                    new RiskRecommendation(RiskType.STAFFING_LIQUIDITY.name(), "Cover with overtime",
                            "Request overtime to cover an understaffed team",
                            "CREATE_OVERTIME", "/api/v1/overtime-requests"),
                    new RiskRecommendation(RiskType.STAFFING_LIQUIDITY.name(), "Notify operations manager",
                            "Alert the manager about the staffing shortfall",
                            "NOTIFY_MANAGER", "/api/v1/notifications"))),
            Map.entry(RiskType.ATTENDANCE_TREND, List.of(
                    new RiskRecommendation(RiskType.ATTENDANCE_TREND.name(), "Review attendance",
                            "Validate and correct attendance records for this employee",
                            "REVIEW_ATTENDANCE", "/api/v1/attendance-corrections"),
                    new RiskRecommendation(RiskType.ATTENDANCE_TREND.name(), "Notify supervisor",
                            "Escalate the attendance pattern to the supervisor",
                            "NOTIFY_SUPERVISOR", "/api/v1/notifications"))),
            Map.entry(RiskType.OVERTIME_DEPENDENCY, List.of(
                    new RiskRecommendation(RiskType.OVERTIME_DEPENDENCY.name(), "Rebalance roster",
                            "Spread scheduled workload across more employees",
                            "REVIEW_ROSTER", "/api/v1/rosters"),
                    new RiskRecommendation(RiskType.OVERTIME_DEPENDENCY.name(), "Notify manager",
                            "Alert the manager about overtime dependency",
                            "NOTIFY_MANAGER", "/api/v1/notifications"))),
            Map.entry(RiskType.SINGLE_POINT_OF_FAILURE, List.of(
                    new RiskRecommendation(RiskType.SINGLE_POINT_OF_FAILURE.name(), "Propose a shift swap",
                            "Add a backup assignee for the single-person overtime shift",
                            "CREATE_SHIFT_SWAP", "/api/v1/shift-swap-requests"),
                    new RiskRecommendation(RiskType.SINGLE_POINT_OF_FAILURE.name(), "Review roster",
                            "Review the roster to add coverage for this shift",
                            "REVIEW_ROSTER", "/api/v1/rosters"))));

    public List<RiskRecommendation> forType(RiskType type) {
        return List.copyOf(templates.getOrDefault(type, List.of()));
    }

    public List<RiskRecommendation> forAssessments(List<RiskAssessment> assessments) {
        return assessments.stream()
                .map(assessment -> forType(assessment.type()))
                .flatMap(List::stream)
                .distinct()
                .toList();
    }
}