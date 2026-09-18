package com.workforceos.event;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;

public class KafkaRelayer {

    private static final int BATCH_SIZE = 100;

    private final OutboxStore outbox;
    private final KafkaEventWriter writer;

    public KafkaRelayer(OutboxStore outbox, KafkaEventWriter writer) {
        this.outbox = outbox;
        this.writer = writer;
    }

    public long relayDue(OffsetDateTime now) {
        long relayed = 0;
        for (OutboxEntry entry : outbox.findUnpublished(now, BATCH_SIZE)) {
            if (relay(entry, now)) {
                relayed++;
            }
        }
        return relayed;
    }

    private boolean relay(OutboxEntry entry, OffsetDateTime now) {
        try {
            writer.write(entry.event());
            outbox.save(entry.kafkaPublished(now));
            return true;
        } catch (RuntimeException ex) {
            OffsetDateTime nextAttempt = now.plus(backoff(entry.attemptCount() + 1));
            outbox.save(entry.relayFailed(ex.getMessage(), nextAttempt));
            return false;
        }
    }

    @Scheduled(fixedDelayString = "${workforce.events.kafka.poll-ms:2000}")
    public void poll() {
        relayDue(OffsetDateTime.now());
    }

    private static Duration backoff(int attempt) {
        return Duration.ofSeconds(Math.min(60, Math.max(1, 1L << Math.min(attempt - 1, 5))));
    }
}