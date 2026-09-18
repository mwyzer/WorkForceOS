package com.workforceos.approval;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalStore {

    Optional<ApprovalEngine.ApprovalFlow> findFlow(UUID requestId);

    void saveFlow(ApprovalEngine.ApprovalFlow flow);

    List<ApprovalAction> findHistory(UUID requestId);

    List<ApprovalAction> findAllHistory();

    void saveAction(ApprovalAction action);
}