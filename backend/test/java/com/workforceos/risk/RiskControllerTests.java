package com.workforceos.risk;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
class RiskControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void detectsCoverageRiskAndCreatesAlertWhenLeaveIsApproved() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee(departmentId, teamId);
        UUID shiftTemplateId = createShiftTemplate();
        UUID rosterId = createRoster();
        addAssignment(rosterId, employeeId, shiftTemplateId);
        publishRoster(rosterId);

        String tomorrow = tomorrow();
        UUID leaveId = createLeaveRequest(employeeId, tomorrow);
        approveLeave(leaveId);

        mockMvc.perform(get("/api/v1/risk/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        mockMvc.perform(get("/api/v1/risk/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskIndex").value(85))
                .andExpect(jsonPath("$.totalAssessments").value(1))
                .andExpect(jsonPath("$.coveragePercentage").value(0.0));

        mockMvc.perform(get("/api/v1/risk/assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("COVERAGE_SHORTFALL"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));

        String alertId = mockMvc.perform(get("/api/v1/risk/alerts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .split("\"id\":\"")[1]
                .split("\"")[0];

        mockMvc.perform(post("/api/v1/risk/alerts/{id}/resolve", UUID.fromString(alertId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void trendTracksNewAssessmentsPerDay() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee(departmentId, teamId);
        UUID shiftTemplateId = createShiftTemplate();
        UUID rosterId = createRoster();
        addAssignment(rosterId, employeeId, shiftTemplateId);
        publishRoster(rosterId);

        UUID leaveId = createLeaveRequest(employeeId, tomorrow());
        approveLeave(leaveId);

        mockMvc.perform(get("/api/v1/risk/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date").isString())
                .andExpect(jsonPath("$[0].newAssessments").value(1))
                .andExpect(jsonPath("$[0].high").value(1))
                .andExpect(jsonPath("$[0].medium").value(0))
                .andExpect(jsonPath("$[0].low").value(0));
    }

    @Test
    void analyzeIsIdempotentWithinTheSameAnalysisWindow() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        createEmployee(departmentId, teamId);

        mockMvc.perform(post("/api/v1/risk/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssessments").value(0));

        mockMvc.perform(post("/api/v1/risk/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssessments").value(0));

        mockMvc.perform(get("/api/v1/risk/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private UUID createDepartment() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Operations"
                }
                """.formatted(UUID.randomUUID());
        return idFromLocation(mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location"));
    }

    private UUID createTeam(UUID departmentId) throws Exception {
        String request = """
                {
                  "departmentId": "%s",
                  "name": "Warehouse"
                }
                """.formatted(departmentId);
        return idFromLocation(mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location"));
    }

    private UUID createEmployee(UUID departmentId, UUID teamId) throws Exception {
        String request = """
                {
                  "employeeNumber": "EMP-777",
                  "firstName": "Risk",
                  "lastName": "Scout",
                  "email": "risk.scout@example.com",
                  "departmentId": "%s",
                  "teamId": "%s"
                }
                """.formatted(departmentId, teamId);
        return idFromLocation(mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location"));
    }

    private UUID createShiftTemplate() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Day",
                  "startTime": "08:00:00",
                  "endTime": "16:00:00",
                  "breaks": []
                }
                """.formatted(UUID.randomUUID());
        String body = mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(body.split("\"id\":\"")[1].split("\"")[0]);
    }

    private UUID createRoster() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Risk Week"
                }
                """.formatted(UUID.randomUUID());
        return idFromLocation(mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location"));
    }

    private void addAssignment(UUID rosterId, UUID employeeId, UUID shiftTemplateId) throws Exception {
        OffsetDateTime shiftStart = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).with(LocalTime.of(8, 0));
        String request = """
                {
                  "employeeId": "%s",
                  "shiftTemplateId": "%s",
                  "start": "%s",
                  "end": "%s"
                }
                """.formatted(employeeId, shiftTemplateId,
                shiftStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                shiftStart.plusHours(8).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }

    private void publishRoster(UUID rosterId) throws Exception {
        mockMvc.perform(post("/api/v1/rosters/{id}/publish", rosterId))
                .andExpect(status().isOk());
    }

    private UUID createLeaveRequest(UUID employeeId, String day) throws Exception {
        String request = """
                {
                  "employeeId": "%s",
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Family emergency"
                }
                """.formatted(employeeId, day, day);
        return idFromLocation(mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location"));
    }

    private void approveLeave(UUID leaveId) throws Exception {
        mockMvc.perform(post("/api/v1/leave-requests/{id}/approve", leaveId))
                .andExpect(status().isOk());
    }

    private UUID idFromLocation(String location) {
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private String tomorrow() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}