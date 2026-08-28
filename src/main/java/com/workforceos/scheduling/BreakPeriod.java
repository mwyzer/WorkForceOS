package com.workforceos.scheduling;

import java.time.LocalTime;

public record BreakPeriod(LocalTime start, LocalTime end) {
}
