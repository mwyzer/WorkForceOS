package com.workforceos.scheduling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.workforceos.bootstrap.WorkforceOsApplication;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.security.enabled=false")
class ShiftTemplateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsOvernightShiftWithBreaks() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Night",
                  "startTime": "22:00:00",
                  "endTime": "06:00:00",
                  "breaks": [{"start": "02:00:00", "end": "02:30:00"}]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Night"))
                .andExpect(jsonPath("$.overnight").value(true))
                .andExpect(jsonPath("$.breaks[0].start").value("02:00:00"));

        mockMvc.perform(get("/api/v1/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void rejectsShiftWithEqualStartAndEnd() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Invalid",
                  "startTime": "08:00:00",
                  "endTime": "08:00:00"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
