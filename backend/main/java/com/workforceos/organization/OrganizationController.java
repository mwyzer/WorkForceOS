package com.workforceos.organization;

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

import com.workforceos.shared.CurrentUser;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final CurrentUser currentUser;

    public OrganizationController(OrganizationService organizationService, CurrentUser currentUser) {
        this.organizationService = organizationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Organization> findAll() {
        currentUser.requireAdmin();
        return organizationService.findAll();
    }

    @GetMapping("/{id}")
    public Organization findById(@PathVariable UUID id) {
        currentUser.requireAdmin();
        return organizationService.findById(id).orElseThrow();
    }

    @GetMapping("/current")
    public Organization current() {
        return organizationService.findById(TenantContext.require()).orElseGet(organizationService::getDefault);
    }

    @PostMapping
    public ResponseEntity<Organization> create(@RequestBody OrganizationRequest request) {
        currentUser.requireAdmin();
        Organization organization = organizationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/organizations/" + organization.id())).body(organization);
    }

    @PatchMapping("/{id}")
    public Organization update(@PathVariable UUID id, @RequestBody OrganizationUpdateRequest request) {
        currentUser.requireAdmin();
        return organizationService.update(id,
                request == null ? null : request.name(),
                request == null ? null : request.timezone(),
                request == null ? null : request.active());
    }
}