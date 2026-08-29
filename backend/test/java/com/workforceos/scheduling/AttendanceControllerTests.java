package com.workforceos.scheduling;

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
class AttendanceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clockInRequiresEligibleAssignmentAndRejectsDuplicates() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID rosterId = createRoster();
        createAssignment(rosterId, employeeId, "2026-09-03T08:00:00+00:00", "2026-09-03T16:00:00+00:00");

        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "2026-09-03T08:15:00+00:00"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "2026-09-03T08:30:00+00:00"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isConflict());
    }

    @Test
    void clockOutClosesActiveSessionAndClockInWithoutAssignmentIsRejected() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID rosterId = createRoster();
        createAssignment(rosterId, employeeId, "2026-09-04T08:00:00+00:00", "2026-09-04T16:00:00+00:00");

        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "2026-09-04T08:10:00+00:00"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/attendance/clock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "2026-09-04T16:05:00+00:00"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "2026-09-05T08:00:00+00:00"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict());
    }

    private UUID createRoster() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Week Attendance"
                }
                """.formatted(UUID.randomUUID());

        String response = mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.split("\"id\":\"")[1].split("\"")[0];
        return UUID.fromString(id);
    }

    private void createAssignment(UUID rosterId, UUID employeeId, String start, String end) throws Exception {
        String request = """
                {
                  "employeeId": "%s",
                  "shiftTemplateId": "%s",
                  "start": "%s",
                  "end": "%s"
                }
                """.formatted(employeeId, UUID.randomUUID(), start, end);

        mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }
}
