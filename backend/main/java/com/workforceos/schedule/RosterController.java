package com.workforceos.schedule;

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

import com.workforceos.scheduling.AutoSchedulePlan;
import com.workforceos.scheduling.AutoScheduleService;

@RestController
@RequestMapping("/api/v1")
public class RosterController {

    private final ScheduleEngine scheduleEngine;
    private final AutoScheduleService autoScheduleService;

    public RosterController(ScheduleEngine scheduleEngine, AutoScheduleService autoScheduleService) {
        this.scheduleEngine = scheduleEngine;
        this.autoScheduleService = autoScheduleService;
    }

    @GetMapping("/rosters")
    public List<Roster> findAll() {
        return scheduleEngine.findAllRosters();
    }

    @GetMapping("/rosters/conflicts")
    public List<ScheduleConflict> findConflicts() {
        return scheduleEngine.findConflicts();
    }

    @GetMapping("/rosters/{id}")
    public Roster findById(@PathVariable UUID id) {
        return scheduleEngine.findRoster(id);
    }

    @PostMapping("/rosters")
    public ResponseEntity<Roster> create(@RequestBody RosterRequest request) {
        Roster roster = scheduleEngine.createRoster(request);
        return ResponseEntity.created(URI.create("/api/v1/rosters/" + roster.id())).body(roster);
    }

    @PostMapping("/rosters/{id}/publish")
    public Roster publish(@PathVariable UUID id) {
        return scheduleEngine.publishRoster(id);
    }

    @GetMapping("/rosters/{id}/assignments")
    public List<RosterAssignment> findAssignments(@PathVariable UUID id) {
        return scheduleEngine.findAssignments(id);
    }

    @PostMapping("/rosters/{id}/assignments")
    public ResponseEntity<RosterAssignment> addAssignment(@PathVariable UUID id,
            @RequestBody RosterAssignmentRequest request) {
        RosterAssignment assignment = scheduleEngine.addAssignment(id, request);
        return ResponseEntity.created(URI.create("/api/v1/rosters/" + id + "/assignments/" + assignment.id()))
                .body(assignment);
    }

    @GetMapping("/rosters/{id}/auto-schedule")
    public AutoSchedulePlan autoSchedule(@PathVariable UUID id) {
        return autoScheduleService.propose(id);
    }
}