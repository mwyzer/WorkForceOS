package com.workforceos.risk;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

final class RiskScoring {

    private RiskScoring() {
    }

    static int cap(int score) {
        return Math.min(100, Math.max(0, score));
    }

    static RiskSeverity severity(int score) {
        if (score >= 80) {
            return RiskSeverity.HIGH;
        }
        if (score >= 50) {
            return RiskSeverity.MEDIUM;
        }
        return RiskSeverity.LOW;
    }

    static Instant weekBucket(Instant timestamp) {
        LocalDate date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        return monday.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    static Instant monthBucket(Instant timestamp) {
        LocalDate firstDay = timestamp.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        return firstDay.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}