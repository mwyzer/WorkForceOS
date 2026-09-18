package com.workforceos.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class KafkaRelayerTests {

    @Test
    void relaysUnpublishedEntriesAndMarksKafkaPublished() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        List<DomainEvent> written = new ArrayList<>();
        KafkaRelayer relayer = new KafkaRelayer(outbox, written::add);

        DomainEvent event = DomainEvents.of("RosterPublished", UUID.randomUUID(), "Roster", UUID.randomUUID(), null,
                "{}");
        new EventPublisher(outbox, List.of()).publish(event);

        OffsetDateTime now = OffsetDateTime.now();
        long relayed = relayer.relayDue(now);

        assertEquals(1, relayed);
        assertEquals(List.of(event), written);
        assertTrue(outbox.find(event.eventId()).orElseThrow().kafkaPublished());
        assertFalse(outbox.find(event.eventId()).orElseThrow().kafkaPublishedAt().isBefore(now));
    }

    @Test
    void skipsEntriesAlreadyPublished() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        List<DomainEvent> written = new ArrayList<>();
        KafkaRelayer relayer = new KafkaRelayer(outbox, written::add);

        DomainEvent first = DomainEvents.of("A", null, "Thing", UUID.randomUUID(), null, "{}");
        DomainEvent second = DomainEvents.of("B", null, "Thing", UUID.randomUUID(), null, "{}");
        EventPublisher publisher = new EventPublisher(outbox, List.of());
        publisher.publish(first);
        publisher.publish(second);

        OffsetDateTime now = OffsetDateTime.now();
        assertEquals(2, relayer.relayDue(now));
        assertEquals(0, relayer.relayDue(now.plusSeconds(1)));
        assertEquals(2, written.size());
    }

    @Test
    void failedWritesAreRetriedWithBackoffAndNotMarkedPublished() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        AtomicInteger calls = new AtomicInteger();
        KafkaEventWriter flaky = event -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("broker unreachable");
            }
        };
        KafkaRelayer relayer = new KafkaRelayer(outbox, flaky);

        DomainEvent event = DomainEvents.of("Flaky", null, "Thing", UUID.randomUUID(), null, "{}");
        new EventPublisher(outbox, List.of()).publish(event);

        OffsetDateTime now = OffsetDateTime.now();
        assertEquals(0, relayer.relayDue(now));

        OutboxEntry after = outbox.find(event.eventId()).orElseThrow();
        assertFalse(after.kafkaPublished());
        assertEquals(1, after.attemptCount());
        assertTrue(after.lastError().contains("broker unreachable"));
        assertTrue(after.nextAttemptAt().isAfter(now));

        long relayed = relayer.relayDue(now.plusSeconds(10));
        assertEquals(1, relayed);
        assertTrue(outbox.find(event.eventId()).orElseThrow().kafkaPublished());
        assertEquals(2, calls.get());
    }
}