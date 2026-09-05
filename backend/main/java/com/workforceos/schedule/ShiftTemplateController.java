package com.workforceos.schedule;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftTemplateController {

    private final ScheduleEngine scheduleEngine;

    public ShiftTemplateController(ScheduleEngine scheduleEngine) {
        this.scheduleEngine = scheduleEngine;
    }

    @GetMapping
    public List<ShiftTemplate> findAll() {
        return scheduleEngine.findAllShifts();
    }

    @GetMapping("/{id}")
    public ShiftTemplate findById(@PathVariable UUID id) {
        return scheduleEngine.findShift(id);
    }

    @PostMapping
    public ResponseEntity<ShiftTemplate> create(@RequestBody ShiftTemplateRequest request) {
        ShiftTemplate shift = scheduleEngine.createShift(request);
        return ResponseEntity.created(URI.create("/api/v1/shifts/" + shift.id())).body(shift);
    }

    @PatchMapping("/{id}")
    public ShiftTemplate update(@PathVariable UUID id, @RequestBody ShiftTemplateRequest request) {
        return scheduleEngine.updateShift(id, request);
    }
}