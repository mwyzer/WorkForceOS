package com.workforceos.organization;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<Organization> findAll() {
        return organizationService.findAll();
    }

    @GetMapping("/{id}")
    public Organization findById(@PathVariable UUID id) {
        return organizationService.findById(id).orElseThrow();
    }

    @GetMapping("/current")
    public Organization current() {
        return organizationService.findById(TenantContext.require()).orElseGet(organizationService::getDefault);
    }
}