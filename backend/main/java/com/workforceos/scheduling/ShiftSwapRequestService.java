package com.workforceos.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShiftSwapRequestService {

    private final ConcurrentMap<UUID, ShiftSwapRequest> shiftSwapRequests = new ConcurrentHashMap<>();

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
        return shiftSwapRequest;
    }

    public ShiftSwapRequest approve(UUID id) {
        ShiftSwapRequest shiftSwapRequest = findById(id);
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
                || isBlank(request.reason())) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
