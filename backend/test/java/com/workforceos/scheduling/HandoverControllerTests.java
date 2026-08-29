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
class HandoverControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsSubmitsAndAcknowledgesHandover() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String createRequest = """
                {
                  "employeeId": "%s",
                  "items": [
                    {
                      "type": "OPERATIONAL_SUMMARY",
                      "content": "All systems operational"
                    },
                    {
                      "type": "OUTSTANDING_TASKS",
                      "content": "Complete report by end of shift"
                    }
                  ]
                }
                """.formatted(employeeId);

        String location = mockMvc.perform(post("/api/v1/handovers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/v1/handovers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/handovers/{id}/submit", location.substring(location.lastIndexOf('/') + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/v1/handovers/{id}/acknowledge", location.substring(location.lastIndexOf('/') + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void rejectsHandoverWithNoItems() throws Exception {
        String request = """
                {
                  "employeeId": "%s",
                  "items": []
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/handovers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
