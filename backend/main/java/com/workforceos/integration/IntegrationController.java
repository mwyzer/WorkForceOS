package com.workforceos.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationRegistry integrationRegistry;
    private final BiometricClockService biometricClockService;
    private final boolean biometricEnabled;

    public IntegrationController(IntegrationRegistry integrationRegistry, BiometricClockService biometricClockService,
            @Value("${workforce.integrations.biometric.enabled:false}") boolean biometricEnabled) {
        this.integrationRegistry = integrationRegistry;
        this.biometricClockService = biometricClockService;
        this.biometricEnabled = biometricEnabled;
    }

    @GetMapping
    public List<IntegrationStatus> status() {
        return integrationRegistry.status();
    }

    @PostMapping("/biometric/clock-events")
    public List<ClockEventResult> ingestBiometricClockEvents(@RequestBody List<ClockEventRequest> events) {
        if (!biometricEnabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Biometric integration is disabled");
        }
        if (events == null || events.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one clock event is required");
        }
        return biometricClockService.ingest(events);
    }
}