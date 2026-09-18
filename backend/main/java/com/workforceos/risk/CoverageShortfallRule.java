package com.workforceos.risk;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.workforceos.risk.WorkforceRiskData.AssignmentSnapshot;
import com.workforceos.risk.WorkforceRiskData.EmployeeSnapshot;
import com.workforceos.risk.WorkforceRiskData.LeaveSnapshot;
import com.workforceos.risk.WorkforceRiskData.ShiftSnapshot;
import com.workforceos.risk.WorkforceRiskData.SwapSnapshot;

@Component
public class CoverageShortfallRule implements RiskRule {

    private static final int LEAVE_SCORE = 70;
    private static final int INACTIVE_SCORE = 95;
    private static final int NEAR_DAYS_PROXIMITY = 15;
    private static final int WARNING_DAYS_PROXIMITY = 10;

    @Override
    public RiskType type() {
        return RiskType.COVERAGE_SHORTFALL;
    }

    @Override
    public List<RiskAssessment> detect(WorkforceRiskData data) {
        Map<UUID, EmployeeSnapshot> employeesById = new HashMap<>();
        for (EmployeeSnapshot employee : data.employees()) {
            employeesById.put(employee.id(), employee);
        }
        Map<UUID, String> shiftNames = new HashMap<>();
        data.shifts().values().forEach(shift -> shiftNames.put(shift.id(), shift.name()));

        Set<String> swapCoverage = swapCoverage(data.approvedSwaps());

        List<RiskAssessment> assessments = new ArrayList<>();
        Instant now = Instant.now();
        for (AssignmentSnapshot assignment : data.assignments()) {
            if (!assignment.active()) {
                continue;
            }
            EmployeeSnapshot employee = employeesById.get(assignment.employeeId());
            if (employee == null) {
                continue;
            }

            String reason;
            boolean inactive = !employee.active();
            if (inactive) {
                reason = "employee is no longer active";
            } else if (hasApprovedLeave(data.approvedLeaves(), assignment)) {
                reason = "approved leave overlaps the assigned shift";
            } else {
                continue;
            }

            if (swapCoverage.contains(swapKey(employee.id(), assignment.start().atZone(ZoneOffset.UTC).toLocalDate()))) {
                continue;
            }

            int proximity = proximityBonus(now, assignment.start());
            int score = RiskScoring.cap(inactive ? INACTIVE_SCORE + proximity : LEAVE_SCORE + proximity);
            long impactMinutes = Math.max(0, Duration.between(assignment.start(), assignment.end()).toMinutes());

            String shiftName = shiftNames.getOrDefault(assignment.shiftTemplateId(), "assigned shift");
            String period = assignment.start() + " to " + assignment.end();
            String evidence = "Employee " + employee.employeeNumber() + " on " + shiftName + " (" + period + "); "
                    + reason;

            assessments.add(new RiskAssessment(
                    UUID.randomUUID(),
                    type(),
                    RiskScoring.severity(score),
                    score,
                    RiskEntityType.EMPLOYEE,
                    employee.id(),
                    employee.teamId(),
                    employee.departmentId(),
                    assignment.start(),
                    assignment.end(),
                    impactMinutes,
                    "Uncovered " + shiftName + " for " + employee.employeeNumber() + " because " + reason,
                    List.of(evidence, "Reason: " + reason)));
        }
        return assessments;
    }

    private boolean hasApprovedLeave(List<LeaveSnapshot> leaves, AssignmentSnapshot assignment) {
        LocalDate start = assignment.start().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = assignment.end().atZone(ZoneOffset.UTC).toLocalDate();
        for (LeaveSnapshot leave : leaves) {
            if (!leave.employeeId().equals(assignment.employeeId())) {
                continue;
            }
            boolean overlaps = !leave.startDate().isAfter(end) && !leave.endDate().isBefore(start);
            if (overlaps) {
                return true;
            }
        }
        return false;
    }

    private int proximityBonus(Instant now, Instant start) {
        Duration until = Duration.between(now, start);
        if (until.isNegative() || until.toDays() <= 3) {
            return NEAR_DAYS_PROXIMITY;
        }
        if (until.toDays() <= 7) {
            return WARNING_DAYS_PROXIMITY;
        }
        return 0;
    }

    private Set<String> swapCoverage(Collection<SwapSnapshot> swaps) {
        Set<String> coverage = new HashSet<>();
        for (SwapSnapshot swap : swaps) {
            coverage.add(swapKey(swap.requestingEmployeeId(), swap.requestedDate()));
            coverage.add(swapKey(swap.targetEmployeeId(), swap.offeredDate()));
        }
        return coverage;
    }

    private String swapKey(UUID employeeId, LocalDate date) {
        return employeeId + ":" + date;
    }
}