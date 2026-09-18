package com.workforceos.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryService {

    private static final int DEFAULT_BATCH = 100;

    private final NotificationStore store;
    private final Map<NotificationChannel, NotificationDeliveryAdapter> adapters;
    private final Set<NotificationChannel> supportedChannels;

    public NotificationDeliveryService(NotificationStore store,
            List<NotificationDeliveryAdapter> configuredAdapters) {
        this.store = store;
        this.adapters = new HashMap<>();
        for (NotificationDeliveryAdapter adapter : configuredAdapters) {
            for (NotificationChannel channel : adapter.channels()) {
                this.adapters.put(channel, adapter);
            }
        }
        this.supportedChannels = Set.copyOf(this.adapters.keySet());
    }

    @Transactional
    public Notification deliver(Notification notification) {
        NotificationDeliveryAdapter adapter = adapters.get(notification.channel());
        if (adapter == null) {
            return store.save(notification.failed(
                    "No delivery adapter for channel " + notification.channel(),
                    Instant.now().plus(backoff(notification.attemptCount() + 1))));
        }
        try {
            DeliveryOutcome outcome = adapter.deliver(notification);
            Notification updated = switch (outcome) {
                case DELIVERED -> notification.delivered();
                case SENT -> notification.sent();
                case FAILED -> notification.failed("Adapter reported a delivery failure",
                        Instant.now().plus(backoff(notification.attemptCount() + 1)));
            };
            return store.save(updated);
        } catch (RuntimeException ex) {
            return store.save(notification.failed(ex.getMessage(),
                    Instant.now().plus(backoff(notification.attemptCount() + 1))));
        }
    }

    @Transactional
    public int dispatchDue(int limit) {
        if (limit <= 0) {
            limit = DEFAULT_BATCH;
        }
        int dispatched = 0;
        for (Notification due : store.findDue(Instant.now(), limit)) {
            deliver(due);
            dispatched++;
        }
        return dispatched;
    }

    public Set<NotificationChannel> supportedChannels() {
        return supportedChannels;
    }

    static Duration backoff(int attempt) {
        return Duration.ofSeconds(Math.min(60, Math.max(1, 1L << Math.min(attempt - 1, 5))));
    }
}