package com.workforceos.dashboard;

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
class DashboardOperationsTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesOperationsOverviewOnEmptyData() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingLeaveRequests").value(0))
                .andExpect(jsonPath("$.pendingOvertimeRequests").value(0))
                .andExpect(jsonPath("$.pendingHandovers").value(0))
                .andExpect(jsonPath("$.publishedRosters").value(0))
                .andExpect(jsonPath("$.recentNotifications").isArray())
                .andExpect(jsonPath("$.recentNotifications.length()").value(0));
    }
}