package com.workforceos.workforce;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OvertimeRequestService {

    private final ConcurrentMap<UUID, OvertimeRequest> overtimeRequests = new ConcurrentHashMap<>();

    public List<OvertimeRequest> findAll() {
        return overtimeRequests.values().stream().toList();
    }

    public OvertimeRequest create(OvertimeRequestRequest request) {
        validateRequest(request);

        OvertimeRequest overtimeRequest = new OvertimeRequest(
                UUID.randomUUID(),
                request.employeeId(),
                request.date(),
                request.hours(),
                request.reason().trim(),
                OvertimeRequestStatus.PENDING);

        overtimeRequests.put(overtimeRequest.id(), overtimeRequest);
        return overtimeRequest;
    }

    public OvertimeRequest approve(UUID id) {
        OvertimeRequest overtimeRequest = findById(id);
        OvertimeRequest approved = new OvertimeRequest(
                overtimeRequest.id(),
                overtimeRequest.employeeId(),
                overtimeRequest.date(),
                overtimeRequest.hours(),
                overtimeRequest.reason(),
                OvertimeRequestStatus.APPROVED);
        overtimeRequests.put(id, approved);
        return approved;
    }

    public OvertimeRequest reject(UUID id) {
        OvertimeRequest overtimeRequest = findById(id);
        OvertimeRequest rejected = new OvertimeRequest(
                overtimeRequest.id(),
                overtimeRequest.employeeId(),
                overtimeRequest.date(),
                overtimeRequest.hours(),
                overtimeRequest.reason(),
                OvertimeRequestStatus.REJECTED);
        overtimeRequests.put(id, rejected);
        return rejected;
    }

    public OvertimeRequest findById(UUID id) {
        OvertimeRequest overtimeRequest = overtimeRequests.get(id);
        if (overtimeRequest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found");
        }
        return overtimeRequest;
    }

    private void validateRequest(OvertimeRequestRequest request) {
        if (request == null
                || request.employeeId() == null
                || request.date() == null
                || request.hours() == null
                || isBlank(request.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee, date, hours, and reason are required");
        }
        if (request.hours() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overtime hours must be greater than zero");
        }
        if (request.date().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overtime date cannot be in the past");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
