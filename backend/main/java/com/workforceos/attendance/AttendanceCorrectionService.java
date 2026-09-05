package com.workforceos.attendance;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class AttendanceCorrectionService {

    private final ConcurrentMap<UUID, AttendanceCorrection> attendanceCorrections = new ConcurrentHashMap<>();

    public List<AttendanceCorrection> findAll() {
        return attendanceCorrections.values().stream().toList();
    }

    public AttendanceCorrection create(AttendanceCorrectionRequest request) {
        validateRequest(request);

        AttendanceCorrection attendanceCorrection = new AttendanceCorrection(
                UUID.randomUUID(),
                request.employeeId(),
                request.attendanceId(),
                request.type(),
                request.details().trim(),
                AttendanceCorrectionStatus.PENDING);

        attendanceCorrections.put(attendanceCorrection.id(), attendanceCorrection);
        return attendanceCorrection;
    }

    public AttendanceCorrection approve(UUID id) {
        AttendanceCorrection attendanceCorrection = findById(id);
        if (attendanceCorrection.status() != AttendanceCorrectionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING attendance corrections can be approved");
        }
        AttendanceCorrection approved = new AttendanceCorrection(
                attendanceCorrection.id(),
                attendanceCorrection.employeeId(),
                attendanceCorrection.attendanceId(),
                attendanceCorrection.type(),
                attendanceCorrection.details(),
                AttendanceCorrectionStatus.APPROVED);
        attendanceCorrections.put(id, approved);
        return approved;
    }

    public AttendanceCorrection reject(UUID id) {
        AttendanceCorrection attendanceCorrection = findById(id);
        if (attendanceCorrection.status() != AttendanceCorrectionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING attendance corrections can be rejected");
        }
        AttendanceCorrection rejected = new AttendanceCorrection(
                attendanceCorrection.id(),
                attendanceCorrection.employeeId(),
                attendanceCorrection.attendanceId(),
                attendanceCorrection.type(),
                attendanceCorrection.details(),
                AttendanceCorrectionStatus.REJECTED);
        attendanceCorrections.put(id, rejected);
        return rejected;
    }

    public AttendanceCorrection findById(UUID id) {
        AttendanceCorrection attendanceCorrection = attendanceCorrections.get(id);
        if (attendanceCorrection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance correction not found");
        }
        return attendanceCorrection;
    }

    private void validateRequest(AttendanceCorrectionRequest request) {
        if (request == null
                || request.employeeId() == null
                || request.attendanceId() == null
                || request.type() == null
                || ValidationUtils.isBlank(request.details())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee, attendance, type, and details are required");
        }
    }
}
