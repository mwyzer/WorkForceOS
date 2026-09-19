package com.workforceos.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.workforceos.risk.WorkforceRiskData.AssignmentSnapshot;
import com.workforceos.risk.WorkforceRiskData.AttendanceSnapshot;
import com.workforceos.risk.WorkforceRiskData.EmployeeSnapshot;
import com.workforceos.risk.WorkforceRiskData.LeaveSnapshot;
import com.workforceos.risk.WorkforceRiskData.OvertimeSnapshot;
import com.workforceos.risk.WorkforceRiskData.ShiftSnapshot;
import com.workforceos.risk.WorkforceRiskData.SwapSnapshot;

class RiskEngineTests {

    private final UUID departmentId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID rosterId = UUID.randomUUID();
    private final UUID shiftTemplateId = UUID.randomUUID();
    private final UUID otherEmployeeId = UUID.randomUUID();

    @Test
    void detectsCoverageShortfallWhenLeaveOverlapsPublishedShift() {
        Instant start = Instant.now().plusSeconds(86_400);
        WorkforceRiskData data = base(staff(1), assignments(start), List.of(new LeaveSnapshot(employeeId,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(1))));

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments).extracting(RiskAssessment::type)
                .contains(RiskType.COVERAGE_SHORTFALL);
        RiskAssessment assessment = assessments.stream()
                .filter(candidate -> candidate.type() == RiskType.COVERAGE_SHORTFALL)
                .findFirst().orElseThrow();
        assertThat(assessment.entityId()).isEqualTo(employeeId);
        assertThat(assessment.severity()).isEqualTo(RiskSeverity.HIGH);
        assertThat(assessment.impactMinutes()).isGreaterThan(0);
    }

    @Test
    void skipsCoverageShortfallWhenApprovedSwapCoversShift() {
        Instant start = Instant.now().plusSeconds(86_400);
        LocalDate tomorrow = start.atZone(ZoneOffset.UTC).toLocalDate();
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                staff(1),
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                assignments(start),
                List.of(new LeaveSnapshot(employeeId, tomorrow, tomorrow)),
                List.of(),
                Set.of(new SwapSnapshot(employeeId, otherEmployeeId,
                        tomorrow, tomorrow)),
                List.of(),
                Map.of(shiftTemplateId, new ShiftSnapshot(shiftTemplateId, "Day", false)));

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments).extracting(RiskAssessment::type)
                .doesNotContain(RiskType.COVERAGE_SHORTFALL);
    }

    @Test
    void detectsAttendanceTrendWhenIssueRatioExceedsThreshold() {
        LocalDate today = LocalDate.now();
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                staff(1),
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                List.of(),
                List.of(),
                List.of(new OvertimeSnapshot(employeeId, today, 0)),
                Set.of(),
                List.of(new AttendanceSnapshot(employeeId, 4, 2, 0, 0)),
                Map.of());

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments)
                .extracting(RiskAssessment::type)
                .contains(RiskType.ATTENDANCE_TREND);
        RiskAssessment assessment = assessments.stream()
                .filter(candidate -> candidate.type() == RiskType.ATTENDANCE_TREND)
                .findFirst().orElseThrow();
        assertThat(assessment.score()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void skipsAttendanceTrendWhenSessionsBelowMinimum() {
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                staff(1),
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                List.of(new AttendanceSnapshot(employeeId, 2, 2, 0, 0)),
                Map.of());

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments).extracting(RiskAssessment::type)
                .doesNotContain(RiskType.ATTENDANCE_TREND);
    }

    @Test
    void detectsOvertimeDependencyAboveMediumThreshold() {
        LocalDate today = LocalDate.now();
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                staff(1),
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                List.of(),
                List.of(),
                List.of(new OvertimeSnapshot(employeeId, today, 22)),
                Set.of(),
                List.of(),
                Map.of());

        List<RiskAssessment> assessments = engine().evaluate(data);

        RiskAssessment assessment = assessments.stream()
                .filter(candidate -> candidate.type() == RiskType.OVERTIME_DEPENDENCY)
                .findFirst().orElseThrow();
        assertThat(assessment.severity()).isEqualTo(RiskSeverity.HIGH);
    }

    @Test
    void detectsStaffingLiquidityShortfallForTeam() {
        Instant start = Instant.now().plusSeconds(86_400);
        List<AssignmentSnapshot> assignments = List.of(
                assignment(start),
                assignment(start.plusSeconds(3_600)));
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                staff(1),
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                assignments,
                List.of(),
                List.of(),
                Set.of(),
                List.of(),
                Map.of(shiftTemplateId, new ShiftSnapshot(shiftTemplateId, "Day", false)));

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments).extracting(RiskAssessment::type)
                .contains(RiskType.STAFFING_LIQUIDITY);
        RiskAssessment assessment = assessments.stream()
                .filter(candidate -> candidate.type() == RiskType.STAFFING_LIQUIDITY)
                .findFirst().orElseThrow();
        assertThat(assessment.entityType()).isEqualTo(RiskEntityType.TEAM);
        assertThat(assessment.entityId()).isEqualTo(teamId);
    }

    @Test
    void detectsSinglePointOfFailureForOvernightShift() {
        Instant start = Instant.now().plusSeconds(86_400);
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                staff(1),
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                assignments(start),
                List.of(),
                List.of(),
                Set.of(),
                List.of(),
                Map.of(shiftTemplateId, new ShiftSnapshot(shiftTemplateId, "Night", true)));

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments).extracting(RiskAssessment::type)
                .contains(RiskType.SINGLE_POINT_OF_FAILURE);
        RiskAssessment assessment = assessments.stream()
                .filter(candidate -> candidate.type() == RiskType.SINGLE_POINT_OF_FAILURE)
                .findFirst().orElseThrow();
        assertThat(assessment.entityType()).isEqualTo(RiskEntityType.ROSTER);
    }

    @Test
    void producesNoAssessmentsForHealthyWorkforce() {
        Instant start = Instant.now().plusSeconds(86_400);
        List<EmployeeSnapshot> twoStaff = List.of(
                employee(employeeId, 1),
                employee(otherEmployeeId, 2));
        List<AssignmentSnapshot> assignments = List.of(assignment(start));
        WorkforceRiskData data = new WorkforceRiskData(
                Instant.now(),
                twoStaff,
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                assignments,
                List.of(),
                List.of(),
                Set.of(),
                List.of(),
                Map.of(shiftTemplateId, new ShiftSnapshot(shiftTemplateId, "Day", false)));

        List<RiskAssessment> assessments = engine().evaluate(data);

        assertThat(assessments).isEmpty();
    }

    private RiskEngine engine() {
        return new RiskEngine(List.of(
                new CoverageShortfallRule(),
                new StaffingLiquidityRule(14),
                new AttendanceTrendRule(4, 0.25),
                new OvertimeDependencyRule(30, 20, 10),
                new SinglePointOfFailureRule()));
    }

    private WorkforceRiskData base(List<EmployeeSnapshot> staff, List<AssignmentSnapshot> assignments,
            List<LeaveSnapshot> leaves) {
        return new WorkforceRiskData(
                Instant.now(),
                staff,
                Map.of(teamId, "Ops"),
                Map.of(departmentId, "Operations"),
                assignments,
                leaves,
                List.of(),
                Set.of(),
                List.of(),
                Map.of(shiftTemplateId, new ShiftSnapshot(shiftTemplateId, "Day", false)));
    }

    private List<EmployeeSnapshot> staff(int count) {
        List<EmployeeSnapshot> staff = new java.util.ArrayList<>();
        for (int index = 1; index <= count; index++) {
            staff.add(employee(index == 1 ? employeeId : UUID.randomUUID(), index));
        }
        return staff;
    }

    private EmployeeSnapshot employee(UUID id, int number) {
        return new EmployeeSnapshot(id, "EMP-" + String.format("%03d", number), teamId, departmentId, true);
    }

    private List<AssignmentSnapshot> assignments(Instant start) {
        return List.of(assignment(start));
    }

    private AssignmentSnapshot assignment(Instant start) {
        return new AssignmentSnapshot(UUID.randomUUID(), rosterId, employeeId, shiftTemplateId, start,
                start.plusSeconds(28_800), true);
    }
}