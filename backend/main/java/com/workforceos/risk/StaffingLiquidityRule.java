package com.workforceos.risk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workforceos.risk.WorkforceRiskData.AssignmentSnapshot;
import com.workforceos.risk.WorkforceRiskData.EmployeeSnapshot;

@Component
public class StaffingLiquidityRule implements RiskRule {

    private final int liquidityDays;

    public StaffingLiquidityRule(@Value("${workforce.risk.liquidity-days:14}") int liquidityDays) {
        this.liquidityDays = liquidityDays;
    }

    @Override
    public RiskType type() {
        return RiskType.STAFFING_LIQUIDITY;
    }

    @Override
    public List<RiskAssessment> detect(WorkforceRiskData data) {
        Map<UUID, List<EmployeeSnapshot>> employeesByTeam = new HashMap<>();
        for (EmployeeSnapshot employee : data.employees()) {
            if (!employee.active()) {
                continue;
            }
            employeesByTeam.computeIfAbsent(employee.teamId(), ignored -> new ArrayList<>()).add(employee);
        }

        Instant now = Instant.now();
        Instant windowStart = RiskScoring.weekBucket(now);
        Instant horizon = windowStart.plusSeconds(liquidityDays * 24L * 60L * 60L);

        Map<UUID, List<AssignmentSnapshot>> demandByTeam = new HashMap<>();
        for (AssignmentSnapshot assignment : data.assignments()) {
            if (!assignment.active() || assignment.start().isAfter(horizon) || assignment.end() == null) {
                continue;
            }
            EmployeeSnapshot employee = findEmployee(data.employees(), assignment.employeeId());
            if (employee == null) {
                continue;
            }
            demandByTeam.computeIfAbsent(employee.teamId(), ignored -> new ArrayList<>()).add(assignment);
        }

        List<RiskAssessment> assessments = new ArrayList<>();
        for (Map.Entry<UUID, List<AssignmentSnapshot>> entry : demandByTeam.entrySet()) {
            UUID teamId = entry.getKey();
            int demand = entry.getValue().size();
            List<EmployeeSnapshot> staff = employeesByTeam.getOrDefault(teamId, List.of());
            int capacity = staff.size();

            if (capacity >= demand) {
                continue;
            }

            int shortage = demand - capacity;
            double shortfallRatio = shortage / (double) demand;
            int score = RiskScoring.cap((int) Math.round(shortfallRatio * 100) + (shortfallRatio > 0.5 ? 20 : 10));
            UUID departmentId = firstDepartmentId(staff);

            String teamName = data.teamNames().getOrDefault(teamId, "Team");
            assessments.add(new RiskAssessment(
                    UUID.randomUUID(),
                    type(),
                    RiskScoring.severity(score),
                    score,
                    RiskEntityType.TEAM,
                    teamId,
                    teamId,
                    departmentId,
                    windowStart,
                    horizon,
                    0,
                    "Team " + teamName + " has " + capacity + " active staff for " + demand
                            + " scheduled slots over the next " + liquidityDays + " days",
                    List.of("Active staff: " + capacity, "Scheduled slots: " + demand,
                            "Shortfall: " + shortage)));
        }
        return assessments;
    }

    private UUID firstDepartmentId(List<EmployeeSnapshot> staff) {
        return staff.isEmpty() ? null : staff.get(0).departmentId();
    }

    private EmployeeSnapshot findEmployee(List<EmployeeSnapshot> employees, UUID employeeId) {
        for (EmployeeSnapshot employee : employees) {
            if (employee.id().equals(employeeId)) {
                return employee;
            }
        }
        return null;
    }
}