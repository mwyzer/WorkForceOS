package com.workforceos.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;

class DemandForecastServiceTests {

    private final EmployeeService employeeService = mock(EmployeeService.class);
    private final ScheduleEngine scheduleEngine = mock(ScheduleEngine.class);
    private final LeaveRequestService leaveRequestService = mock(LeaveRequestService.class);
    private final DemandForecastService service = new DemandForecastService(
            employeeService, scheduleEngine, leaveRequestService, 7, 4);

    @Test
    void averagesHistoricalVolumeByWeekdayAndFlagsShortage() {
        LocalDate today = LocalDate.now();
        List<RosterAssignment> history = new java.util.ArrayList<>();
        for (int week = 1; week <= 4; week++) {
            OffsetDateTime day = today.minusWeeks(week).atTime(9, 0).atOffset(ZoneOffset.UTC);
            history.add(assignment(day));
            history.add(assignment(day.plusHours(1)));
        }

        when(employeeService.findAll()).thenReturn(List.of(
                employee(true), employee(true), employee(true), employee(false)));
        when(scheduleEngine.findAllAssignments()).thenReturn(history);
        when(leaveRequestService.findAll()).thenReturn(List.of());

        DemandForecast forecast = service.forecast(7);

        assertEquals(7, forecast.horizonDays());
        assertEquals(7, forecast.days().size());
        DailyDemand firstDay = forecast.days().get(0);
        assertEquals(today, firstDay.date());
        assertEquals(2, firstDay.expectedDemand());
        assertEquals(0, firstDay.scheduledShifts());
        assertEquals(2, firstDay.gap());
        assertTrue(firstDay.shortage());
        assertEquals(3, firstDay.expectedAvailable());
        assertTrue(forecast.shortageDays() >= 1);
    }

    @Test
    void countsApprovedLeaveAsUnavailable() {
        LocalDate today = LocalDate.now();
        UUID onLeaveId = UUID.randomUUID();
        when(employeeService.findAll()).thenReturn(List.of(
                employee(true), employee(true), employee(true)));
        when(scheduleEngine.findAllAssignments()).thenReturn(List.of());
        when(leaveRequestService.findAll()).thenReturn(List.of(
                new LeaveRequest(UUID.randomUUID(), onLeaveId, today, today.plusDays(2), "Rest",
                        LeaveRequestStatus.APPROVED)));

        DailyDemand firstDay = service.forecast(3).days().get(0);

        assertEquals(1, firstDay.approvedLeave());
        assertEquals(2, firstDay.expectedAvailable());
        assertFalse(firstDay.shortage());
    }

    private static Employee employee(boolean active) {
        return new Employee(UUID.randomUUID(), "EMP-" + UUID.randomUUID(), "First", "Last", "e@workforceos.io",
                null, null, active);
    }

    private static RosterAssignment assignment(OffsetDateTime start) {
        return new RosterAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                start, start.plusHours(8), true);
    }
}