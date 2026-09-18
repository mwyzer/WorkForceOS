package com.workforceos.event;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(KafkaRelayIntegrationTests.StubWriterConfiguration.class)
@TestPropertySource(properties = {
        "spring.security.enabled=false",
        "workforce.events.kafka.enabled=true",
        "workforce.events.kafka.poll-ms=60000000",
        "workforce.events.kafka.bootstrap-servers=localhost:9092" })
class KafkaRelayIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaRelayer kafkaRelayer;

    @Autowired
    @Qualifier("kafkaEventWriterStub")
    private KafkaEventWriterStub stubWriter;

    @Test
    void relaysRosterEventFromTransactionalOutboxToKafka() throws Exception {
        UUID organizationId = UUID.randomUUID();
        MvcResult roster = mockMvc.perform(post("/api/v1/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "Kafka Week"
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isCreated())
                .andReturn();
        String rosterJson = roster.getResponse().getContentAsString();
        UUID rosterId = UUID.fromString(rosterJson.split("\"id\":\"")[1].split("\"")[0]);

        mockMvc.perform(post("/api/v1/rosters/{id}/publish", rosterId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].event.eventType").value("RosterPublished"))
                .andExpect(jsonPath("$[0].kafkaPublishedAt", nullValue()));

        long relayed = kafkaRelayer.relayDue(OffsetDateTime.now());
        org.junit.jupiter.api.Assertions.assertEquals(1, relayed);

        mockMvc.perform(get("/api/v1/events/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kafkaPublishedAt", notNullValue()));

        List<DomainEvent> written = stubWriter.written();
        org.junit.jupiter.api.Assertions.assertEquals(1, written.size());
        org.junit.jupiter.api.Assertions.assertEquals("RosterPublished", written.get(0).eventType());
        org.junit.jupiter.api.Assertions.assertEquals(rosterId, written.get(0).aggregateId());
    }

    @TestConfiguration
    static class StubWriterConfiguration {

        @Bean
        @Primary
        KafkaEventWriterStub kafkaEventWriterStub() {
            return new KafkaEventWriterStub();
        }
    }

    static class KafkaEventWriterStub implements KafkaEventWriter {

        private final List<DomainEvent> written = new ArrayList<>();

        @Override
        public void write(DomainEvent event) {
            written.add(event);
        }

        List<DomainEvent> written() {
            return written;
        }
    }
}