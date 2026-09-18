package com.workforceos.event;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventPublisher {

    private final OutboxStore outbox;
    private final List<EventHandler> handlers;
    private final Set<String> handledByConsumer = ConcurrentHashMap.newKeySet();

    public EventPublisher(OutboxStore outbox, List<EventHandler> handlers) {
        this.outbox = outbox;
        this.handlers = handlers;
    }

    @Transactional
    public void publish(DomainEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        OutboxEntry entry = new OutboxEntry(event.eventId(), event, OutboxStatus.PENDING, 0, now, now, null, null);
        outbox.save(entry);
        process(entry);
    }

    @Transactional
    public void process(OutboxEntry entry) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean allSucceeded = true;
        OutboxEntry current = entry.status() == OutboxStatus.FAILED ? entry.retried() : entry;

        for (EventHandler handler : handlers) {
            if (!handler.supports(entry.event())) {
                continue;
            }
            String dedupeKey = entry.eventId() + ":" + handler.getClass().getName();
            if (!handledByConsumer.add(dedupeKey)) {
                continue;
            }
            try {
                handler.onEvent(entry.event());
            } catch (RuntimeException ex) {
                allSucceeded = false;
                handledByConsumer.remove(dedupeKey);
                OffsetDateTime nextAttempt = now.plus(backoff(current.attemptCount() + 1));
                current = current.failed(ex.getMessage(), nextAttempt);
            }
        }

        if (allSucceeded) {
            current = current.delivered();
        }
        outbox.save(current);
    }

    private static Duration backoff(int attempt) {
        return Duration.ofSeconds(Math.min(60, Math.max(1, 1L << Math.min(attempt - 1, 5))));
    }

    public List<OutboxEntry> outboxSnapshot() {
        return outbox.findAll();
    }

    public long handledCount() {
        return handledByConsumer.size();
    }
}