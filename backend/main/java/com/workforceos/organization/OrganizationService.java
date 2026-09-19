package com.workforceos.organization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

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

    public Organization create(OrganizationRequest request) {
        if (request == null
                || ValidationUtils.isBlank(request.name())
                || ValidationUtils.isBlank(request.timezone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name and timezone are required");
        }
        OrganizationEntity organization = new OrganizationEntity(
                UUID.randomUUID(),
                request.name().trim(),
                request.timezone().trim(),
                true,
                OffsetDateTime.now());
        organizationRepository.save(organization);
        return organization.toRecord();
    }

    public Organization update(UUID id, String name, String timezone, Boolean active) {
        OrganizationEntity organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        if ((name == null || name.isBlank()) && (timezone == null || timezone.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name or timezone is required");
        }
        organization.update(
                (name == null || name.isBlank()) ? null : name.trim(),
                (timezone == null || timezone.isBlank()) ? null : timezone.trim(),
                active);
        organizationRepository.save(organization);
        return organization.toRecord();
    }
}