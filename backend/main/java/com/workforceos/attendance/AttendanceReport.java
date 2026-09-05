package com.workforceos.attendance;

public record AttendanceReport(
        long present,
        long late,
        long absent,
        long earlyLeave,
        long overtime) {
}
