package com.workforceos.integration;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.attendance.AttendanceEngine;
import com.workforceos.attendance.AttendanceRequest;

/**
 * Inbound adapter that turns external biometric/mobile clock events into attendance commands. It
 * validates the geofence, maps the external direction, and returns a per-event result rather than
 * failing the whole batch, so a device can replay a payload idempotently.
 */
@Service
public class BiometricClockService {

    private final AttendanceEngine attendanceEngine;
    private final GeofenceValidator geofenceValidator;

    public BiometricClockService(AttendanceEngine attendanceEngine, GeofenceValidator geofenceValidator) {
        this.attendanceEngine = attendanceEngine;
        this.geofenceValidator = geofenceValidator;
    }

    public List<ClockEventResult> ingest(List<ClockEventRequest> events) {
        return events.stream().map(this::ingestOne).toList();
    }

    private ClockEventResult ingestOne(ClockEventRequest event) {
        String reference = reference(event);
        if (event == null || event.employeeId() == null || event.occurredAt() == null) {
            return new ClockEventResult(reference, false, "Employee and occurrence time are required");
        }
        if (event.latitude() != null && event.longitude() != null
                && !geofenceValidator.inside(event.latitude(), event.longitude())) {
            return new ClockEventResult(reference, false, "Clock event is outside the configured geofence");
        }

        AttendanceRequest request = new AttendanceRequest(event.employeeId(), event.occurredAt());
        String direction = normalizeDirection(event.direction());
        try {
            if ("IN".equals(direction)) {
                attendanceEngine.clockIn(request);
                return new ClockEventResult(reference, true, "Clock-in recorded");
            }
            if ("OUT".equals(direction)) {
                attendanceEngine.clockOut(request);
                return new ClockEventResult(reference, true, "Clock-out recorded");
            }
            return new ClockEventResult(reference, false,
                    "Unsupported clock direction: " + event.direction());
        } catch (ResponseStatusException ex) {
            return new ClockEventResult(reference, false,
                    ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason());
        } catch (RuntimeException ex) {
            return new ClockEventResult(reference, false, "Clock event could not be processed");
        }
    }

    private String reference(ClockEventRequest event) {
        if (event == null || event.employeeId() == null) {
            return "unknown";
        }
        return event.employeeId() + "@" + event.occurredAt();
    }

    private String normalizeDirection(String direction) {
        if (direction == null) {
            return "";
        }
        return switch (direction.trim().toUpperCase(Locale.ROOT)) {
            case "IN", "CLOCK_IN", "CLOCKIN", "CLOCK-IN" -> "IN";
            case "OUT", "CLOCK_OUT", "CLOCKOUT", "CLOCK-OUT" -> "OUT";
            default -> "";
        };
    }
}