package com.workforceos.approval;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApprovalEngine {

    public record ApprovalFlow(
            UUID requestId,
            String requestType,
            UUID subjectId,
            String registrar,
            boolean open) {
    }

    private final ConcurrentMap<UUID, ApprovalFlow> flows = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<ApprovalAction>> history = new ConcurrentHashMap<>();

    public void register(UUID requestId, String requestType, UUID subjectId, String registrar) {
        if (requestId == null || requestType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request ID and request type are required");
        }
        flows.put(requestId, new ApprovalFlow(requestId, requestType, subjectId, registrar, true));
    }

    public ApprovalAction decide(UUID requestId, ApprovalDecision decision, String actorId, String reason) {
        if (requestId == null || decision == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request ID and decision are required");
        }
        if (decision == ApprovalDecision.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pending is not a decision");
        }

        ApprovalFlow flow = flows.get(requestId);
        if (flow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval flow not found");
        }
        if (!flow.open()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been decided");
        }
        if (flow.registrar() != null && flow.registrar().equalsIgnoreCase(actorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Users cannot approve their own requests");
        }

        ApprovalAction action = new ApprovalAction(requestId, flow.requestType(), decision, actorId,
                OffsetDateTime.now(), reason);
        history.computeIfAbsent(requestId, ignored -> new ArrayList<>()).add(action);
        flows.put(requestId, new ApprovalFlow(requestId, flow.requestType(), flow.subjectId(), flow.registrar(), false));
        return action;
    }

    public ApprovalFlow flow(UUID requestId) {
        ApprovalFlow flow = flows.get(requestId);
        if (flow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval flow not found");
        }
        return flow;
    }

    public List<ApprovalAction> history(UUID requestId) {
        return List.copyOf(history.getOrDefault(requestId, List.of()));
    }

    public List<ApprovalAction> findAllHistory() {
        return history.values().stream().flatMap(List::stream).toList();
    }
}