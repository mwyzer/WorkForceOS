package com.workforceos.event;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventPublisher publisher;
    private final DemoEventConsumer consumer;

    public EventController(EventPublisher publisher, DemoEventConsumer consumer) {
        this.publisher = publisher;
        this.consumer = consumer;
    }

    @GetMapping("/outbox")
    public List<OutboxEntry> outbox() {
        return publisher.outboxSnapshot();
    }

    @GetMapping("/received")
    public List<DomainEvent> received() {
        return consumer.received();
    }
}