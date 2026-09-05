package com.workforceos.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.approval.ApprovalDecision;
import com.workforceos.approval.ApprovalEngine;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.ValidationUtils;

@Service
public class ShiftSwapRequestService {

    private final ConcurrentMap<UUID, ShiftSwapRequest> shiftSwapRequests = new ConcurrentHashMap<>();
    private final ApprovalEngine approvalEngine;
    private final CurrentUser currentUser;

    public ShiftSwapRequestService(ApprovalEngine approvalEngine, CurrentUser currentUser) {
        this.approvalEngine = approvalEngine;
        this.currentUser = currentUser;
    }

    public List<ShiftSwapRequest> findAll() {
        return shiftSwapRequests.values().stream().toList();
    }

    public ShiftSwapRequest create(ShiftSwapRequestRequest request) {
        validateRequest(request);

        ShiftSwapRequest shiftSwapRequest = new ShiftSwapRequest(
                UUID.randomUUID(),
                request.requestingEmployeeId(),
                request.targetEmployeeId(),
                request.offeredDate(),
                request.requestedDate(),
                request.reason().trim(),
                ShiftSwapRequestStatus.PENDING);

        shiftSwapRequests.put(shiftSwapRequest.id(), shiftSwapRequest);
        approvalEngine.register(shiftSwapRequest.id(), "SHIFT_SWAP", request.requestingEmployeeId(), currentUser.username());
        return shiftSwapRequest;
    }

    public ShiftSwapRequest approve(UUID id) {
        ShiftSwapRequest shiftSwapRequest = findById(id);
        if (shiftSwapRequest.status() != ShiftSwapRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING shift swap requests can be approved");
        }
        approvalEngine.decide(id, ApprovalDecision.APPROVED, currentUser.username(), null);
        ShiftSwapRequest approved = new ShiftSwapRequest(
                shiftSwapRequest.id(),
                shiftSwapRequest.requestingEmployeeId(),
                shiftSwapRequest.targetEmployeeId(),
                shiftSwapRequest.offeredDate(),
                shiftSwapRequest.requestedDate(),
                shiftSwapRequest.reason(),
                ShiftSwapRequestStatus.APPROVED);
        shiftSwapRequests.put(id, approved);
        return approved;
    }

    public ShiftSwapRequest reject(UUID id) {
        ShiftSwapRequest shiftSwapRequest = findById(id);
        if (shiftSwapRequest.status() != ShiftSwapRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING shift swap requests can be rejected");
        }
        approvalEngine.decide(id, ApprovalDecision.REJECTED, currentUser.username(), null);
        ShiftSwapRequest rejected = new ShiftSwapRequest(
                shiftSwapRequest.id(),
                shiftSwapRequest.requestingEmployeeId(),
                shiftSwapRequest.targetEmployeeId(),
                shiftSwapRequest.offeredDate(),
                shiftSwapRequest.requestedDate(),
                shiftSwapRequest.reason(),
                ShiftSwapRequestStatus.REJECTED);
        shiftSwapRequests.put(id, rejected);
        return rejected;
    }

    public ShiftSwapRequest findById(UUID id) {
        ShiftSwapRequest shiftSwapRequest = shiftSwapRequests.get(id);
        if (shiftSwapRequest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift swap request not found");
        }
        return shiftSwapRequest;
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
