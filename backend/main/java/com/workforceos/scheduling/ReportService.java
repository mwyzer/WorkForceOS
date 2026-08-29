package com.workforceos.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.LeaveRequestService;
import com.workforceos.workforce.LeaveRequestStatus;
import com.workforceos.workforce.OvertimeRequest;
import com.workforceos.workforce.OvertimeRequestService;
import com.workforceos.workforce.OvertimeRequestStatus;

@Service
public class ReportService {

    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;
    private final AuditLogService auditLogService;
    private final AttendanceService attendanceService;
    private final RosterService rosterService;

    public ReportService(LeaveRequestService leaveRequestService,
            OvertimeRequestService overtimeRequestService,
            AuditLogService auditLogService,
            AttendanceService attendanceService,
            RosterService rosterService) {
        this.leaveRequestService = leaveRequestService;
        this.overtimeRequestService = overtimeRequestService;
        this.auditLogService = auditLogService;
        this.attendanceService = attendanceService;
        this.rosterService = rosterService;
    }

    @Cacheable("leave-request-report")
    public LeaveRequestReport getLeaveRequestReport() {
        Map<LeaveRequestStatus, Long> counts = leaveRequestService.findAll().stream()
                .collect(Collectors.groupingBy(LeaveRequest::status, Collectors.counting()));

        return new LeaveRequestReport(
                counts.getOrDefault(LeaveRequestStatus.PENDING, 0L),
                counts.getOrDefault(LeaveRequestStatus.APPROVED, 0L),
                counts.getOrDefault(LeaveRequestStatus.REJECTED, 0L));
    }

    @Cacheable("overtime-request-report")
    public OvertimeRequestReport getOvertimeRequestReport() {
        List<OvertimeRequest> allRequests = overtimeRequestService.findAll();
        Map<OvertimeRequestStatus, Long> counts = allRequests.stream()
                .collect(Collectors.groupingBy(OvertimeRequest::status, Collectors.counting()));
        double totalHours = allRequests.stream().mapToDouble(OvertimeRequest::hours).sum();

        return new OvertimeRequestReport(
                allRequests.size(),
                counts.getOrDefault(OvertimeRequestStatus.APPROVED, 0L),
                counts.getOrDefault(OvertimeRequestStatus.REJECTED, 0L),
                counts.getOrDefault(OvertimeRequestStatus.PENDING, 0L),
                totalHours);
    }

    @Cacheable("attendance-report")
    public AttendanceReport getAttendanceReport() {
        List<AttendanceSession> sessions = attendanceService.findAllSessions();
        List<RosterAssignment> assignments = rosterService.findAllAssignments();

        long present = sessions.size();
        long late = 0;
        long earlyLeave = 0;
        long overtime = 0;

        for (AttendanceSession session : sessions) {
            RosterAssignment assignment = matchAssignment(session, assignments);
            if (assignment == null) {
                continue;
            }
            if (session.clockInAt().isAfter(assignment.start())) {
                late++;
            }
            if (session.clockOutAt() != null) {
                if (session.clockOutAt().isBefore(assignment.end())) {
                    earlyLeave++;
                } else if (session.clockOutAt().isAfter(assignment.end())) {
                    overtime++;
                }
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        long absent = assignments.stream()
                .filter(RosterAssignment::active)
                .filter(assignment -> assignment.end().isBefore(now))
                .filter(assignment -> sessions.stream().noneMatch(session -> matchesAssignment(session, assignment)))
                .count();

        return new AttendanceReport(present, late, absent, earlyLeave, overtime);
    }

    private RosterAssignment matchAssignment(AttendanceSession session, List<RosterAssignment> assignments) {
        return assignments.stream()
                .filter(assignment -> matchesAssignment(session, assignment))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesAssignment(AttendanceSession session, RosterAssignment assignment) {
        return session.rosterId().equals(assignment.rosterId())
                && session.employeeId().equals(assignment.employeeId())
                && session.shiftTemplateId().equals(assignment.shiftTemplateId());
    }

    @Cacheable("audit-log-summary")
    public AuditLogSummary getAuditLogSummary() {
        List<AuditLog> allLogs = auditLogService.findAll();

        Set<String> distinctActors = allLogs.stream().map(AuditLog::actor).collect(Collectors.toSet());
        Set<String> distinctActions = allLogs.stream().map(AuditLog::action).collect(Collectors.toSet());
        Set<String> distinctResources = allLogs.stream().map(AuditLog::resource).collect(Collectors.toSet());

        return new AuditLogSummary(allLogs.size(), distinctActors.size(), distinctActions.size(), distinctResources.size());
    }
}
