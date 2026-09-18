package com.workforceos.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationService {

    public static final UUID DEFAULT_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<Organization> findAll() {
        return organizationRepository.findAll().stream().map(OrganizationEntity::toRecord).toList();
    }

    public Optional<Organization> findById(UUID id) {
        return organizationRepository.findById(id).map(OrganizationEntity::toRecord);
    }

    public Organization getDefault() {
        return findById(DEFAULT_ORGANIZATION_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Default organization is not configured"));
    }
}