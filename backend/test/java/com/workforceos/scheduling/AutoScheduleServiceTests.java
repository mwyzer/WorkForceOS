package com.workforceos.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.workforceos.schedule.Roster;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.RosterStatus;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.schedule.ShiftTemplate;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;

class AutoScheduleServiceTests {

    private final ScheduleEngine scheduleEngine = mock(ScheduleEngine.class);
    private final EmployeeService employeeService = mock(EmployeeService.class);
    private final LeaveRequestService leaveRequestService = mock(LeaveRequestService.class);
    private final AutoScheduleService service = new AutoScheduleService(scheduleEngine, employeeService,
            leaveRequestService, 2);

    @Test
    void proposesEligibleEmployeeForUnderstaffedShift() {
        UUID rosterId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        Employee assigned = employee("EMP-1");
        Employee eligible = employee("EMP-2");
        Employee onLeave = employee("EMP-3");
        Employee conflicting = employee("EMP-4");

        OffsetDateTime start = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0), ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(8);

        when(scheduleEngine.findRoster(rosterId))
                .thenReturn(new Roster(rosterId, UUID.randomUUID(), "Week 1", RosterStatus.DRAFT,
                        OffsetDateTime.now(), true));
        RosterAssignment existing = new RosterAssignment(UUID.randomUUID(), UUID.randomUUID(), rosterId,
                assigned.id(), shiftId,
                start, end, true);
        when(scheduleEngine.findAssignments(rosterId)).thenReturn(List.of(existing));
        RosterAssignment elsewhere = new RosterAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                conflicting.id(),
                shiftId, start.minusHours(1), end.plusHours(1), true);
        when(scheduleEngine.findAllAssignments()).thenReturn(List.of(existing, elsewhere));
        when(scheduleEngine.findAllShifts()).thenReturn(List.of(new ShiftTemplate(shiftId, UUID.randomUUID(),
                "Day", LocalTime.of(9, 0), LocalTime.of(17, 0), false, List.of(), true)));
        when(employeeService.findAll()).thenReturn(List.of(conflicting, eligible, onLeave, assigned));
        when(leaveRequestService.findAll()).thenReturn(List.of(new LeaveRequest(UUID.randomUUID(), onLeave.id(),
                LocalDate.now(), LocalDate.now().plusDays(3), "Rest", LeaveRequestStatus.APPROVED)));

        AutoSchedulePlan plan = service.propose(rosterId);

        assertEquals(1, plan.understaffedSlots());
        assertEquals(2, plan.targetHeadcount());
        assertEquals("Week 1", plan.rosterName());
        assertEquals(1, plan.proposals().size());
        AutoScheduleProposal proposal = plan.proposals().get(0);
        assertEquals(eligible.id(), proposal.employeeId());
        assertEquals(shiftId, proposal.shiftTemplateId());
        assertEquals("Day", proposal.shiftName());
        assertTrue(proposal.rationale().contains("understaffed"));
    }

    @Test
    void returnsEmptyPlanWhenFullyStaffed() {
        UUID rosterId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        Employee first = employee("EMP-1");
        Employee second = employee("EMP-2");
        OffsetDateTime start = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0), ZoneOffset.UTC);

        when(scheduleEngine.findRoster(rosterId))
                .thenReturn(new Roster(rosterId, UUID.randomUUID(), "Week 2", RosterStatus.DRAFT,
                        OffsetDateTime.now(), true));
        when(scheduleEngine.findAssignments(rosterId)).thenReturn(List.of(
                new RosterAssignment(UUID.randomUUID(), UUID.randomUUID(), rosterId, first.id(), shiftId, start,
                        start.plusHours(8), true),
                new RosterAssignment(UUID.randomUUID(), UUID.randomUUID(), rosterId, second.id(), shiftId, start,
                        start.plusHours(8), true)));
        when(scheduleEngine.findAllAssignments()).thenReturn(List.of());
        when(scheduleEngine.findAllShifts()).thenReturn(List.of());
        when(employeeService.findAll()).thenReturn(List.of(first, second));
        when(leaveRequestService.findAll()).thenReturn(List.of());

        AutoSchedulePlan plan = service.propose(rosterId);

        assertEquals(0, plan.understaffedSlots());
        assertTrue(plan.proposals().isEmpty());
    }

    private static Employee employee(String number) {
        return new Employee(UUID.randomUUID(), number, "First", "Last", number + "@workforceos.io", null, null, true);
    }
}