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
class AttendanceCorrectionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndApprovesAttendanceCorrection() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();

        String request = """
                {
                  "employeeId": "%s",
                  "attendanceId": "%s",
                  "type": "CLOCK_OUT",
                  "details": "Forgot to clock out before leaving site"
                }
                """.formatted(employeeId, attendanceId);

        String location = mockMvc.perform(post("/api/v1/attendance-corrections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/v1/attendance-corrections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/attendance-corrections/{id}/approve", location.substring(location.lastIndexOf('/') + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectsCorrectionWithoutDetails() throws Exception {
        String request = """
                {
                  "employeeId": "%s",
                  "attendanceId": "%s",
                  "type": "CLOCK_OUT",
                  "details": ""
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/attendance-corrections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
