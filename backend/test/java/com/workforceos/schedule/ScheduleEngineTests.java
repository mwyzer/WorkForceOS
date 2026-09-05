package com.workforceos.schedule;

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
import com.workforceos.event.OutboxStatus;
import com.workforceos.shared.CurrentUser;

class ScheduleEngineTests {

    private final OutboxStore outbox = new OutboxStore();
    private final EventPublisher publisher = new EventPublisher(outbox, List.of());
    private final ScheduleEngine engine = new ScheduleEngine(publisher, new CurrentUser());

    @Test
    void createsShiftTemplatesIncludingOvernightShifts() {
        UUID organizationId = UUID.randomUUID();

        ShiftTemplate day = engine.createShift(new ShiftTemplateRequest(
                organizationId, "Day", java.time.LocalTime.of(8, 0), java.time.LocalTime.of(16, 0), List.of()));
        ShiftTemplate night = engine.createShift(new ShiftTemplateRequest(
                organizationId, "Night", java.time.LocalTime.of(22, 0), java.time.LocalTime.of(6, 0), List.of()));

        assertEquals(2, engine.findAllShifts().size());
        assertTrue(!day.overnight());
        assertTrue(night.overnight());
    }

    @Test
    void rejectsEqualShiftStartAndEnd() {
        assertThrows(ResponseStatusException.class,
                () -> engine.createShift(new ShiftTemplateRequest(UUID.randomUUID(), "Broken",
                        java.time.LocalTime.of(9, 0), java.time.LocalTime.of(9, 0), List.of())));
    }

    @Test
    void publishesOnlyDraftRosters() {
        UUID rosterId = engine.createRoster(new RosterRequest(UUID.randomUUID(), "Week 10")).id();

        assertEquals(RosterStatus.PUBLISHED, engine.publishRoster(rosterId).status());
        assertEquals(1, outbox.findAll().size());
        assertEquals(OutboxStatus.DELIVERED, outbox.findAll().getFirst().status());
        assertThrows(ResponseStatusException.class, () -> engine.publishRoster(rosterId));
    }

    @Test
    void detectsConflictsAcrossRostersAtEngineLevel() {
        UUID employeeId = UUID.randomUUID();
        UUID firstRosterId = engine.createRoster(new RosterRequest(UUID.randomUUID(), "Week A")).id();

        OffsetDateTime start = OffsetDateTime.parse("2026-10-05T08:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-10-05T16:00:00Z");

        engine.addAssignment(firstRosterId, new RosterAssignmentRequest(employeeId, UUID.randomUUID(), start, end));

        UUID secondRosterId = engine.createRoster(new RosterRequest(UUID.randomUUID(), "Week B")).id();
        engine.addAssignment(secondRosterId, new RosterAssignmentRequest(employeeId, UUID.randomUUID(),
                start.plusHours(1), end.plusHours(1)));

        List<ScheduleConflict> conflicts = engine.findConflicts();
        assertEquals(1, conflicts.size());
        assertEquals(employeeId, conflicts.getFirst().employeeId());
    }
}