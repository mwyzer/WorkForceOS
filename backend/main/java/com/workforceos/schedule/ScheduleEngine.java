package com.workforceos.schedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.event.DomainEvent;
import com.workforceos.event.DomainEvents;
import com.workforceos.event.EventPublisher;
import com.workforceos.event.EventTypes;
import com.workforceos.event.Payloads;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.ValidationUtils;

@Service
public class ScheduleEngine {

    private final EventPublisher eventPublisher;
    private final CurrentUser currentUser;
    private final ConcurrentMap<UUID, ShiftTemplate> shifts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Roster> rosters = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, RosterAssignment>> assignmentsByRoster = new ConcurrentHashMap<>();

    public ScheduleEngine(EventPublisher eventPublisher, CurrentUser currentUser) {
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    public List<ShiftTemplate> findAllShifts() {
        return shifts.values().stream().toList();
    }

    public ShiftTemplate findShift(UUID id) {
        ShiftTemplate shift = shifts.get(id);
        if (shift == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift template not found");
        }
        return shift;
    }

    public ShiftTemplate createShift(ShiftTemplateRequest request) {
        validateShift(request);
        ShiftTemplate shift = toShift(UUID.randomUUID(), request);
        shifts.put(shift.id(), shift);
        return shift;
    }

    public ShiftTemplate updateShift(UUID id, ShiftTemplateRequest request) {
        findShift(id);
        validateShift(request);
        ShiftTemplate shift = toShift(id, request);
        shifts.put(id, shift);
        return shift;
    }

    public List<Roster> findAllRosters() {
        return rosters.values().stream().toList();
    }

    public Roster findRoster(UUID id) {
        Roster roster = rosters.get(id);
        if (roster == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found");
        }
        return roster;
    }

    public Roster createRoster(RosterRequest request) {
        validateRoster(request);
        Roster roster = new Roster(UUID.randomUUID(), request.organizationId(), request.name().trim(),
                RosterStatus.DRAFT, java.time.OffsetDateTime.now(), true);
        rosters.put(roster.id(), roster);
        assignmentsByRoster.put(roster.id(), new ConcurrentHashMap<>());
        return roster;
    }

    public Roster publishRoster(UUID id) {
        Roster roster = findRoster(id);
        if (roster.status() != RosterStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft rosters can be published");
        }
        Roster published = new Roster(roster.id(), roster.organizationId(), roster.name(), RosterStatus.PUBLISHED,
                roster.createdAt(), roster.active());
        rosters.put(id, published);

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
        ConcurrentMap<UUID, RosterAssignment> assignments = assignmentsByRoster.get(rosterId);
        return assignments == null ? List.of() : List.copyOf(assignments.values());
    }

    public List<RosterAssignment> findAllAssignments() {
        return assignmentsByRoster.values().stream()
                .flatMap(assignments -> assignments.values().stream())
                .toList();
    }

    public RosterAssignment addAssignment(UUID rosterId, RosterAssignmentRequest request) {
        findRoster(rosterId);
        validateAssignment(request);

        ConcurrentMap<UUID, RosterAssignment> rosterAssignments = assignmentsByRoster.computeIfAbsent(rosterId,
                ignored -> new ConcurrentHashMap<>());

        synchronized (rosterAssignments) {
            boolean overlaps = rosterAssignments.values().stream()
                    .filter(assignment -> assignment.employeeId().equals(request.employeeId()) && assignment.active())
                    .anyMatch(assignment -> request.start().isBefore(assignment.end()) && request.end().isAfter(assignment.start()));
            if (overlaps) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee has overlapping roster assignments");
            }

            RosterAssignment assignment = new RosterAssignment(UUID.randomUUID(), rosterId, request.employeeId(),
                    request.shiftTemplateId(), request.start(), request.end(), true);
            rosterAssignments.put(assignment.id(), assignment);
            return assignment;
        }
    }

    public void removeAssignment(UUID rosterId, UUID assignmentId) {
        findRoster(rosterId);
        ConcurrentMap<UUID, RosterAssignment> rosterAssignments = assignmentsByRoster.get(rosterId);
        if (rosterAssignments == null || rosterAssignments.remove(assignmentId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster assignment not found");
        }
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
                request.organizationId(),
                request.name().trim(),
                start,
                end,
                !end.isAfter(start),
                request.breaks() == null ? List.of() : List.copyOf(request.breaks()),
                true);
    }

    private void validateShift(ShiftTemplateRequest request) {
        if (request == null
                || request.organizationId() == null
                || request.name() == null
                || request.name().isBlank()
                || request.startTime() == null
                || request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift name, organization, start, and end are required");
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
        if (request == null || request.organizationId() == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization and roster name are required");
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