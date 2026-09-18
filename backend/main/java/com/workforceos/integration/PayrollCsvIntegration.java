package com.workforceos.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reference payroll integration that appends each event to a CSV file. Useful offline and as an
 * adapter for scheduled file-based payroll imports; a vendor-specific adapter can replace or
 * complement it behind the same {@link OutboundIntegration} port.
 */
@Component
public class PayrollCsvIntegration implements OutboundIntegration {

    private static final String FILE_NAME = "payroll-export.csv";

    private final boolean enabled;
    private final Path directory;

    public PayrollCsvIntegration(
            @Value("${workforce.integrations.payroll-csv.enabled:false}") boolean enabled,
            @Value("${workforce.integrations.payroll-csv.directory:}") String directory) {
        this.enabled = enabled;
        this.directory = Path.of(directory == null || directory.isBlank()
                ? System.getProperty("java.io.tmpdir") + "/workforceos-integrations"
                : directory.trim());
    }

    @Override
    public String name() {
        return "payroll-csv";
    }

    @Override
    public IntegrationType type() {
        return IntegrationType.PAYROLL;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public IntegrationDelivery deliver(IntegrationEvent event) {
        if (!enabled) {
            return new IntegrationDelivery(name(), DeliveryStatus.SKIPPED, "Integration disabled");
        }
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(FILE_NAME);
            synchronized (this) {
                Files.writeString(file, toRow(event) + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            return new IntegrationDelivery(name(), DeliveryStatus.DELIVERED, file.toString());
        } catch (IOException ex) {
            return new IntegrationDelivery(name(), DeliveryStatus.FAILED, ex.getMessage());
        }
    }

    private String toRow(IntegrationEvent event) {
        StringBuilder row = new StringBuilder();
        row.append(escape(event.occurredAt() == null ? Instant.now().toString() : event.occurredAt().toString()))
                .append(',').append(escape(event.externalId()));
        Map<String, Object> payload = new TreeMap<>(event.payload() == null ? Map.of() : event.payload());
        row.append(',').append(escape(payload.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "))));
        return row.toString();
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}