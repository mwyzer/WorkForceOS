package com.workforceos.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.workforceos.attendance.AttendanceCalculation;
import com.workforceos.attendance.AttendanceEngine;
import com.workforceos.attendance.AttendanceSession;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;

class AbsenteeismAnalyticsServiceTests {

    private final EmployeeService employeeService = mock(EmployeeService.class);
    private final ScheduleEngine scheduleEngine = mock(ScheduleEngine.class);
    private final AttendanceEngine attendanceEngine = mock(AttendanceEngine.class);
    private final LeaveRequestService leaveRequestService = mock(LeaveRequestService.class);
    private final AbsenteeismAnalyticsService service = new AbsenteeismAnalyticsService(
            employeeService, scheduleEngine, attendanceEngine, leaveRequestService, 30);

    @Test
    void scoresEmployeeWithAbsenceAndWorseningTrend() {
        UUID employeeId = UUID.randomUUID();
        UUID rosterId = UUID.randomUUID();
        UUID firstShift = UUID.randomUUID();
        UUID secondShift = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime firstEnd = now.minusDays(20);
        OffsetDateTime secondEnd = now.minusDays(5);

        when(employeeService.findAll()).thenReturn(List.of(
                new Employee(employeeId, "EMP-1", "Ada", "Lovelace", "ada@workforceos.io", null, null, true)));
        RosterAssignment first = new RosterAssignment(UUID.randomUUID(), rosterId, employeeId, firstShift,
                firstEnd.minusHours(8), firstEnd, true);
        RosterAssignment second = new RosterAssignment(UUID.randomUUID(), rosterId, employeeId, secondShift,
                secondEnd.minusHours(8), secondEnd, true);
        when(scheduleEngine.findAllAssignments()).thenReturn(List.of(first, second));

        AttendanceSession session = new AttendanceSession(UUID.randomUUID(), employeeId, rosterId, firstShift,
                firstEnd.minusHours(8).plusMinutes(20), firstEnd, false);
        when(attendanceEngine.calculateAll()).thenReturn(List.of(
                new AttendanceCalculation(session, first, 460, true, false, 0)));
        when(leaveRequestService.findAll()).thenReturn(List.of());

        AbsenteeismReport report = service.report(30);

        assertEquals(30, report.windowDays());
        assertEquals(1, report.insights().size());
        assertEquals(1, report.highRisk());

        AbsenteeismInsight insight = report.insights().get(0);
        assertEquals(employeeId, insight.employeeId());
        assertEquals(2, insight.scheduledShifts());
        assertEquals(1, insight.absentShifts());
        assertEquals(1, insight.lateCount());
        assertEquals(0.5, insight.absenceRate());
        assertEquals("INCREASING", insight.trend());
        assertEquals("HIGH", insight.riskLevel());
        assertTrue(insight.drivers().contains("Absence rate 50%"));
        assertTrue(insight.drivers().contains("Worsening absence trend"));
    }

    @Test
    void surfacesPlannedLeaveEvenWithoutAssignments() {
        UUID employeeId = UUID.randomUUID();
        when(employeeService.findAll()).thenReturn(List.of(
                new Employee(employeeId, "EMP-2", "Grace", "Hopper", "grace@workforceos.io", null, null, true)));
        when(scheduleEngine.findAllAssignments()).thenReturn(List.of());
        when(attendanceEngine.calculateAll()).thenReturn(List.of());
        when(leaveRequestService.findAll()).thenReturn(List.of(
                new LeaveRequest(UUID.randomUUID(), employeeId, LocalDate.now().minusDays(3),
                        LocalDate.now().minusDays(1), "Rest", LeaveRequestStatus.APPROVED)));

        AbsenteeismReport report = service.report(30);

        assertEquals(1, report.insights().size());
        AbsenteeismInsight insight = report.insights().get(0);
        assertEquals(3, insight.plannedLeaveDays());
        assertEquals("LOW", insight.riskLevel());
        assertTrue(insight.drivers().contains("High planned leave (3 days)"));
    }

    @Test
    void clampsWindowToSupportedRange() {
        when(employeeService.findAll()).thenReturn(List.of());
        when(scheduleEngine.findAllAssignments()).thenReturn(List.of());
        when(attendanceEngine.calculateAll()).thenReturn(List.of());
        when(leaveRequestService.findAll()).thenReturn(List.of());

        assertEquals(1, service.report(0).windowDays());
        assertEquals(365, service.report(10_000).windowDays());
        assertEquals(30, service.report(null).windowDays());
    }
}