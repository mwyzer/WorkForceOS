package com.workforceos.observability;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import com.workforceos.event.EventPublisher;
import com.workforceos.event.OutboxStatus;
import com.workforceos.event.OutboxStore;

@Component
public class ObservabilityMetrics {

    public ObservabilityMetrics(MeterRegistry registry, OutboxStore outbox, EventPublisher publisher) {
        for (OutboxStatus status : OutboxStatus.values()) {
            registry.gauge("workforceos.outbox.entries", Tags.of("status", status.name()), outbox,
                    store -> store.countByStatus(status));
        }
        registry.gauge("workforceos.events.handled", publisher, EventPublisher::handledCount);
    }
}