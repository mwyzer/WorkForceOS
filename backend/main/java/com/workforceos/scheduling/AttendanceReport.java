package com.workforceos.scheduling;

public record AttendanceReport(
        long present,
        long late,
        long absent,
        long earlyLeave,
        long overtime) {
}
