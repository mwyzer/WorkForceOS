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
public class ShiftSwapRequestController {

    private final ShiftSwapRequestService shiftSwapRequestService;

    public ShiftSwapRequestController(ShiftSwapRequestService shiftSwapRequestService) {
        this.shiftSwapRequestService = shiftSwapRequestService;
    }

    @GetMapping("/shift-swap-requests")
    public List<ShiftSwapRequest> findAll() {
        return shiftSwapRequestService.findAll();
    }

    @PostMapping("/shift-swap-requests")
    public ResponseEntity<ShiftSwapRequest> create(@RequestBody ShiftSwapRequestRequest request) {
        ShiftSwapRequest shiftSwapRequest = shiftSwapRequestService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/shift-swap-requests/" + shiftSwapRequest.id()))
                .body(shiftSwapRequest);
    }

    @PostMapping("/shift-swap-requests/{id}/approve")
    public ShiftSwapRequest approve(@PathVariable UUID id) {
        return shiftSwapRequestService.approve(id);
    }

    @PostMapping("/shift-swap-requests/{id}/reject")
    public ShiftSwapRequest reject(@PathVariable UUID id) {
        return shiftSwapRequestService.reject(id);
    }
}
