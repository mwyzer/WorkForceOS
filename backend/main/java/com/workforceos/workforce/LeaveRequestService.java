package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class LeaveRequestService {

    private final ConcurrentMap<UUID, LeaveRequest> leaveRequests = new ConcurrentHashMap<>();

    public List<LeaveRequest> findAll() {
        return leaveRequests.values().stream().toList();
    }

    public LeaveRequest create(LeaveRequestRequest request) {
        validateRequest(request);

        LeaveRequest leaveRequest = new LeaveRequest(
                UUID.randomUUID(),
                request.employeeId(),
                request.startDate(),
                request.endDate(),
                request.reason().trim(),
                LeaveRequestStatus.PENDING);

        leaveRequests.put(leaveRequest.id(), leaveRequest);
        return leaveRequest;
    }

    public LeaveRequest approve(UUID id) {
        LeaveRequest leaveRequest = findById(id);
        if (leaveRequest.status() != LeaveRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING leave requests can be approved");
        }
        LeaveRequest approved = new LeaveRequest(
                leaveRequest.id(),
                leaveRequest.employeeId(),
                leaveRequest.startDate(),
                leaveRequest.endDate(),
                leaveRequest.reason(),
                LeaveRequestStatus.APPROVED);
        leaveRequests.put(id, approved);
        return approved;
    }

    public LeaveRequest findById(UUID id) {
        LeaveRequest leaveRequest = leaveRequests.get(id);
        if (leaveRequest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
        }
        return leaveRequest;
    }

    private void validateRequest(LeaveRequestRequest request) {
        if (request == null
                || request.employeeId() == null
                || request.startDate() == null
                || request.endDate() == null
                || ValidationUtils.isBlank(request.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee, dates, and reason are required");
        }
        if (!request.endDate().isAfter(request.startDate()) && !request.endDate().isEqual(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave end date must be on or after the start date");
        }
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave start date cannot be in the past");
        }
    }
}
