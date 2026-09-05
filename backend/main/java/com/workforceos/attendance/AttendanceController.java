package com.workforceos.attendance;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceEngine attendanceEngine;

    public AttendanceController(AttendanceEngine attendanceEngine) {
        this.attendanceEngine = attendanceEngine;
    }

    @PostMapping("/clock-in")
    public AttendanceSession clockIn(@RequestBody AttendanceRequest request) {
        return attendanceEngine.clockIn(request);
    }

    @PostMapping("/clock-out")
    public AttendanceSession clockOut(@RequestBody AttendanceRequest request) {
        return attendanceEngine.clockOut(request);
    }

    @GetMapping("/me")
    public List<AttendanceSession> findMyAttendance(@RequestParam UUID employeeId) {
        return attendanceEngine.findByEmployee(employeeId);
    }
}
