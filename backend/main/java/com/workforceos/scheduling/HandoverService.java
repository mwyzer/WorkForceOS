package com.workforceos.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.event.DomainEvents;
import com.workforceos.event.EventPublisher;
import com.workforceos.event.EventTypes;
import com.workforceos.event.Payloads;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.TenantScope;

@Service
public class HandoverService {

    private final HandoverStore handoverStore;
    private final EventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public HandoverService(HandoverStore handoverStore, EventPublisher eventPublisher, CurrentUser currentUser) {
        this.handoverStore = handoverStore;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    public List<Handover> findAll() {
        UUID tenantId = TenantScope.require();
        return handoverStore.findAll().stream()
                .filter(handover -> handover.organizationId() != null && handover.organizationId().equals(tenantId))
                .toList();
    }

    @Transactional
    public Handover create(HandoverRequest request) {
        validateRequest(request);

        Handover handover = new Handover(
                UUID.randomUUID(),
                TenantScope.require(),
                request.employeeId(),
                request.items(),
                HandoverStatus.DRAFT);

        return handoverStore.save(handover);
    }

    @Transactional
    public Handover submit(UUID id) {
        Handover handover = findById(id);
        if (handover.status() != HandoverStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT handovers can be submitted");
        }
        Handover submitted = new Handover(
                handover.id(),
                handover.organizationId(),
                handover.employeeId(),
                handover.items(),
                HandoverStatus.SUBMITTED);
        handoverStore.save(submitted);

        eventPublisher.publish(DomainEvents.of(EventTypes.HANDOVER_SUBMITTED, submitted.organizationId(), "Handover",
                submitted.id(),
                currentUser.username(),
                Payloads.json(java.util.Map.of(
                        "employeeId", submitted.employeeId().toString(),
                        "itemCount", String.valueOf(submitted.items().size())))));
        return submitted;
    }

    @Transactional
    public Handover acknowledge(UUID id) {
        Handover handover = findById(id);
        if (handover.status() != HandoverStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED handovers can be acknowledged");
        }
        Handover acknowledged = new Handover(
                handover.id(),
                handover.organizationId(),
                handover.employeeId(),
                handover.items(),
                HandoverStatus.ACKNOWLEDGED);
        handoverStore.save(acknowledged);

        eventPublisher.publish(DomainEvents.of(EventTypes.HANDOVER_ACKNOWLEDGED, acknowledged.organizationId(),
                "Handover", acknowledged.id(),
                currentUser.username(),
                Payloads.json(java.util.Map.of(
                        "employeeId", acknowledged.employeeId().toString()))));
        return acknowledged;
    }

    public Handover findById(UUID id) {
        return handoverStore.findById(id)
                .map(handover -> {
                    TenantScope.assertAccess(handover.organizationId());
                    return handover;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Handover not found"));
    }

    private void validateRequest(HandoverRequest request) {
        if (request == null
                || request.employeeId() == null
                || request.items() == null
                || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee and at least one handover item are required");
        }
    }
}