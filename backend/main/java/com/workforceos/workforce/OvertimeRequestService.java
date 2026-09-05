package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.approval.ApprovalDecision;
import com.workforceos.approval.ApprovalEngine;
import com.workforceos.event.DomainEvents;
import com.workforceos.event.EventPublisher;
import com.workforceos.event.EventTypes;
import com.workforceos.event.Payloads;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.ValidationUtils;

@Service
public class OvertimeRequestService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final ApprovalEngine approvalEngine;
    private final EventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public OvertimeRequestService(OvertimeRequestRepository overtimeRequestRepository, ApprovalEngine approvalEngine,
            EventPublisher eventPublisher, CurrentUser currentUser) {
        this.overtimeRequestRepository = overtimeRequestRepository;
        this.approvalEngine = approvalEngine;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    @Cacheable("overtimeRequests")
    public List<OvertimeRequest> findAll() {
        return overtimeRequestRepository.findAll().stream().map(OvertimeRequestEntity::toRecord).toList();
    }

    @CacheEvict(value = { "overtimeRequests", "overtime-request-report" }, allEntries = true)
    public OvertimeRequest create(OvertimeRequestRequest request) {
        validateRequest(request);

        OvertimeRequestEntity overtimeRequest = new OvertimeRequestEntity(
                UUID.randomUUID(),
                request.employeeId(),
                request.date(),
                request.hours(),
                request.reason().trim(),
                OvertimeRequestStatus.PENDING);

        overtimeRequestRepository.save(overtimeRequest);
        approvalEngine.register(overtimeRequest.getId(), "OVERTIME", overtimeRequest.getEmployeeId(), currentUser.username());
        return overtimeRequest.toRecord();
    }

    @CacheEvict(value = { "overtimeRequests", "overtime-request-report" }, allEntries = true)
    public OvertimeRequest approve(UUID id) {
        OvertimeRequestEntity overtimeRequest = overtimeRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found"));
        if (overtimeRequest.getStatus() != OvertimeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING overtime requests can be approved");
        }
        approvalEngine.decide(id, ApprovalDecision.APPROVED, currentUser.username(), null);
        overtimeRequest.approve();
        overtimeRequestRepository.save(overtimeRequest);
        OvertimeRequest approved = overtimeRequest.toRecord();
        eventPublisher.publish(DomainEvents.of(EventTypes.OVERTIME_APPROVED, null, "OvertimeRequest", approved.id(),
                currentUser.username(),
                Payloads.json(java.util.Map.of(
                        "employeeId", approved.employeeId().toString(),
                        "date", approved.date().toString(),
                        "hours", approved.hours().toString()))));
        return approved;
    }

    @CacheEvict(value = { "overtimeRequests", "overtime-request-report" }, allEntries = true)
    public OvertimeRequest reject(UUID id) {
        OvertimeRequestEntity overtimeRequest = overtimeRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found"));
        if (overtimeRequest.getStatus() != OvertimeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING overtime requests can be rejected");
        }
        approvalEngine.decide(id, ApprovalDecision.REJECTED, currentUser.username(), null);
        overtimeRequest.reject();
        overtimeRequestRepository.save(overtimeRequest);
        OvertimeRequest rejected = overtimeRequest.toRecord();
        eventPublisher.publish(DomainEvents.of(EventTypes.OVERTIME_REJECTED, null, "OvertimeRequest", rejected.id(),
                currentUser.username(),
                Payloads.json(java.util.Map.of(
                        "employeeId", rejected.employeeId().toString(),
                        "date", rejected.date().toString(),
                        "hours", rejected.hours().toString()))));
        return rejected;
    }

    public OvertimeRequest findById(UUID id) {
        return overtimeRequestRepository.findById(id)
                .map(OvertimeRequestEntity::toRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found"));
    }

    private void validateRequest(OvertimeRequestRequest request) {
        if (request == null
                || request.employeeId() == null
                || request.date() == null
                || request.hours() == null
                || ValidationUtils.isBlank(request.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee, date, hours, and reason are required");
        }
        if (request.hours() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overtime hours must be greater than zero");
        }
        if (request.date().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overtime date cannot be in the past");
        }
    }
}
