package com.workforceos.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.event.EventPublisher;
import com.workforceos.event.OutboxStore;
import com.workforceos.schedule.RosterRequest;
import com.workforceos.schedule.RosterAssignmentRequest;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.shared.CurrentUser;

class AttendanceEngineTests {

    private final OutboxStore outbox = new OutboxStore();
    private final EventPublisher publisher = new EventPublisher(outbox, List.of());
    private final ScheduleEngine scheduleEngine = new ScheduleEngine(new EventPublisher(new OutboxStore(), List.of()),
            new CurrentUser());
    private final AttendanceCalculator calculator = new AttendanceCalculator(scheduleEngine);
    private final AttendanceEngine engine = new AttendanceEngine(scheduleEngine, calculator, publisher, new CurrentUser());

    @Test
    void clockInRequiresAnEligiblePublishedAssignment() {
        UUID employeeId = UUID.randomUUID();
        assertThrows(ResponseStatusException.class,
                () -> engine.clockIn(new AttendanceRequest(employeeId, OffsetDateTime.parse("2026-11-02T08:00:00Z"))));
    }

    @Test
    void calculatesWorkedDurationLateAndOvertimeAgainstAssignment() {
        UUID employeeId = UUID.randomUUID();
        UUID rosterId = scheduleEngine.createRoster(new RosterRequest(UUID.randomUUID(), "Week X")).id();
        scheduleEngine.addAssignment(rosterId, new RosterAssignmentRequest(
                employeeId, UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-02T08:00:00Z"),
                OffsetDateTime.parse("2026-11-02T16:00:00Z")));
        scheduleEngine.publishRoster(rosterId);

        engine.clockIn(new AttendanceRequest(employeeId, OffsetDateTime.parse("2026-11-02T08:30:00Z")));
        AttendanceSession closed = engine.clockOut(new AttendanceRequest(employeeId, OffsetDateTime.parse("2026-11-02T17:00:00Z")));

        AttendanceCalculation calculation = engine.calculate(closed);
        assertTrue(calculation.late());
        assertEquals(510, calculation.workedMinutes());
        assertEquals(60, calculation.overtimeMinutes());
        assertEquals(1, engine.calculateAll().size());
    }

    @Test
    void rejectsMultipleActiveClockIns() {
        UUID employeeId = UUID.randomUUID();
        UUID rosterId = scheduleEngine.createRoster(new RosterRequest(UUID.randomUUID(), "Week Y")).id();
        scheduleEngine.addAssignment(rosterId, new RosterAssignmentRequest(
                employeeId, UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-03T08:00:00Z"),
                OffsetDateTime.parse("2026-11-03T16:00:00Z")));
        scheduleEngine.publishRoster(rosterId);

        engine.clockIn(new AttendanceRequest(employeeId, OffsetDateTime.parse("2026-11-03T08:05:00Z")));
        assertThrows(ResponseStatusException.class,
                () -> engine.clockIn(new AttendanceRequest(employeeId, OffsetDateTime.parse("2026-11-03T09:00:00Z"))));
    }
}