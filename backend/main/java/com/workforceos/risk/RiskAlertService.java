package com.workforceos.risk;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.event.DomainEvent;
import com.workforceos.event.EventHandler;
import com.workforceos.event.EventTypes;

@Service
public class RiskAlertService implements EventHandler {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            EventTypes.COVERAGE_RISK_DETECTED,
            EventTypes.WORKFORCE_RISK_ELEVATED);

    private final RiskAlertRepository alertRepository;
    private final ObjectMapper mapper;

    public RiskAlertService(RiskAlertRepository alertRepository, ObjectMapper mapper) {
        this.alertRepository = alertRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return SUPPORTED_EVENTS.contains(event.eventType());
    }

    @Override
    public void onEvent(DomainEvent event) {
        if (event.aggregateId() == null || alertRepository.findByAssessmentId(event.aggregateId()).isPresent()) {
            return;
        }
        String severityName = "HIGH";
        String summary = "Workforce risk detected";
        try {
            JsonNode node = mapper.readTree(event.payload());
            if (node.hasNonNull("severity")) {
                severityName = node.get("severity").asText();
            }
            if (node.hasNonNull("summary")) {
                summary = node.get("summary").asText();
            }
        } catch (Exception ignored) {
            // Keep defaults if the payload cannot be parsed.
        }
        RiskSeverity severity = RiskSeverity.MEDIUM;
        for (RiskSeverity candidate : RiskSeverity.values()) {
            if (candidate.name().equalsIgnoreCase(severityName)) {
                severity = candidate;
                break;
            }
        }
        alertRepository.save(new RiskAlertEntity(UUID.randomUUID(), event.aggregateId(), severity, summary));
    }

    public List<RiskAlert> findAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RiskAlertEntity::toRecord)
                .toList();
    }

    public RiskAlert findById(UUID id) {
        return alertRepository.findById(id)
                .map(RiskAlertEntity::toRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk alert not found"));
    }

    public RiskAlert resolve(UUID id) {
        RiskAlertEntity alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk alert not found"));
        if (alert.status() != RiskAlertStatus.RESOLVED) {
            alert.resolve();
            alertRepository.save(alert);
        }
        return alert.toRecord();
    }
}