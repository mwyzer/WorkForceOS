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
public class HandoverController {

    private final HandoverService handoverService;

    public HandoverController(HandoverService handoverService) {
        this.handoverService = handoverService;
    }

    @GetMapping("/handovers")
    public List<Handover> findAll() {
        return handoverService.findAll();
    }

    @PostMapping("/handovers")
    public ResponseEntity<Handover> create(@RequestBody HandoverRequest request) {
        Handover handover = handoverService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/handovers/" + handover.id())).body(handover);
    }

    @PostMapping("/handovers/{id}/submit")
    public Handover submit(@PathVariable UUID id) {
        return handoverService.submit(id);
    }

    @PostMapping("/handovers/{id}/acknowledge")
    public Handover acknowledge(@PathVariable UUID id) {
        return handoverService.acknowledge(id);
    }
}
