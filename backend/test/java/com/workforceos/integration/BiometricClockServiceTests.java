package com.workforceos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.attendance.AttendanceEngine;

class BiometricClockServiceTests {

    private final AttendanceEngine attendanceEngine = mock(AttendanceEngine.class);
    private final ClockEventRequest clockIn = new ClockEventRequest(UUID.randomUUID(), OffsetDateTime.now(), "IN",
            null, null, "device-1");

    @Test
    void recordsClockInForValidEvent() {
        BiometricClockService service = new BiometricClockService(attendanceEngine,
                new GeofenceValidator("", "", 500));

        ClockEventResult result = service.ingest(List.of(clockIn)).get(0);

        assertTrue(result.accepted());
        verify(attendanceEngine).clockIn(any());
    }

    @Test
    void rejectsUnsupportedDirection() {
        BiometricClockService service = new BiometricClockService(attendanceEngine,
                new GeofenceValidator("", "", 500));
        ClockEventRequest event = new ClockEventRequest(UUID.randomUUID(), OffsetDateTime.now(), "SIDEWAYS",
                null, null, "device-1");

        ClockEventResult result = service.ingest(List.of(event)).get(0);

        assertFalse(result.accepted());
        assertTrue(result.message().contains("Unsupported"));
        verify(attendanceEngine, never()).clockIn(any());
    }

    @Test
    void rejectsEventOutsideGeofence() {
        BiometricClockService service = new BiometricClockService(attendanceEngine,
                new GeofenceValidator("51.5074", "-0.1278", 500));
        ClockEventRequest outside = new ClockEventRequest(UUID.randomUUID(), OffsetDateTime.now(), "IN",
                40.7128, -74.0060, "device-1");

        ClockEventResult result = service.ingest(List.of(outside)).get(0);

        assertFalse(result.accepted());
        assertTrue(result.message().contains("geofence"));
        verify(attendanceEngine, never()).clockIn(any());
    }

    @Test
    void surfacesEngineConflictAsRejectedResult() {
        when(attendanceEngine.clockIn(any())).thenThrow(
                new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not eligible to clock in"));
        BiometricClockService service = new BiometricClockService(attendanceEngine,
                new GeofenceValidator("", "", 500));

        ClockEventResult result = service.ingest(List.of(clockIn)).get(0);

        assertFalse(result.accepted());
        assertEquals("Employee is not eligible to clock in", result.message());
    }

    @Test
    void rejectsMissingEmployee() {
        BiometricClockService service = new BiometricClockService(attendanceEngine,
                new GeofenceValidator("", "", 500));
        ClockEventRequest invalid = new ClockEventRequest(null, OffsetDateTime.now(), "IN", null, null, "device-1");

        ClockEventResult result = service.ingest(List.of(invalid)).get(0);

        assertFalse(result.accepted());
        assertTrue(result.message().contains("required"));
    }
}