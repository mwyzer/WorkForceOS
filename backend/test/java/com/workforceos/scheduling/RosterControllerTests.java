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
class RosterControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsRosterAndPublishesAValidSchedule() throws Exception {
        UUID rosterId = createRoster("Week 1");

        String createAssignment = """
                {
                  "employeeId": "%s",
                  "shiftTemplateId": "%s",
                  "start": "2026-09-01T08:00:00+00:00",
                  "end": "2026-09-01T16:00:00+00:00"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAssignment))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").exists());

        mockMvc.perform(post("/api/v1/rosters/{id}/publish", rosterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/rosters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsOverlappingAssignmentsForTheSameEmployee() throws Exception {
        UUID rosterId = createRoster("Week 2");
        UUID employeeId = UUID.randomUUID();

        String firstAssignment = """
                {
                  "employeeId": "%s",
                  "shiftTemplateId": "%s",
                  "start": "2026-09-02T08:00:00+00:00",
                  "end": "2026-09-02T16:00:00+00:00"
                }
                """.formatted(employeeId, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstAssignment))
                .andExpect(status().isCreated());

        String overlappingAssignment = """
                {
                  "employeeId": "%s",
                  "shiftTemplateId": "%s",
                  "start": "2026-09-02T15:00:00+00:00",
                  "end": "2026-09-02T17:00:00+00:00"
                }
                """.formatted(employeeId, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingAssignment))
                .andExpect(status().isConflict());
    }

    private UUID createRoster(String name) throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "%s"
                }
                """.formatted(UUID.randomUUID(), name);

        String location = mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
