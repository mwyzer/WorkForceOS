package com.workforceos.scheduling;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.OvertimeRequestService;

@Service
public class ReportService {

    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;
    private final AuditLogService auditLogService;

    public ReportService(LeaveRequestService leaveRequestService,
            OvertimeRequestService overtimeRequestService,
            AuditLogService auditLogService) {
        this.leaveRequestService = leaveRequestService;
        this.overtimeRequestService = overtimeRequestService;
        this.auditLogService = auditLogService;
    }

    public LeaveRequestReport getLeaveRequestReport() {
        List<?> allRequests = leaveRequestService.findAll();
        long pending = allRequests.stream()
                .filter(r -> {
                    if (r instanceof com.workforceos.workforce.LeaveRequest lr) {
                        return lr.status() == com.workforceos.workforce.LeaveRequestStatus.PENDING;
                    }
                    return false;
                })
                .count();

        long approved = allRequests.stream()
                .filter(r -> {
                    if (r instanceof com.workforceos.workforce.LeaveRequest lr) {
                        return lr.status() == com.workforceos.workforce.LeaveRequestStatus.APPROVED;
                    }
                    return false;
                })
                .count();

        long rejected = allRequests.stream()
                .filter(r -> {
                    if (r instanceof com.workforceos.workforce.LeaveRequest lr) {
                        return lr.status() == com.workforceos.workforce.LeaveRequestStatus.REJECTED;
                    }
                    return false;
                })
                .count();

        return new LeaveRequestReport(pending, approved, rejected);
    }

    public OvertimeRequestReport getOvertimeRequestReport() {
        List<?> allRequests = overtimeRequestService.findAll();
        long total = allRequests.size();

        long approved = allRequests.stream()
                .filter(r -> {
                    if (r instanceof com.workforceos.workforce.OvertimeRequest or) {
                        return or.status() == com.workforceos.workforce.OvertimeRequestStatus.APPROVED;
                    }
                    return false;
                })
                .count();

        long rejected = allRequests.stream()
                .filter(r -> {
                    if (r instanceof com.workforceos.workforce.OvertimeRequest or) {
                        return or.status() == com.workforceos.workforce.OvertimeRequestStatus.REJECTED;
                    }
                    return false;
                })
                .count();

        long pending = allRequests.stream()
                .filter(r -> {
                    if (r instanceof com.workforceos.workforce.OvertimeRequest or) {
                        return or.status() == com.workforceos.workforce.OvertimeRequestStatus.PENDING;
                    }
                    return false;
                })
                .count();

        Double totalHours = allRequests.stream()
                .filter(r -> r instanceof com.workforceos.workforce.OvertimeRequest)
                .mapToDouble(r -> ((com.workforceos.workforce.OvertimeRequest) r).hours())
                .sum();

        return new OvertimeRequestReport(total, approved, rejected, pending, totalHours);
    }

    public AttendanceReport getAttendanceReport() {
        // Placeholder for attendance calculation
        // In production, this would aggregate actual attendance records
        return new AttendanceReport(0, 0, 0, 0, 0);
    }

    public AuditLogSummary getAuditLogSummary() {
        List<AuditLog> allLogs = auditLogService.findAll();
        long totalLogs = allLogs.size();

        Set<String> distinctActors = allLogs.stream()
                .map(AuditLog::actor)
                .collect(Collectors.toSet());

        Set<String> distinctActions = allLogs.stream()
                .map(AuditLog::action)
                .collect(Collectors.toSet());

        Set<String> distinctResources = allLogs.stream()
                .map(AuditLog::resource)
                .collect(Collectors.toSet());

        return new AuditLogSummary(totalLogs, distinctActors.size(), distinctActions.size(), distinctResources.size());
    }
}
