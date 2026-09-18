package com.workforceos.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeofenceValidatorTests {

    @Test
    void disabledWhenCoordinatesMissing() {
        GeofenceValidator validator = new GeofenceValidator("", "", 500);

        assertFalse(validator.configured());
        assertTrue(validator.inside(0.0, 0.0));
    }

    @Test
    void acceptsCoordinatesInsideRadius() {
        GeofenceValidator validator = new GeofenceValidator("51.5074", "-0.1278", 1000);

        assertTrue(validator.configured());
        assertTrue(validator.inside(51.5076, -0.1280));
    }

    @Test
    void rejectsCoordinatesOutsideRadius() {
        GeofenceValidator validator = new GeofenceValidator("51.5074", "-0.1278", 500);

        assertFalse(validator.inside(40.7128, -74.0060));
    }

    @Test
    void distanceMatchesKnownSeparation() {
        double meters = GeofenceValidator.distanceMeters(51.5074, -0.1278, 51.5174, -0.1278);

        assertTrue(meters > 1000 && meters < 1200, "Expected ~1.1km, got " + meters);
    }
}