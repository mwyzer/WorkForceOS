package com.workforceos.scheduling;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {

    private final RosterService rosterService;
    private final ConcurrentMap<UUID, AttendanceSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<AttendanceSession>> employeeHistory = new ConcurrentHashMap<>();

    public AttendanceService(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    public AttendanceSession clockIn(AttendanceRequest request) {
        validateRequest(request);

        RosterAssignment assignment = findEligibleAssignment(request.employeeId(), request.occurredAt());
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not eligible to clock in at the specified time");
        }

        AttendanceSession session = new AttendanceSession(
                UUID.randomUUID(),
                request.employeeId(),
                assignment.rosterId(),
                assignment.shiftTemplateId(),
                request.occurredAt(),
                null,
                true);

        if (activeSessions.putIfAbsent(request.employeeId(), session) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee already has an active attendance session");
        }
        employeeHistory.computeIfAbsent(request.employeeId(), ignored -> new ArrayList<>()).add(session);
        return session;
    }

    public AttendanceSession clockOut(AttendanceRequest request) {
        validateRequest(request);

        AttendanceSession activeSession = activeSessions.get(request.employeeId());
        if (activeSession == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee does not have an active attendance session");
        }

        AttendanceSession closed = new AttendanceSession(
                activeSession.id(),
                activeSession.employeeId(),
                activeSession.rosterId(),
                activeSession.shiftTemplateId(),
                activeSession.clockInAt(),
                request.occurredAt(),
                false);

        activeSessions.remove(request.employeeId());
        employeeHistory.computeIfAbsent(request.employeeId(), ignored -> new ArrayList<>()).add(closed);
        return closed;
    }

    public List<AttendanceSession> findByEmployee(UUID employeeId) {
        if (employeeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee ID is required");
        }
        return employeeHistory.getOrDefault(employeeId, List.of());
    }

    private void validateRequest(AttendanceRequest request) {
        if (request == null || request.employeeId() == null || request.occurredAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee and occurrence time are required");
        }
    }

    private RosterAssignment findEligibleAssignment(UUID employeeId, OffsetDateTime occurredAt) {
        return rosterService.findAll().stream()
                .filter(roster -> roster.status() == RosterStatus.PUBLISHED)
                .flatMap(roster -> rosterService.findAssignments(roster.id()).stream())
                .filter(assignment -> assignment.employeeId().equals(employeeId))
                .filter(RosterAssignment::active)
                .filter(assignment -> !occurredAt.isBefore(assignment.start()) && !occurredAt.isAfter(assignment.end()))
                .findFirst()
                .orElse(null);
    }

    public List<AttendanceSession> findAllSessions() {
        return employeeHistory.values().stream().flatMap(List::stream).toList();
    }
}
