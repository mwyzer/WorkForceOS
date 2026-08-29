package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Cacheable("leaveRequests")
    public List<LeaveRequest> findAll() {
        return leaveRequestRepository.findAll().stream().map(LeaveRequestEntity::toRecord).toList();
    }

    @CacheEvict(value = { "leaveRequests", "leave-request-report" }, allEntries = true)
    public LeaveRequest create(LeaveRequestRequest request) {
        validateRequest(request);

        LeaveRequestEntity leaveRequest = new LeaveRequestEntity(
                UUID.randomUUID(),
                request.employeeId(),
                request.startDate(),
                request.endDate(),
                request.reason().trim(),
                LeaveRequestStatus.PENDING);

        leaveRequestRepository.save(leaveRequest);
        return leaveRequest.toRecord();
    }

    @CacheEvict(value = { "leaveRequests", "leave-request-report" }, allEntries = true)
    public LeaveRequest approve(UUID id) {
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING leave requests can be approved");
        }
        leaveRequest.approve();
        leaveRequestRepository.save(leaveRequest);
        return leaveRequest.toRecord();
    }

    public LeaveRequest findById(UUID id) {
        return leaveRequestRepository.findById(id)
                .map(LeaveRequestEntity::toRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
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
