package com.workforceos.event;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class DemoEventConsumer implements EventHandler {

    private final ConcurrentMap<UUID, DomainEvent> received = new ConcurrentHashMap<>();

    @Override
    public boolean supports(DomainEvent event) {
        return true;
    }

    @Override
    public void onEvent(DomainEvent event) {
        received.put(event.eventId(), event);
    }

    public List<DomainEvent> received() {
        return received.values().stream()
                .sorted(Comparator.comparing(DomainEvent::occurredAt))
                .toList();
    }
}