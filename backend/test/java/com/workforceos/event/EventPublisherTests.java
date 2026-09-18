package com.workforceos.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class EventPublisherTests {

    @Test
    void publishesAndDeliversEventsToHandlers() {
        OutboxStore outbox = new InMemoryOutboxStore();
        AtomicInteger calls = new AtomicInteger();
        EventPublisher publisher = new EventPublisher(outbox, List.of(new EventHandler() {
            @Override
            public boolean supports(DomainEvent event) {
                return true;
            }

            @Override
            public void onEvent(DomainEvent event) {
                calls.incrementAndGet();
            }
        }));

        publisher.publish(DomainEvents.of("AnythingHappened", null, "Thing", UUID.randomUUID(), null, "{}"));

        assertEquals(OutboxStatus.DELIVERED, outbox.findAll().getFirst().status());
        assertEquals(1, calls.get());
    }

    @Test
    void failedHandlersMoveEntryToFailedAndRetryWithoutReRunningSucceededConsumers() {
        OutboxStore outbox = new InMemoryOutboxStore();
        AtomicInteger failingCalls = new AtomicInteger();
        AtomicInteger succeedingCalls = new AtomicInteger();

        EventPublisher publisher = new EventPublisher(outbox, List.of(
                new EventHandler() {
                    @Override
                    public boolean supports(DomainEvent event) {
                        return true;
                    }

                    @Override
                    public void onEvent(DomainEvent event) {
                        succeedingCalls.incrementAndGet();
                    }
                },
                new EventHandler() {
                    @Override
                    public boolean supports(DomainEvent event) {
                        return true;
                    }

                    @Override
                    public void onEvent(DomainEvent event) {
                        if (failingCalls.getAndIncrement() == 0) {
                            throw new IllegalStateException("transient failure");
                        }
                    }
                }));

        DomainEvent event = DomainEvents.of("Flaky", null, "Thing", UUID.randomUUID(), null, "{}");
        publisher.publish(event);
        assertEquals(OutboxStatus.FAILED, outbox.find(event.eventId()).orElseThrow().status());
        assertEquals(1, outbox.find(event.eventId()).orElseThrow().attemptCount());
        assertEquals("transient failure", outbox.find(event.eventId()).orElseThrow().lastError());

        publisher.process(outbox.find(event.eventId()).orElseThrow());
        assertSame(OutboxStatus.DELIVERED, outbox.find(event.eventId()).orElseThrow().status());
        assertEquals(1, succeedingCalls.get());
        assertEquals(2, failingCalls.get());
    }
}