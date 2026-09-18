package com.workforceos.integration;

public record ClockEventResult(
        String externalReference,
        boolean accepted,
        String message) {
}