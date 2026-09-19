package com.workforceos.scheduling;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.event.DomainEvent;
import com.workforceos.event.EventHandler;
import com.workforceos.event.EventTypes;
import com.workforceos.organization.TenantContext;
import com.workforceos.schedule.RosterAssignment;
import com.workforceos.schedule.ScheduleEngine;

@Component
public class NotificationProjector implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationProjector.class);

    private static final Set<String> SUPPORTED = Set.of(
            EventTypes.LEAVE_APPROVED,
            EventTypes.OVERTIME_APPROVED,
            EventTypes.OVERTIME_REJECTED,
            EventTypes.HANDOVER_SUBMITTED,
            EventTypes.HANDOVER_ACKNOWLEDGED,
            EventTypes.ROSTER_PUBLISHED);

    private final NotificationService notificationService;
    private final ObjectProvider<ScheduleEngine> scheduleEngineProvider;
    private final ObjectMapper objectMapper;

    public NotificationProjector(NotificationService notificationService,
            ObjectProvider<ScheduleEngine> scheduleEngineProvider, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.scheduleEngineProvider = scheduleEngineProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return SUPPORTED.contains(event.eventType());
    }

    @Override
    public void onEvent(DomainEvent event) {
        boolean scopedToEvent = event.organizationId() != null && TenantContext.current().isEmpty();
        if (scopedToEvent) {
            TenantContext.set(event.organizationId());
        }
        try {
            Map<String, String> payload = readPayload(event);
            String employeeId = payload.get("employeeId");
            switch (event.eventType()) {
                case EventTypes.LEAVE_APPROVED -> notify(employeeId, NotificationType.LEAVE_APPROVED,
                        "Leave request approved",
                        "Your leave request starting on " + payload.getOrDefault("startDate", "") + " was approved.");
                case EventTypes.OVERTIME_APPROVED -> notify(employeeId, NotificationType.OVERTIME_APPROVED,
                        "Overtime approved",
                        "Your overtime request for " + payload.getOrDefault("date", "") + " was approved.");
                case EventTypes.OVERTIME_REJECTED -> notify(employeeId, NotificationType.OVERTIME_REJECTED,
                        "Overtime rejected",
                        "Your overtime request for " + payload.getOrDefault("date", "") + " was rejected.");
                case EventTypes.HANDOVER_SUBMITTED -> notify(employeeId, NotificationType.HANDOVER_SUBMITTED,
                        "Handover submitted",
                        "Your handover was submitted and is pending acknowledgement.");
                case EventTypes.HANDOVER_ACKNOWLEDGED -> notify(employeeId, NotificationType.HANDOVER_ACKNOWLEDGED,
                        "Handover acknowledged",
                        "Your handover was acknowledged.");
                case EventTypes.ROSTER_PUBLISHED -> notifyRosterPublished(payload);
                default -> {
                }
            }
        } finally {
            if (scopedToEvent) {
                TenantContext.clear();
            }
        }
    }

    private void notifyRosterPublished(Map<String, String> payload) {
        String rosterId = payload.get("rosterId");
        ScheduleEngine scheduleEngine = scheduleEngineProvider.getIfAvailable();
        if (rosterId == null || scheduleEngine == null) {
            return;
        }
        scheduleEngine.findAssignments(UUID.fromString(rosterId)).stream()
                .map(RosterAssignment::employeeId)
                .distinct()
                .map(UUID::toString)
                .forEach(employeeId -> notify(employeeId, NotificationType.ROSTER_PUBLISHED,
                        "New roster published",
                        "The roster \"" + payload.getOrDefault("name", "") + "\" is available."));
    }

    private void notify(String employeeId, NotificationType type, String title, String body) {
        if (employeeId == null) {
            return;
        }
        try {
            notificationService.create(new NotificationRequest(
                    UUID.fromString(employeeId), type, title, body, NotificationChannel.IN_APP));
        } catch (RuntimeException ex) {
            log.warn("Failed to project notification type={} for employee={}: {}", type, employeeId, ex.getMessage());
        }
    }

    private Map<String, String> readPayload(DomainEvent event) {
        try {
            return objectMapper.readValue(event.payload(), new TypeReference<Map<String, String>>() {
            });
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not parse payload for event {}: {}", event.eventType(), ex.getMessage());
            return Map.of();
        }
    }
}