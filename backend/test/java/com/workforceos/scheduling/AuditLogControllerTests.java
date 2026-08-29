package com.workforceos.scheduling;

import static org.hamcrest.Matchers.hasSize;
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
class AuditLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsAuditLogs() throws Exception {
        UUID resourceId = UUID.randomUUID();
        String request = """
                {
                  "actor": "Supervisor",
                  "action": "APPROVE_OVERTIME",
                  "resource": "OvertimeRequest",
                  "resourceId": "%s",
                  "details": "Approved overtime request for coverage"
                }
                """.formatted(resourceId);

        String location = mockMvc.perform(post("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actor").value("Supervisor"))
                .andExpect(jsonPath("$.action").value("APPROVE_OVERTIME"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resource").value("OvertimeRequest"));

        mockMvc.perform(post("/api/v1/audit-logs/{id}", location.substring(location.lastIndexOf('/') + 1)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsAuditLogWithoutAction() throws Exception {
        String request = """
                {
                  "actor": "Supervisor",
                  "action": "",
                  "resource": "OvertimeRequest",
                  "resourceId": "%s",
                  "details": "Missing action"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
