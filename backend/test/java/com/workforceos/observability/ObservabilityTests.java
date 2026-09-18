package com.workforceos.observability;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.security.enabled=false",
        "management.endpoint.health.show-details=always"
})
class ObservabilityTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new RequestCorrelationFilter())
                .build();
    }

    @Test
    void errorResponsesCarryTraceIdAndRequestId() throws Exception {
        UUID missingRosterId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/rosters/{id}/publish", missingRosterId)
                        .header("X-Request-Id", "client-request-1"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "client-request-1"))
                .andExpect(jsonPath("$.traceId").value(not("-")))
                .andExpect(jsonPath("$.requestId").value("client-request-1"))
                .andExpect(jsonPath("$.path").value(is("/api/v1/rosters/" + missingRosterId + "/publish")));
    }

    @Test
    void honorsIncomingW3cTraceparentHeader() throws Exception {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        UUID missingRosterId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/rosters/{id}", missingRosterId)
                        .header("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("traceparent",
                        org.hamcrest.Matchers.startsWith("00-" + traceId + "-")))
                .andExpect(jsonPath("$.traceId").value(traceId));
    }

    @Test
    void healthAndMetricsExposeOutboxAndRequestSignals() throws Exception {
        String rosterResponse = mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "Health Week"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID rosterId = UUID.fromString(rosterResponse.split("\"id\":\"")[1].split("\"")[0]);

        mockMvc.perform(post("/api/v1/rosters/{id}/publish", rosterId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.outbox.status").value("UP"))
                .andExpect(jsonPath("$.components.outbox.details.pending").value(0))
                .andExpect(jsonPath("$.components.outbox.details.delivered").value(1));

        mockMvc.perform(get("/actuator/metrics/workforceos.outbox.entries?tag=status:DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements", hasSize(1)))
                .andExpect(jsonPath("$.measurements[0].value").value(1.0));
    }
}