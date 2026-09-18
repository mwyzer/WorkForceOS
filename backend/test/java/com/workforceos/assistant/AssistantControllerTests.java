package com.workforceos.assistant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AssistantControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void answersHeadcountQuestionsOffline() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How many employees do we have?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("heuristic"))
                .andExpect(jsonPath("$.topics", hasItem("HEADCOUNT")))
                .andExpect(jsonPath("$.answer", containsString("0 employees")));
    }

    @Test
    void answersRiskQuestions() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What are the open risk alerts?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics", hasItem("RISK")))
                .andExpect(jsonPath("$.answer", containsString("risk index")));
    }

    @Test
    void fallsBackToGeneralOverview() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Give me a briefing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics", hasItem("GENERAL")))
                .andExpect(jsonPath("$.answer", containsString("Workforce overview")));
    }

    @Test
    void rejectsBlankQuestions() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}