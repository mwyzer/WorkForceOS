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
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsNotifications() throws Exception {
        UUID recipientId = UUID.randomUUID();
        String request = """
                {
                  "recipientId": "%s",
                  "type": "LEAVE_APPROVED",
                  "title": "Leave Request Approved",
                  "body": "Your leave request has been approved",
                  "channel": "IN_APP"
                }
                """.formatted(recipientId);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientId").value(recipientId.toString()))
                .andExpect(jsonPath("$.type").value("LEAVE_APPROVED"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Leave Request Approved"));
    }

    @Test
    void rejectsNotificationWithoutChannel() throws Exception {
        String request = """
                {
                  "recipientId": "%s",
                  "type": "LEAVE_APPROVED",
                  "title": "Leave Request Approved",
                  "body": "Your leave request has been approved",
                  "channel": ""
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
