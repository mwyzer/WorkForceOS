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
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @GetMapping("/rosters")
    public List<Roster> findAll() {
        return rosterService.findAll();
    }

    @GetMapping("/rosters/{id}")
    public Roster findById(@PathVariable UUID id) {
        return rosterService.findById(id);
    }

    @PostMapping("/rosters")
    public ResponseEntity<Roster> create(@RequestBody RosterRequest request) {
        Roster roster = rosterService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/rosters/" + roster.id())).body(roster);
    }

    @PostMapping("/rosters/{id}/publish")
    public Roster publish(@PathVariable UUID id) {
        return rosterService.publish(id);
    }

    @GetMapping("/rosters/{id}/assignments")
    public List<RosterAssignment> findAssignments(@PathVariable UUID id) {
        return rosterService.findAssignments(id);
    }

    @PostMapping("/rosters/{id}/assignments")
    public ResponseEntity<RosterAssignment> addAssignment(@PathVariable UUID id,
            @RequestBody RosterAssignmentRequest request) {
        RosterAssignment assignment = rosterService.addAssignment(id, request);
        return ResponseEntity.created(URI.create("/api/v1/rosters/" + id + "/assignments/" + assignment.id()))
                .body(assignment);
    }
}
