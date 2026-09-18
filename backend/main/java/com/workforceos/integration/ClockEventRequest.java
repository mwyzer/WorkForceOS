package com.workforceos.integration;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClockEventRequest(
        UUID employeeId,
        OffsetDateTime occurredAt,
        String direction,
        Double latitude,
        Double longitude,
        String deviceId) {
}