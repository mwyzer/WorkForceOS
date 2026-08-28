package com.workforceos.scheduling;

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

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    @GetMapping
    public List<ShiftTemplate> findAll() {
        return shiftTemplateService.findAll();
    }

    @GetMapping("/{id}")
    public ShiftTemplate findById(@PathVariable UUID id) {
        return shiftTemplateService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ShiftTemplate> create(@RequestBody ShiftTemplateRequest request) {
        ShiftTemplate shift = shiftTemplateService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/shifts/" + shift.id())).body(shift);
    }

    @PatchMapping("/{id}")
    public ShiftTemplate update(@PathVariable UUID id, @RequestBody ShiftTemplateRequest request) {
        return shiftTemplateService.update(id, request);
    }
}
