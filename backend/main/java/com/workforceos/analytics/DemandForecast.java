package com.workforceos.analytics;

import java.time.Instant;
import java.util.List;

public record DemandForecast(
        Instant generatedAt,
        int horizonDays,
        int lookbackWeeks,
        long totalExpectedDemand,
        long totalScheduledShifts,
        long shortageDays,
        List<DailyDemand> days) {
}