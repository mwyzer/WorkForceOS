package com.workforceos.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
@TestPropertySource(properties = {
        "spring.security.enabled=false",
        "workforce.api-rate-limit.enabled=true",
        "workforce.api-rate-limit.requests-per-minute=2" })
class RateLimitFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsRequestsWithinTheWindow() throws Exception {
        mockMvc.perform(get("/api/v1/reports/leave-requests"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/leave-requests"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsRequestsExceedingTheWindowWithRetryAfter() throws Exception {
        mockMvc.perform(get("/api/v1/reports/leave-requests"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/leave-requests"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/leave-requests"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    void neverRateLimitsHealthEndpoint() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk());
        }
    }
}