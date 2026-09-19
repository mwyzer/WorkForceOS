package com.workforceos.schedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.event.DomainEvent;
import com.workforceos.event.DomainEvents;
import com.workforceos.event.EventPublisher;
import com.workforceos.event.EventTypes;
import com.workforceos.event.Payloads;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.TenantScope;
import com.workforceos.shared.ValidationUtils;

@Service
public class ScheduleEngine {

    private final ShiftTemplateStore shiftStore;
    private final RosterStore rosterStore;
    private final RosterAssignmentStore assignmentStore;
    private final EventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public ScheduleEngine(ShiftTemplateStore shiftStore, RosterStore rosterStore,
            RosterAssignmentStore assignmentStore, EventPublisher eventPublisher, CurrentUser currentUser) {
        this.shiftStore = shiftStore;
        this.rosterStore = rosterStore;
        this.assignmentStore = assignmentStore;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    public List<ShiftTemplate> findAllShifts() {
        UUID tenantId = TenantScope.require();
        return shiftStore.findAll().stream()
                .filter(shift -> shift.organizationId() != null && shift.organizationId().equals(tenantId))
                .toList();
    }

    public ShiftTemplate findShift(UUID id) {
        ShiftTemplate shift = shiftStore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift template not found"));
        TenantScope.assertAccess(shift.organizationId());
        return shift;
    }

    @Transactional
    public ShiftTemplate createShift(ShiftTemplateRequest request) {
        validateShift(request);
        return shiftStore.save(toShift(UUID.randomUUID(), request));
    }

    @Transactional
    public ShiftTemplate updateShift(UUID id, ShiftTemplateRequest request) {
        findShift(id);
        validateShift(request);
        return shiftStore.save(toShift(id, request));
    }

    public List<Roster> findAllRosters() {
        UUID tenantId = TenantScope.require();
        return rosterStore.findAll().stream()
                .filter(roster -> roster.organizationId() != null && roster.organizationId().equals(tenantId))
                .toList();
    }

    public Roster findRoster(UUID id) {
        Roster roster = rosterStore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found"));
        TenantScope.assertAccess(roster.organizationId());
        return roster;
    }

    @Transactional
    public Roster createRoster(RosterRequest request) {
        validateRoster(request);
        return rosterStore.save(new Roster(UUID.randomUUID(), TenantScope.require(), request.name().trim(),
                RosterStatus.DRAFT, java.time.OffsetDateTime.now(), true));
    }

    @Transactional
    public Roster publishRoster(UUID id) {
        Roster roster = findRoster(id);
        if (roster.status() != RosterStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft rosters can be published");
        }
        Roster published = new Roster(roster.id(), roster.organizationId(), roster.name(), RosterStatus.PUBLISHED,
                roster.createdAt(), roster.active());
        rosterStore.save(published);

        DomainEvent event = DomainEvents.of(EventTypes.ROSTER_PUBLISHED, published.organizationId(), "Roster",
                published.id(), currentUser.username(),
                Payloads.json(Map.of(
                        "rosterId", published.id().toString(),
                        "name", published.name(),
                        "status", published.status().name())));
        eventPublisher.publish(event);
        return published;
    }

    public List<RosterAssignment> findAssignments(UUID rosterId) {
        findRoster(rosterId);
        return assignmentStore.findByRosterId(rosterId);
    }

    public List<RosterAssignment> findAllAssignments() {
        UUID tenantId = TenantScope.require();
        return assignmentStore.findAll().stream()
                .filter(assignment -> assignment.organizationId() != null
                        && assignment.organizationId().equals(tenantId))
                .toList();
    }

    @Transactional
    public RosterAssignment addAssignment(UUID rosterId, RosterAssignmentRequest request) {
        findRoster(rosterId);
        validateAssignment(request);

        boolean overlaps = assignmentStore.findByRosterId(rosterId).stream()
                .filter(assignment -> assignment.employeeId().equals(request.employeeId()) && assignment.active())
                .anyMatch(assignment -> request.start().isBefore(assignment.end())
                        && request.end().isAfter(assignment.start()));
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee has overlapping roster assignments");
        }

        RosterAssignment assignment = new RosterAssignment(UUID.randomUUID(), TenantScope.require(), rosterId,
                request.employeeId(),
                request.shiftTemplateId(), request.start(), request.end(), true);
        return assignmentStore.save(assignment);
    }

    @Transactional
    public void removeAssignment(UUID rosterId, UUID assignmentId) {
        findRoster(rosterId);
        boolean exists = assignmentStore.findByRosterId(rosterId).stream()
                .anyMatch(assignment -> assignment.id().equals(assignmentId));
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster assignment not found");
        }
        assignmentStore.delete(assignmentId);
    }

    public List<ScheduleConflict> findConflicts() {
        List<RosterAssignment> assignments = findAllAssignments().stream()
                .filter(RosterAssignment::active)
                .toList();
        List<ScheduleConflict> conflicts = new ArrayList<>();
        for (int i = 0; i < assignments.size(); i++) {
            RosterAssignment first = assignments.get(i);
            for (int j = i + 1; j < assignments.size(); j++) {
                RosterAssignment second = assignments.get(j);
                if (first.employeeId().equals(second.employeeId())
                        && first.start().isBefore(second.end())
                        && second.start().isBefore(first.end())) {
                    conflicts.add(new ScheduleConflict(first.id(), second.id(), first.rosterId(), first.employeeId(),
                            "Employee has overlapping roster assignments"));
                }
            }
        }
        return conflicts;
    }

    private ShiftTemplate toShift(UUID id, ShiftTemplateRequest request) {
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        return new ShiftTemplate(
                id,
                TenantScope.require(),
                request.name().trim(),
                start,
                end,
                !end.isAfter(start),
                request.breaks() == null ? List.of() : List.copyOf(request.breaks()),
                true);
    }

    private void validateShift(ShiftTemplateRequest request) {
        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.startTime() == null
                || request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift name, start, and end are required");
        }
        if (request.startTime().equals(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift start and end cannot be equal");
        }
        if (request.breaks() != null) {
            boolean invalidBreak = request.breaks().stream()
                    .anyMatch(breakPeriod -> breakPeriod == null
                            || breakPeriod.start() == null
                            || breakPeriod.end() == null
                            || breakPeriod.start().equals(breakPeriod.end()));
            if (invalidBreak) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Break periods require different start and end times");
            }
        }
    }

    private void validateRoster(RosterRequest request) {
        if (request == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roster name is required");
        }
    }

    private void validateAssignment(RosterAssignmentRequest request) {
        if (request == null || request.employeeId() == null || request.shiftTemplateId() == null
                || request.start() == null || request.end() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee, shift, and time range are required");
        }
        if (!request.end().isAfter(request.start())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment end must be after start");
        }
    }
}