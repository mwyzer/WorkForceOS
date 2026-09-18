package com.workforceos.organization;

import java.time.OffsetDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures the seeded default organization exists regardless of how the schema was created
 * (Flyway in production, Hibernate create-drop in tests). Bootstraps configuration so
 * tenant resolution always has a stable root.
 */
@Component
public class OrganizationBootstrap implements ApplicationRunner {

    private final OrganizationRepository organizationRepository;

    public OrganizationBootstrap(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (organizationRepository.existsById(OrganizationService.DEFAULT_ORGANIZATION_ID)) {
            return;
        }
        organizationRepository.saveAndFlush(new OrganizationEntity(
                OrganizationService.DEFAULT_ORGANIZATION_ID,
                "Default Organization",
                "UTC",
                true,
                OffsetDateTime.now()));
    }
}