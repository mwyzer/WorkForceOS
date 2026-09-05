package com.workforceos.attendance;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;

@Component
public class AttendanceCalculator {

    private final ScheduleEngine scheduleEngine;

    public AttendanceCalculator(ScheduleEngine scheduleEngine) {
        this.scheduleEngine = scheduleEngine;
    }

    public AttendanceCalculation calculate(AttendanceSession session) {
        RosterAssignment assignment = matchAssignment(session);
        if (assignment == null) {
            return new AttendanceCalculation(session, null, 0, false, false, 0);
        }

        long workedMinutes = session.clockOutAt() == null ? 0
                : Duration.between(session.clockInAt(), session.clockOutAt()).toMinutes();
        boolean late = session.clockInAt().isAfter(assignment.start());
        boolean earlyLeave = session.clockOutAt() != null && session.clockOutAt().isBefore(assignment.end());
        long overtimeMinutes = session.clockOutAt() != null && session.clockOutAt().isAfter(assignment.end())
                ? Duration.between(assignment.end(), session.clockOutAt()).toMinutes()
                : 0;

        return new AttendanceCalculation(session, assignment, workedMinutes, late, earlyLeave, overtimeMinutes);
    }

    private RosterAssignment matchAssignment(AttendanceSession session) {
        return scheduleEngine.findAllRosters().stream()
                .flatMap(roster -> scheduleEngine.findAssignments(roster.id()).stream())
                .filter(assignment -> session.rosterId().equals(assignment.rosterId())
                        && session.employeeId().equals(assignment.employeeId())
                        && session.shiftTemplateId().equals(assignment.shiftTemplateId()))
                .findFirst()
                .orElse(null);
    }
}