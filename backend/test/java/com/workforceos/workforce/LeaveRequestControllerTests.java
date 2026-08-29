package com.workforceos.workforce;

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
class LeaveRequestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndApprovesLeaveRequest() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String request = """
                {
                  "employeeId": "%s",
                  "startDate": "2026-09-10",
                  "endDate": "2026-09-12",
                  "reason": "Family emergency"
                }
                """.formatted(employeeId);

        String location = mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/v1/leave-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/leave-requests/{id}/approve", location.substring(location.lastIndexOf('/') + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectsInvalidLeaveDateRange() throws Exception {
        String request = """
                {
                  "employeeId": "%s",
                  "startDate": "2026-09-15",
                  "endDate": "2026-09-12",
                  "reason": "Invalid date range"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
