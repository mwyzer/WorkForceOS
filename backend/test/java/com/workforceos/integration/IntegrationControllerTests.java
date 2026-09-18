package com.workforceos.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class IntegrationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsRegisteredIntegrations() throws Exception {
        mockMvc.perform(get("/api/v1/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("payroll-csv")))
                .andExpect(jsonPath("$[*].name", hasItem("webhook")));
    }

    @Test
    void rejectsBiometricIngestionWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/biometric/clock-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"employeeId\":\"" + UUID.randomUUID()
                                + "\",\"occurredAt\":\"2026-01-01T09:00:00Z\",\"direction\":\"IN\"}]"))
                .andExpect(status().isBadRequest());
    }
}