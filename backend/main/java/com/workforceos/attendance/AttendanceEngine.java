package com.workforceos.attendance;

import java.time.OffsetDateTime;
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
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.RosterStatus;
import com.workforceos.schedule.ScheduleEngine;
import com.workforceos.shared.CurrentUser;

@Service
public class AttendanceEngine {

    private final ScheduleEngine scheduleEngine;
    private final AttendanceCalculator calculator;
    private final AttendanceSessionStore sessionStore;
    private final EventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public AttendanceEngine(ScheduleEngine scheduleEngine, AttendanceCalculator calculator,
            AttendanceSessionStore sessionStore, EventPublisher eventPublisher, CurrentUser currentUser) {
        this.scheduleEngine = scheduleEngine;
        this.calculator = calculator;
        this.sessionStore = sessionStore;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    @Transactional
    public AttendanceSession clockIn(AttendanceRequest request) {
        validateRequest(request);

        RosterAssignment assignment = findEligibleAssignment(request.employeeId(), request.occurredAt());
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not eligible to clock in at the specified time");
        }
        if (sessionStore.findActiveByEmployeeId(request.employeeId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee already has an active attendance session");
        }

        AttendanceSession session = new AttendanceSession(
                UUID.randomUUID(),
                request.employeeId(),
                assignment.rosterId(),
                assignment.shiftTemplateId(),
                request.occurredAt(),
                null,
                true);

        sessionStore.save(session);

        eventPublisher.publish(DomainEvents.of(EventTypes.ATTENDANCE_CLOCKED_IN, null, "AttendanceSession",
                session.id(), currentUser.username(),
                Payloads.json(Map.of(
                        "employeeId", session.employeeId().toString(),
                        "rosterId", session.rosterId().toString(),
                        "shiftTemplateId", session.shiftTemplateId().toString(),
                        "clockInAt", session.clockInAt().toString()))));
        return session;
    }

    @Transactional
    public AttendanceSession clockOut(AttendanceRequest request) {
        validateRequest(request);

        AttendanceSession activeSession = sessionStore.findActiveByEmployeeId(request.employeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Employee does not have an active attendance session"));

        AttendanceSession closed = new AttendanceSession(
                activeSession.id(),
                activeSession.employeeId(),
                activeSession.rosterId(),
                activeSession.shiftTemplateId(),
                activeSession.clockInAt(),
                request.occurredAt(),
                false);

        sessionStore.save(closed);

        eventPublisher.publish(DomainEvents.of(EventTypes.ATTENDANCE_CLOCKED_OUT, null, "AttendanceSession",
                closed.id(), currentUser.username(),
                Payloads.json(Map.of(
                        "employeeId", closed.employeeId().toString(),
                        "rosterId", closed.rosterId().toString(),
                        "shiftTemplateId", closed.shiftTemplateId().toString(),
                        "clockInAt", closed.clockInAt().toString(),
                        "clockOutAt", closed.clockOutAt().toString()))));
        return closed;
    }

    public List<AttendanceSession> findByEmployee(UUID employeeId) {
        if (employeeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee ID is required");
        }
        return sessionStore.findByEmployeeId(employeeId);
    }

    public List<AttendanceSession> findAllSessions() {
        return sessionStore.findAll();
    }

    public AttendanceCalculation calculate(AttendanceSession session) {
        return calculator.calculate(session);
    }

    public List<AttendanceCalculation> calculateAll() {
        return findAllSessions().stream()
                .filter(session -> session.clockOutAt() != null)
                .map(calculator::calculate)
                .toList();
    }

    private void validateRequest(AttendanceRequest request) {
        if (request == null || request.employeeId() == null || request.occurredAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee and occurrence time are required");
        }
    }

    private RosterAssignment findEligibleAssignment(UUID employeeId, OffsetDateTime occurredAt) {
        return scheduleEngine.findAllRosters().stream()
                .filter(roster -> roster.status() == RosterStatus.PUBLISHED)
                .flatMap(roster -> scheduleEngine.findAssignments(roster.id()).stream())
                .filter(assignment -> assignment.employeeId().equals(employeeId))
                .filter(RosterAssignment::active)
                .filter(assignment -> !occurredAt.isBefore(assignment.start()) && !occurredAt.isAfter(assignment.end()))
                .findFirst()
                .orElse(null);
    }
}