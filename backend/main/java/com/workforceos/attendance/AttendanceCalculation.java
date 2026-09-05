package com.workforceos.attendance;

import com.workforceos.schedule.RosterAssignment;

public record AttendanceCalculation(
        AttendanceSession session,
        RosterAssignment assignment,
        long workedMinutes,
        boolean late,
        boolean earlyLeave,
        long overtimeMinutes) {
}