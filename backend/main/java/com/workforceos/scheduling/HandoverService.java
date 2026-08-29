package com.workforceos.scheduling;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HandoverService {

    private final ConcurrentMap<UUID, Handover> handovers = new ConcurrentHashMap<>();

    public List<Handover> findAll() {
        return handovers.values().stream().toList();
    }

    public Handover create(HandoverRequest request) {
        validateRequest(request);

        Handover handover = new Handover(
                UUID.randomUUID(),
                request.employeeId(),
                request.items(),
                HandoverStatus.DRAFT);

        handovers.put(handover.id(), handover);
        return handover;
    }

    public Handover submit(UUID id) {
        Handover handover = findById(id);
        if (handover.status() != HandoverStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT handovers can be submitted");
        }
        Handover submitted = new Handover(
                handover.id(),
                handover.employeeId(),
                handover.items(),
                HandoverStatus.SUBMITTED);
        handovers.put(id, submitted);
        return submitted;
    }

    public Handover acknowledge(UUID id) {
        Handover handover = findById(id);
        if (handover.status() != HandoverStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED handovers can be acknowledged");
        }
        Handover acknowledged = new Handover(
                handover.id(),
                handover.employeeId(),
                handover.items(),
                HandoverStatus.ACKNOWLEDGED);
        handovers.put(id, acknowledged);
        return acknowledged;
    }

    public Handover findById(UUID id) {
        Handover handover = handovers.get(id);
        if (handover == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Handover not found");
        }
        return handover;
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
