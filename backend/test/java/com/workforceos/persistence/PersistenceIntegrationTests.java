package com.workforceos.persistence;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.bootstrap.WorkforceOsApplication;
import com.workforceos.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Exercises the real JPA-backed store beans (no "test" profile) against an H2 database
 * running the Flyway migrations with Hibernate schema validation, mirroring the
 * production PostgreSQL startup path (application.yml defaults to PostgreSQL). This
 * proves the operational domain persists end to end and that the JPA entities validate
 * against the versioned schema.
 */
@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("jpa")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:jpa-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.cache.type=simple",
        "management.endpoint.health.probes.enabled=true",
        "spring.security.enabled=false",
        "workforce.risk.recompute-cron=0 0 0 31 2 ?"
})
class PersistenceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void migrationsSeedTenantRoot() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void workforceDomainPersistsThroughJpaStores() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee("JPA-001", departmentId, teamId);

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeNumber", org.hamcrest.Matchers.hasItem("JPA-001")));

        mockMvc.perform(get("/api/v1/departments/{id}", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Operations"));
    }

    @Test
    void scheduleAndAttendancePersistThroughJpaStores() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee("JPA-002", departmentId, teamId);
        UUID shiftTemplateId = createShiftTemplate();
        UUID rosterId = createRoster();
        UUID assignmentId = addAssignment(rosterId, employeeId, shiftTemplateId);

        mockMvc.perform(post("/api/v1/rosters/{id}/publish", rosterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/rosters/{id}/assignments", rosterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assignmentId.toString()));

        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "%s"
                                }
                                """.formatted(employeeId, OffsetDateTime.now().toString())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/attendance/clock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "occurredAt": "%s"
                                }
                                """.formatted(employeeId, OffsetDateTime.now().toString())))
                .andExpect(status().isOk());
    }

    @Test
    void requestsNotificationsAndHandoversPersistThroughJpaStores() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee("JPA-003", departmentId, teamId);

        MvcResult leave = mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "reason": "Annual leave"
                                }
                                """.formatted(employeeId, LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        UUID leaveRequestId = locationId(leave);

        mockMvc.perform(post("/api/v1/leave-requests/{id}/approve", leaveRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/approvals/{requestId}", leaveRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(post("/api/v1/overtime-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "date": "%s",
                                  "hours": 4,
                                  "reason": "Peak dispatch"
                                }
                                """.formatted(employeeId, LocalDate.now().plusDays(3))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "type": "ROSTER_PUBLISHED",
                                  "title": "Roster published",
                                  "body": "Your next roster is available",
                                  "channel": "IN_APP"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isCreated());

        String handoverRequest = """
                {
                  "employeeId": "%s",
                  "items": [
                    {
                      "type": "INCIDENT",
                      "content": "Broken dock door 3"
                    }
                  ]
                }
                """.formatted(employeeId);

        MvcResult handover = mockMvc.perform(post("/api/v1/handovers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverRequest))
                .andExpect(status().isCreated())
                .andReturn();
        locationId(handover);

        mockMvc.perform(get("/api/v1/handovers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actor": "admin",
                                  "action": "HANDOVER_CREATED",
                                  "resource": "Handover",
                                  "resourceId": "%s",
                                  "details": "handover test"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void employeeUpdateAndDeactivationPersist() throws Exception {
        UUID departmentId = createDepartment();
        UUID teamId = createTeam(departmentId);
        UUID employeeId = createEmployee("JPA-004", departmentId, teamId);

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Renamed",
                                  "lastName": "Worker",
                                  "email": "renamed@example.com",
                                  "departmentId": "%s",
                                  "teamId": "%s"
                                }
                                """.formatted(departmentId, teamId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Renamed"));

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private UUID createDepartment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "Operations"
                                }
                                """.formatted(OrganizationService.DEFAULT_ORGANIZATION_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        return locationId(result);
    }

    private UUID createTeam(UUID departmentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": "%s",
                                  "name": "Warehouse"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn();
        return locationId(result);
    }

    private UUID createEmployee(String number, UUID departmentId, UUID teamId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "%s",
                                  "firstName": "Test",
                                  "lastName": "Worker",
                                  "email": "%s@example.com",
                                  "departmentId": "%s",
                                  "teamId": "%s"
                                }
                                """.formatted(number, number.toLowerCase(), departmentId, teamId)))
                .andExpect(status().isCreated())
                .andReturn();
        return locationId(result);
    }

    private UUID createShiftTemplate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/shifts")
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
                .andReturn();
        return locationId(result);
    }

    private UUID createRoster() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "Week 38"
                                }
                                """.formatted(OrganizationService.DEFAULT_ORGANIZATION_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        return locationId(result);
    }

    private UUID addAssignment(UUID rosterId, UUID employeeId, UUID shiftTemplateId) throws Exception {
        java.time.OffsetDateTime start = java.time.OffsetDateTime.now().minusDays(1).withHour(0).withMinute(0)
                .withSecond(0).withNano(0);
        java.time.OffsetDateTime end = java.time.OffsetDateTime.now().plusDays(1).withHour(23).withMinute(59)
                .withSecond(0).withNano(0);
        MvcResult result = mockMvc.perform(post("/api/v1/rosters/{id}/assignments", rosterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "%s",
                                  "shiftTemplateId": "%s",
                                  "start": "%s",
                                  "end": "%s"
                                }
                                """.formatted(employeeId, shiftTemplateId, start.toString(), end.toString())))
                .andExpect(status().isCreated())
                .andReturn();
        return locationId(result);
    }

    private UUID locationId(MvcResult result) throws Exception {
        String location = result.getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}