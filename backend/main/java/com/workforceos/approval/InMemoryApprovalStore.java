package com.workforceos.approval;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryApprovalStore implements ApprovalStore {

    private final ConcurrentMap<UUID, ApprovalEngine.ApprovalFlow> flows = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<ApprovalAction>> history = new ConcurrentHashMap<>();

    @Override
    public Optional<ApprovalEngine.ApprovalFlow> findFlow(UUID requestId) {
        return Optional.ofNullable(flows.get(requestId));
    }

    @Override
    public void saveFlow(ApprovalEngine.ApprovalFlow flow) {
        flows.put(flow.requestId(), flow);
    }

    @Override
    public List<ApprovalAction> findHistory(UUID requestId) {
        return List.copyOf(history.getOrDefault(requestId, List.of()));
    }

    @Override
    public List<ApprovalAction> findAllHistory() {
        return history.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public void saveAction(ApprovalAction action) {
        history.computeIfAbsent(action.requestId(), ignored -> new ArrayList<>()).add(action);
    }
}