package com.workforceos.scheduling;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class ReportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesLeaveRequestReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/leave-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(0))
                .andExpect(jsonPath("$.approved").value(0))
                .andExpect(jsonPath("$.rejected").value(0));
    }

    @Test
    void providesOvertimeRequestReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overtime-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.approved").value(0))
                .andExpect(jsonPath("$.rejected").value(0))
                .andExpect(jsonPath("$.pending").value(0))
                .andExpect(jsonPath("$.totalHours").value(0.0));
    }

    @Test
    void providesAttendanceReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(0))
                .andExpect(jsonPath("$.late").value(0))
                .andExpect(jsonPath("$.absent").value(0))
                .andExpect(jsonPath("$.earlyLeave").value(0))
                .andExpect(jsonPath("$.overtime").value(0));
    }

    @Test
    void providesAuditLogReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/audit-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").value(0))
                .andExpect(jsonPath("$.distinctActors").value(0))
                .andExpect(jsonPath("$.distinctActions").value(0))
                .andExpect(jsonPath("$.distinctResources").value(0));
    }
}
