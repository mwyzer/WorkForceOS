package com.workforceos.event;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxProcessor {

    private static final int BATCH_SIZE = 100;

    private final OutboxStore outbox;
    private final EventPublisher publisher;

    public OutboxProcessor(OutboxStore outbox, EventPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${workforce.events.outbox-poll-ms:5000}")
    public void poll() {
        outbox.findDue(OffsetDateTime.now(), BATCH_SIZE).forEach(publisher::process);
    }
}