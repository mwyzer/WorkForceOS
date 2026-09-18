package com.workforceos.schedule;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class AutoScheduleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsNotFoundForUnknownRoster() throws Exception {
        mockMvc.perform(get("/api/v1/rosters/{id}/auto-schedule", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsEmptyPlanForRosterWithoutAssignments() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":\"" + UUID.randomUUID() + "\",\"name\":\"Week 1\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode roster = objectMapper.readTree(created.getResponse().getContentAsString());
        String rosterId = roster.get("id").asText();

        mockMvc.perform(get("/api/v1/rosters/{id}/auto-schedule", rosterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rosterId").value(rosterId))
                .andExpect(jsonPath("$.rosterName").value("Week 1"))
                .andExpect(jsonPath("$.targetHeadcount").value(1))
                .andExpect(jsonPath("$.understaffedSlots").value(0))
                .andExpect(jsonPath("$.proposals").isArray())
                .andExpect(jsonPath("$.proposals.length()").value(0));
    }
}