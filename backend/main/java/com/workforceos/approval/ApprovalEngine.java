package com.workforceos.approval;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final ApprovalStore approvalStore;

    public ApprovalEngine(ApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    @Transactional
    public void register(UUID requestId, String requestType, UUID subjectId, String registrar) {
        if (requestId == null || requestType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request ID and request type are required");
        }
        approvalStore.saveFlow(new ApprovalFlow(requestId, requestType, subjectId, registrar, true));
    }

    @Transactional
    public ApprovalAction decide(UUID requestId, ApprovalDecision decision, String actorId, String reason) {
        if (requestId == null || decision == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request ID and decision are required");
        }
        if (decision == ApprovalDecision.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pending is not a decision");
        }

        ApprovalFlow flow = approvalStore.findFlow(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval flow not found"));
        if (!flow.open()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been decided");
        }
        if (flow.registrar() != null && flow.registrar().equalsIgnoreCase(actorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Users cannot approve their own requests");
        }

        ApprovalAction action = new ApprovalAction(requestId, flow.requestType(), decision, actorId,
                OffsetDateTime.now(), reason);
        approvalStore.saveAction(action);
        approvalStore.saveFlow(new ApprovalFlow(requestId, flow.requestType(), flow.subjectId(), flow.registrar(),
                false));
        return action;
    }

    public ApprovalFlow flow(UUID requestId) {
        return approvalStore.findFlow(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval flow not found"));
    }

    public List<ApprovalAction> history(UUID requestId) {
        return approvalStore.findHistory(requestId);
    }

    public List<ApprovalAction> findAllHistory() {
        return approvalStore.findAllHistory();
    }
}