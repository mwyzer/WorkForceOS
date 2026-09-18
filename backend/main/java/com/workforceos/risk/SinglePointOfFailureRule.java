package com.workforceos.risk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.workforceos.risk.WorkforceRiskData.AssignmentSnapshot;
import com.workforceos.risk.WorkforceRiskData.EmployeeSnapshot;
import com.workforceos.risk.WorkforceRiskData.ShiftSnapshot;

@Component
public class SinglePointOfFailureRule implements RiskRule {

    private static final int SPOF_SCORE = 65;

    @Override
    public RiskType type() {
        return RiskType.SINGLE_POINT_OF_FAILURE;
    }

    @Override
    public List<RiskAssessment> detect(WorkforceRiskData data) {
        Map<UUID, EmployeeSnapshot> employeesById = new HashMap<>();
        for (EmployeeSnapshot employee : data.employees()) {
            employeesById.put(employee.id(), employee);
        }
        Map<UUID, ShiftSnapshot> shiftsById = data.shifts();

        List<AssignmentSnapshot> overnightAssignments = data.assignments().stream()
                .filter(assignment -> assignment.active())
                .filter(assignment -> {
                    ShiftSnapshot shift = shiftsById.get(assignment.shiftTemplateId());
                    return shift != null && shift.overnight();
                })
                .toList();

        List<RiskAssessment> assessments = new ArrayList<>();
        for (AssignmentSnapshot assignment : overnightAssignments) {
            boolean hasBackup = overnightAssignments.stream()
                    .anyMatch(other -> !other.employeeId().equals(assignment.employeeId())
                            && assignment.start().isBefore(other.end())
                            && other.start().isBefore(assignment.end()));
            if (hasBackup) {
                continue;
            }

            EmployeeSnapshot employee = employeesById.get(assignment.employeeId());
            if (employee == null) {
                continue;
            }
            ShiftSnapshot shift = shiftsById.get(assignment.shiftTemplateId());
            long impactMinutes = Math.max(0, Duration.between(assignment.start(), assignment.end()).toMinutes());

            assessments.add(new RiskAssessment(
                    UUID.randomUUID(),
                    type(),
                    RiskScoring.severity(SPOF_SCORE),
                    SPOF_SCORE,
                    RiskEntityType.ROSTER,
                    assignment.rosterId(),
                    employee.teamId(),
                    employee.departmentId(),
                    assignment.start(),
                    assignment.end(),
                    impactMinutes,
                    "Overnight shift " + shift.name() + " on " + assignment.start() + " has no backup assignee",
                    List.of("Shift: " + shift.name(),
                            "Single assignee: " + employee.employeeNumber())));
        }
        return assessments;
    }
}