package com.workforceos.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.approval.ApprovalDecision;
import com.workforceos.approval.ApprovalEngine;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.TenantScope;
import com.workforceos.shared.ValidationUtils;

@Service
public class ShiftSwapRequestService {

    private final ShiftSwapRequestStore shiftSwapRequestStore;
    private final ApprovalEngine approvalEngine;
    private final CurrentUser currentUser;

    public ShiftSwapRequestService(ShiftSwapRequestStore shiftSwapRequestStore, ApprovalEngine approvalEngine,
            CurrentUser currentUser) {
        this.shiftSwapRequestStore = shiftSwapRequestStore;
        this.approvalEngine = approvalEngine;
        this.currentUser = currentUser;
    }

    public List<ShiftSwapRequest> findAll() {
        UUID tenantId = TenantScope.require();
        return shiftSwapRequestStore.findAll().stream()
                .filter(request -> request.organizationId() != null && request.organizationId().equals(tenantId))
                .toList();
    }

    @Transactional
    public ShiftSwapRequest create(ShiftSwapRequestRequest request) {
        validateRequest(request);

        ShiftSwapRequest shiftSwapRequest = new ShiftSwapRequest(
                UUID.randomUUID(),
                TenantScope.require(),
                request.requestingEmployeeId(),
                request.targetEmployeeId(),
                request.offeredDate(),
                request.requestedDate(),
                request.reason().trim(),
                ShiftSwapRequestStatus.PENDING);

        shiftSwapRequestStore.save(shiftSwapRequest);
        approvalEngine.register(shiftSwapRequest.id(), "SHIFT_SWAP", request.requestingEmployeeId(), currentUser.username());
        return shiftSwapRequest;
    }

    @Transactional
    public ShiftSwapRequest approve(UUID id) {
        ShiftSwapRequest shiftSwapRequest = findById(id);
        if (shiftSwapRequest.status() != ShiftSwapRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING shift swap requests can be approved");
        }
        approvalEngine.decide(id, ApprovalDecision.APPROVED, currentUser.username(), null);
        ShiftSwapRequest approved = new ShiftSwapRequest(
                shiftSwapRequest.id(),
                shiftSwapRequest.organizationId(),
                shiftSwapRequest.requestingEmployeeId(),
                shiftSwapRequest.targetEmployeeId(),
                shiftSwapRequest.offeredDate(),
                shiftSwapRequest.requestedDate(),
                shiftSwapRequest.reason(),
                ShiftSwapRequestStatus.APPROVED);
        return shiftSwapRequestStore.save(approved);
    }

    @Transactional
    public ShiftSwapRequest reject(UUID id) {
        ShiftSwapRequest shiftSwapRequest = findById(id);
        if (shiftSwapRequest.status() != ShiftSwapRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING shift swap requests can be rejected");
        }
        approvalEngine.decide(id, ApprovalDecision.REJECTED, currentUser.username(), null);
        ShiftSwapRequest rejected = new ShiftSwapRequest(
                shiftSwapRequest.id(),
                shiftSwapRequest.organizationId(),
                shiftSwapRequest.requestingEmployeeId(),
                shiftSwapRequest.targetEmployeeId(),
                shiftSwapRequest.offeredDate(),
                shiftSwapRequest.requestedDate(),
                shiftSwapRequest.reason(),
                ShiftSwapRequestStatus.REJECTED);
        return shiftSwapRequestStore.save(rejected);
    }

    public ShiftSwapRequest findById(UUID id) {
        return shiftSwapRequestStore.findById(id)
                .map(request -> {
                    TenantScope.assertAccess(request.organizationId());
                    return request;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift swap request not found"));
    }

    private void validateRequest(ShiftSwapRequestRequest request) {
        if (request == null
                || request.requestingEmployeeId() == null
                || request.targetEmployeeId() == null
                || request.offeredDate() == null
                || request.requestedDate() == null
                || ValidationUtils.isBlank(request.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Requesting employee, target employee, offered date, requested date, and reason are required");
        }
        if (request.requestingEmployeeId().equals(request.targetEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employees must be different for a shift swap");
        }
        if (request.offeredDate().isBefore(LocalDate.now()) || request.requestedDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Swap dates cannot be in the past");
        }
    }
}