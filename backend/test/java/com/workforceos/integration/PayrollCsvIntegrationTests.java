package com.workforceos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PayrollCsvIntegrationTests {

    @TempDir
    Path directory;

    @Test
    void writesEventToCsvWhenEnabled() throws Exception {
        PayrollCsvIntegration integration = new PayrollCsvIntegration(true, directory.toString());

        IntegrationDelivery delivery = integration.deliver(new IntegrationEvent(IntegrationType.PAYROLL, "EMP-1",
                Map.of("hours", 8, "period", "2026-01"), Instant.parse("2026-01-31T10:00:00Z")));

        assertEquals(DeliveryStatus.DELIVERED, delivery.status());
        String content = Files.readString(directory.resolve("payroll-export.csv"));
        assertTrue(content.contains("EMP-1"));
        assertTrue(content.contains("hours=8"));
    }

    @Test
    void skipsWhenDisabled() {
        PayrollCsvIntegration integration = new PayrollCsvIntegration(false, directory.toString());

        IntegrationDelivery delivery = integration.deliver(new IntegrationEvent(IntegrationType.PAYROLL, "EMP-2",
                Map.of(), Instant.now()));

        assertEquals(DeliveryStatus.SKIPPED, delivery.status());
    }
}