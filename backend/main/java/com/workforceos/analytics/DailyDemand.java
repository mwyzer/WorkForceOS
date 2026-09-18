package com.workforceos.analytics;

import java.time.LocalDate;

public record DailyDemand(
        LocalDate date,
        String weekday,
        long expectedDemand,
        long scheduledShifts,
        long approvedLeave,
        long expectedAvailable,
        long gap,
        boolean shortage) {
}