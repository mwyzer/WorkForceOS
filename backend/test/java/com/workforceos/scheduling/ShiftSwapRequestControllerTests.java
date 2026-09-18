package com.workforceos.scheduling;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
class ShiftSwapRequestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndApprovesShiftSwapRequest() throws Exception {
        UUID requestingEmployeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();

        String request = """
                {
                  "requestingEmployeeId": "%s",
                  "targetEmployeeId": "%s",
                  "offeredDate": "%s",
                  "requestedDate": "%s",
                  "reason": "Need to swap weekend coverage"
                }
                """.formatted(requestingEmployeeId, targetEmployeeId,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));

        String location = mockMvc.perform(post("/api/v1/shift-swap-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/v1/shift-swap-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/shift-swap-requests/{id}/approve", location.substring(location.lastIndexOf('/') + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectsRequestWithSameEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();

        String request = """
                {
                  "requestingEmployeeId": "%s",
                  "targetEmployeeId": "%s",
                  "offeredDate": "%s",
                  "requestedDate": "%s",
                  "reason": "Swap with self"
                }
                """.formatted(employeeId, employeeId,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));

        mockMvc.perform(post("/api/v1/shift-swap-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
