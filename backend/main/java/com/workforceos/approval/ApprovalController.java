package com.workforceos.approval;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalEngine approvalEngine;

    public ApprovalController(ApprovalEngine approvalEngine) {
        this.approvalEngine = approvalEngine;
    }

    @GetMapping("/history")
    public List<ApprovalAction> history() {
        return approvalEngine.findAllHistory();
    }

    @GetMapping("/{requestId}")
    public List<ApprovalAction> history(@PathVariable UUID requestId) {
        return approvalEngine.history(requestId);
    }
}