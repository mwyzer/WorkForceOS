package com.workforceos.event;

public interface KafkaEventWriter {

    void write(DomainEvent event);
}