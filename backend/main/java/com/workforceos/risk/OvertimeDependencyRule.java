package com.workforceos.risk;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workforceos.risk.WorkforceRiskData.EmployeeSnapshot;
import com.workforceos.risk.WorkforceRiskData.OvertimeSnapshot;

@Component
public class OvertimeDependencyRule implements RiskRule {

    private final int windowDays;
    private final double highHours;
    private final double mediumHours;

    public OvertimeDependencyRule(@Value("${workforce.risk.overtime-window-days:30}") int windowDays,
            @Value("${workforce.risk.overtime-high-hours:20}") double highHours,
            @Value("${workforce.risk.overtime-medium-hours:10}") double mediumHours) {
        this.windowDays = windowDays;
        this.highHours = highHours;
        this.mediumHours = mediumHours;
    }

    @Override
    public RiskType type() {
        return RiskType.OVERTIME_DEPENDENCY;
    }

    @Override
    public List<RiskAssessment> detect(WorkforceRiskData data) {
        Map<UUID, EmployeeSnapshot> employeesById = new HashMap<>();
        for (EmployeeSnapshot employee : data.employees()) {
            if (employee.active()) {
                employeesById.put(employee.id(), employee);
            }
        }

        LocalDate lower = LocalDate.now().minusDays(windowDays);
        Map<UUID, Double> hoursByEmployee = new HashMap<>();
        for (OvertimeSnapshot overtime : data.approvedOvertime()) {
            if (overtime.date().isBefore(lower)) {
                continue;
            }
            hoursByEmployee.merge(overtime.employeeId(), overtime.hours(), Double::sum);
        }

        List<RiskAssessment> assessments = new ArrayList<>();
        Instant windowEnd = data.generatedAt();
        Instant windowStart = RiskScoring.monthBucket(windowEnd);

        for (Map.Entry<UUID, Double> entry : hoursByEmployee.entrySet()) {
            if (entry.getValue() < mediumHours) {
                continue;
            }
            EmployeeSnapshot employee = employeesById.get(entry.getKey());
            if (employee == null) {
                continue;
            }
            double hours = entry.getValue();
            int score = hours >= highHours ? 95 : hours >= 15 ? 80 : 60;
            assessments.add(new RiskAssessment(
                    UUID.randomUUID(),
                    type(),
                    RiskScoring.severity(score),
                    score,
                    RiskEntityType.EMPLOYEE,
                    employee.id(),
                    employee.teamId(),
                    employee.departmentId(),
                    windowStart,
                    windowEnd,
                    Math.round(hours * 60),
                    "Employee " + employee.employeeNumber() + " is approved for " + hours
                            + " overtime hours over the last " + windowDays + " days",
                    List.of("Approved overtime hours: " + hours)));
        }
        return assessments;
    }
}