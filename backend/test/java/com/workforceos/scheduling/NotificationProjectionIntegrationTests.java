package com.workforceos.scheduling;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.workforceos.bootstrap.WorkforceOsApplication;
import com.workforceos.organization.OrganizationService;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class NotificationProjectionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void domainEventsProjectIntoDeliveredInAppNotifications() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee(departmentId, teamId);

        UUID shiftTemplateId = createShiftTemplate();
        UUID rosterId = createRoster();
        addAssignment(rosterId, employeeId, shiftTemplateId);
        mockMvc.perform(post("/api/v1/rosters/{id}/publish", rosterId))
                .andExpect(status().isOk());

        UUID overtimeId = createOvertime(employeeId);
        mockMvc.perform(post("/api/v1/overtime-requests/{id}/approve", overtimeId))
                .andExpect(status().isOk());
        UUID overtimeToReject = createOvertime(employeeId);
        mockMvc.perform(post("/api/v1/overtime-requests/{id}/reject", overtimeToReject))
                .andExpect(status().isOk());

        UUID leaveId = createLeave(employeeId);
        mockMvc.perform(post("/api/v1/leave-requests/{id}/approve", leaveId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", hasItem("ROSTER_PUBLISHED")))
                .andExpect(jsonPath("$[*].type", hasItem("OVERTIME_APPROVED")))
                .andExpect(jsonPath("$[*].type", hasItem("OVERTIME_REJECTED")))
                .andExpect(jsonPath("$[*].type", hasItem("LEAVE_APPROVED")))
                .andExpect(jsonPath("$[*].status", everyItem(isOneOf("DELIVERED", "READ"))))
                .andExpect(jsonPath("$[*].recipientId", everyItem(is(employeeId.toString()))));
    }

    private UUID createDepartment() throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "organizationId": "%s",
                          "name": "Operations"
                        }
                        """.formatted(OrganizationService.DEFAULT_ORGANIZATION_ID)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createTeam(UUID departmentId) throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "departmentId": "%s",
                          "name": "Warehouse"
                        }
                        """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createEmployee(UUID departmentId, UUID teamId) throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "employeeNumber": "NTF-001",
                          "firstName": "Notified",
                          "lastName": "Worker",
                          "email": "ntf@example.com",
                          "departmentId": "%s",
                          "teamId": "%s"
                        }
                        """.formatted(departmentId, teamId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createShiftTemplate() throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/shifts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "organizationId": "%s",
                          "name": "Day Shift",
                          "startTime": "08:00:00",
                          "endTime": "16:00:00",
                          "breaks": [{"start": "12:00:00", "end": "12:30:00"}]
                        }
                        """.formatted(OrganizationService.DEFAULT_ORGANIZATION_ID)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createRoster() throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/rosters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "organizationId": "%s",
                          "name": "Week 38"
                        }
                        """.formatted(OrganizationService.DEFAULT_ORGANIZATION_ID)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void addAssignment(UUID rosterId, UUID employeeId, UUID shiftTemplateId) throws Exception {
        java.time.OffsetDateTime start = java.time.OffsetDateTime.now().minusDays(1).withHour(0).withMinute(0)
                .withSecond(0).withNano(0);
        java.time.OffsetDateTime end = java.time.OffsetDateTime.now().plusDays(1).withHour(23).withMinute(59)
                .withSecond(0).withNano(0);
        mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "shiftTemplateId": "%s",
                                  "start": "%s",
                                  "end": "%s"
                                }
                                """.formatted(employeeId, shiftTemplateId, start.toString(), end.toString())))
                .andExpect(status().isCreated());
    }

    private UUID createOvertime(UUID employeeId) throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/overtime-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "employeeId": "%s",
                          "date": "%s",
                          "hours": 4,
                          "reason": "Peak dispatch"
                        }
                        """.formatted(employeeId, LocalDate.now().plusDays(3))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createLeave(UUID employeeId) throws Exception {
        return locationId(mockMvc.perform(post("/api/v1/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "employeeId": "%s",
                          "startDate": "%s",
                          "endDate": "%s",
                          "reason": "Annual leave"
                        }
                        """.formatted(employeeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID locationId(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}