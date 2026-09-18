package com.workforceos.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class AbsenteeismAnalyticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportsEmptyAbsenteeismInsights() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/absenteeism"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowDays").value(30))
                .andExpect(jsonPath("$.overallAbsenceRate").value(0.0))
                .andExpect(jsonPath("$.highRisk").value(0))
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.insights.length()").value(0));
    }

    @Test
    void acceptsCustomWindow() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/absenteeism").param("windowDays", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowDays").value(14));
    }
}