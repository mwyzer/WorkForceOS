package com.workforceos.risk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workforceos.risk.WorkforceRiskData.AttendanceSnapshot;
import com.workforceos.risk.WorkforceRiskData.EmployeeSnapshot;

@Component
public class AttendanceTrendRule implements RiskRule {

    private final int minSessions;
    private final double issueThreshold;

    public AttendanceTrendRule(@Value("${workforce.risk.attendance-min-sessions:4}") int minSessions,
            @Value("${workforce.risk.attendance-issue-threshold:0.25}") double issueThreshold) {
        this.minSessions = minSessions;
        this.issueThreshold = issueThreshold;
    }

    @Override
    public RiskType type() {
        return RiskType.ATTENDANCE_TREND;
    }

    @Override
    public List<RiskAssessment> detect(WorkforceRiskData data) {
        Map<UUID, EmployeeSnapshot> employeesById = new HashMap<>();
        for (EmployeeSnapshot employee : data.employees()) {
            if (employee.active()) {
                employeesById.put(employee.id(), employee);
            }
        }

        List<RiskAssessment> assessments = new ArrayList<>();
        Instant windowEnd = data.generatedAt();
        Instant windowStart = RiskScoring.monthBucket(windowEnd);

        for (AttendanceSnapshot snapshot : data.attendance()) {
            EmployeeSnapshot employee = employeesById.get(snapshot.employeeId());
            if (employee == null || snapshot.sessionCount() < minSessions) {
                continue;
            }
            long issues = snapshot.lateCount() + snapshot.earlyLeaveCount();
            double ratio = issues / (double) snapshot.sessionCount();
            if (ratio < issueThreshold) {
                continue;
            }

            int score = RiskScoring.cap((int) Math.round(ratio * 100) + 10);
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
                    0,
                    "Employee " + employee.employeeNumber() + " has " + issues + " late or early-departure events in "
                            + snapshot.sessionCount() + " sessions",
                    List.of("Late events: " + snapshot.lateCount(),
                            "Early departure events: " + snapshot.earlyLeaveCount(),
                            "Sessions: " + snapshot.sessionCount())));
        }
        return assessments;
    }
}