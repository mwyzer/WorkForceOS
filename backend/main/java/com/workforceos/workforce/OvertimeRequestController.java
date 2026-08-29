package com.workforceos.workforce;

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
public class OvertimeRequestController {

    private final OvertimeRequestService overtimeRequestService;

    public OvertimeRequestController(OvertimeRequestService overtimeRequestService) {
        this.overtimeRequestService = overtimeRequestService;
    }

    @GetMapping("/overtime-requests")
    public List<OvertimeRequest> findAll() {
        return overtimeRequestService.findAll();
    }

    @PostMapping("/overtime-requests")
    public ResponseEntity<OvertimeRequest> create(@RequestBody OvertimeRequestRequest request) {
        OvertimeRequest overtimeRequest = overtimeRequestService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/overtime-requests/" + overtimeRequest.id())).body(overtimeRequest);
    }

    @PostMapping("/overtime-requests/{id}/approve")
    public OvertimeRequest approve(@PathVariable UUID id) {
        return overtimeRequestService.approve(id);
    }

    @PostMapping("/overtime-requests/{id}/reject")
    public OvertimeRequest reject(@PathVariable UUID id) {
        return overtimeRequestService.reject(id);
    }
}
