package com.workforceos.scheduling;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workforceos.attendance.AttendanceReport;

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

    @GetMapping("/attendance-by-employee")
    public List<AttendanceByEmployeeReport> getAttendanceByEmployeeReport() {
        return reportService.getAttendanceByEmployeeReport();
    }

    @GetMapping("/overtime-by-employee")
    public List<OvertimeByEmployeeReport> getOvertimeByEmployeeReport() {
        return reportService.getOvertimeByEmployeeReport();
    }

    @GetMapping("/department-staffing")
    public List<DepartmentStaffingReport> getDepartmentStaffingReport() {
        return reportService.getDepartmentStaffingReport();
    }
}
