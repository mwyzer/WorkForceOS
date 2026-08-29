package com.workforceos.scheduling;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/leave-requests")
    public LeaveRequestReport getLeaveRequestReport() {
        return reportService.getLeaveRequestReport();
    }

    @GetMapping("/overtime-requests")
    public OvertimeRequestReport getOvertimeRequestReport() {
        return reportService.getOvertimeRequestReport();
    }

    @GetMapping("/attendance")
    public AttendanceReport getAttendanceReport() {
        return reportService.getAttendanceReport();
    }

    @GetMapping("/audit-summary")
    public AuditLogSummary getAuditLogSummary() {
        return reportService.getAuditLogSummary();
    }
}
