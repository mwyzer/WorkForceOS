package com.workforceos.scheduling;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AttendanceCorrectionController {

    private final AttendanceCorrectionService attendanceCorrectionService;

    public AttendanceCorrectionController(AttendanceCorrectionService attendanceCorrectionService) {
        this.attendanceCorrectionService = attendanceCorrectionService;
    }

    @GetMapping("/attendance-corrections")
    public List<AttendanceCorrection> findAll() {
        return attendanceCorrectionService.findAll();
    }

    @PostMapping("/attendance-corrections")
    public ResponseEntity<AttendanceCorrection> create(@RequestBody AttendanceCorrectionRequest request) {
        AttendanceCorrection attendanceCorrection = attendanceCorrectionService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/attendance-corrections/" + attendanceCorrection.id()))
                .body(attendanceCorrection);
    }

    @PostMapping("/attendance-corrections/{id}/approve")
    public AttendanceCorrection approve(@PathVariable UUID id) {
        return attendanceCorrectionService.approve(id);
    }

    @PostMapping("/attendance-corrections/{id}/reject")
    public AttendanceCorrection reject(@PathVariable UUID id) {
        return attendanceCorrectionService.reject(id);
    }
}
