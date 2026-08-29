package com.workforceos.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RosterService {

    private final ConcurrentMap<UUID, Roster> rosters = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, RosterAssignment>> assignmentsByRoster = new ConcurrentHashMap<>();

    public List<Roster> findAll() {
        return rosters.values().stream().toList();
    }

    public Roster findById(UUID id) {
        Roster roster = rosters.get(id);
        if (roster == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found");
        }
        return roster;
    }

    public Roster create(RosterRequest request) {
        validateRoster(request);
        Roster roster = new Roster(UUID.randomUUID(), request.organizationId(), request.name().trim(),
                RosterStatus.DRAFT, OffsetDateTime.now(), true);
        rosters.put(roster.id(), roster);
        assignmentsByRoster.put(roster.id(), new ConcurrentHashMap<>());
        return roster;
    }

    public Roster publish(UUID id) {
        Roster roster = findById(id);
        if (roster.status() != RosterStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft rosters can be published");
        }
        Roster published = new Roster(roster.id(), roster.organizationId(), roster.name(), RosterStatus.PUBLISHED,
                roster.createdAt(), roster.active());
        rosters.put(id, published);
        return published;
    }

    public List<RosterAssignment> findAssignments(UUID rosterId) {
        findById(rosterId);
        ConcurrentMap<UUID, RosterAssignment> assignments = assignmentsByRoster.get(rosterId);
        return assignments == null ? List.of() : List.copyOf(assignments.values());
    }

    public RosterAssignment addAssignment(UUID rosterId, RosterAssignmentRequest request) {
        findById(rosterId);
        validateAssignment(request);

        ConcurrentMap<UUID, RosterAssignment> rosterAssignments = assignmentsByRoster.computeIfAbsent(rosterId,
                ignored -> new ConcurrentHashMap<>());

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

    private void validateRoster(RosterRequest request) {
        if (request == null || request.organizationId() == null || isBlank(request.name())) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
