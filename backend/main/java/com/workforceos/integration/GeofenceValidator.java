package com.workforceos.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates a clock event's coordinates against an optional circular site geofence. When latitude,
 * longitude, or radius are not configured, the geofence is treated as disabled and all events pass.
 */
@Component
public class GeofenceValidator {

    private static final Logger LOG = LoggerFactory.getLogger(GeofenceValidator.class);
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final Double latitude;
    private final Double longitude;
    private final double radiusMeters;

    public GeofenceValidator(
            @Value("${workforce.integrations.geofence.latitude:}") String latitude,
            @Value("${workforce.integrations.geofence.longitude:}") String longitude,
            @Value("${workforce.integrations.geofence.radius-meters:500}") double radiusMeters) {
        this.latitude = parse(latitude);
        this.longitude = parse(longitude);
        this.radiusMeters = radiusMeters;
    }

    public boolean configured() {
        return latitude != null && longitude != null;
    }

    public boolean inside(double candidateLatitude, double candidateLongitude) {
        if (!configured()) {
            return true;
        }
        return distanceMeters(latitude, longitude, candidateLatitude, candidateLongitude) <= radiusMeters;
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static Double parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            LOG.warn("Ignoring unparseable geofence coordinate '{}'", value);
            return null;
        }
    }
}