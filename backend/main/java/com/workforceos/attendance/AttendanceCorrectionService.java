package com.workforceos.attendance;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.approval.ApprovalDecision;
import com.workforceos.approval.ApprovalEngine;
import com.workforceos.shared.CurrentUser;
import com.workforceos.shared.ValidationUtils;

@Service
public class AttendanceCorrectionService {

    private final AttendanceCorrectionStore correctionStore;
    private final ApprovalEngine approvalEngine;
    private final CurrentUser currentUser;

    public AttendanceCorrectionService(AttendanceCorrectionStore correctionStore, ApprovalEngine approvalEngine,
            CurrentUser currentUser) {
        this.correctionStore = correctionStore;
        this.approvalEngine = approvalEngine;
        this.currentUser = currentUser;
    }

    public List<AttendanceCorrection> findAll() {
        return correctionStore.findAll();
    }

    @Transactional
    public AttendanceCorrection create(AttendanceCorrectionRequest request) {
        validateRequest(request);

        AttendanceCorrection attendanceCorrection = new AttendanceCorrection(
                UUID.randomUUID(),
                request.employeeId(),
                request.attendanceId(),
                request.type(),
                request.details().trim(),
                AttendanceCorrectionStatus.PENDING);

        correctionStore.save(attendanceCorrection);
        approvalEngine.register(attendanceCorrection.id(), "ATTENDANCE_CORRECTION", attendanceCorrection.employeeId(),
                currentUser.username());
        return attendanceCorrection;
    }

    @Transactional
    public AttendanceCorrection approve(UUID id) {
        AttendanceCorrection attendanceCorrection = findById(id);
        if (attendanceCorrection.status() != AttendanceCorrectionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING attendance corrections can be approved");
        }
        approvalEngine.decide(id, ApprovalDecision.APPROVED, currentUser.username(), null);
        AttendanceCorrection approved = new AttendanceCorrection(
                attendanceCorrection.id(),
                attendanceCorrection.employeeId(),
                attendanceCorrection.attendanceId(),
                attendanceCorrection.type(),
                attendanceCorrection.details(),
                AttendanceCorrectionStatus.APPROVED);
        return correctionStore.save(approved);
    }

    @Transactional
    public AttendanceCorrection reject(UUID id) {
        AttendanceCorrection attendanceCorrection = findById(id);
        if (attendanceCorrection.status() != AttendanceCorrectionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING attendance corrections can be rejected");
        }
        approvalEngine.decide(id, ApprovalDecision.REJECTED, currentUser.username(), null);
        AttendanceCorrection rejected = new AttendanceCorrection(
                attendanceCorrection.id(),
                attendanceCorrection.employeeId(),
                attendanceCorrection.attendanceId(),
                attendanceCorrection.type(),
                attendanceCorrection.details(),
                AttendanceCorrectionStatus.REJECTED);
        return correctionStore.save(rejected);
    }

    public AttendanceCorrection findById(UUID id) {
        return correctionStore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance correction not found"));
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