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
class DemandForecastControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void forecastsDefaultHorizon() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizonDays").value(7))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days.length()").value(7));
    }

    @Test
    void acceptsCustomHorizon() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/forecast").param("horizonDays", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizonDays").value(14))
                .andExpect(jsonPath("$.days.length()").value(14));
    }
}