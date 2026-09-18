package com.workforceos.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import com.workforceos.bootstrap.WorkforceOsApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class OrganizationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesSeededDefaultOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Default Organization"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void currentTenantResolvesToDefaultOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OrganizationService.DEFAULT_ORGANIZATION_ID.toString()));
    }

    @Test
    void tenantContextIsolationContract() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        TenantContext.set(first);
        assertEquals(first, TenantContext.require());

        TenantContext.set(second);
        assertEquals(second, TenantContext.require());

        TenantContext.clear();
        assertTrue(TenantContext.current().isEmpty());
        assertEquals(OrganizationService.DEFAULT_ORGANIZATION_ID, TenantContext.require());
    }
}