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
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping("/leave-requests")
    public List<LeaveRequest> findAll() {
        return leaveRequestService.findAll();
    }

    @PostMapping("/leave-requests")
    public ResponseEntity<LeaveRequest> create(@RequestBody LeaveRequestRequest request) {
        LeaveRequest leaveRequest = leaveRequestService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/leave-requests/" + leaveRequest.id())).body(leaveRequest);
    }

    @PostMapping("/leave-requests/{id}/approve")
    public LeaveRequest approve(@PathVariable UUID id) {
        return leaveRequestService.approve(id);
    }
}
