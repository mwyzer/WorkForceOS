package com.workforceos.event;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SpringKafkaEventWriter implements KafkaEventWriter {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final Duration timeout;

    public SpringKafkaEventWriter(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            String topic, Duration timeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.topic = topic;
        this.timeout = timeout;
    }

    @Override
    public void write(DomainEvent event) {
        try {
            String envelope = objectMapper.writeValueAsString(envelope(event));
            MessageBuilder<String> builder = MessageBuilder.withPayload(envelope)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, event.eventId().toString())
                    .setHeader("eventType", event.eventType())
                    .setHeader("eventVersion", event.eventVersion())
                    .setHeader("occurredAt", event.occurredAt().toInstant().toString());
            if (event.organizationId() != null) {
                builder.setHeader("organizationId", event.organizationId().toString());
            }
            if (event.aggregateType() != null) {
                builder.setHeader("aggregateType", event.aggregateType());
            }
            if (event.aggregateId() != null) {
                builder.setHeader("aggregateId", event.aggregateId().toString());
            }
            if (event.correlationId() != null) {
                builder.setHeader("correlationId", event.correlationId());
            }
            if (event.causationId() != null) {
                builder.setHeader("causationId", event.causationId());
            }
            Message<String> message = builder.build();

            kafkaTemplate.send(message).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event " + event.eventId(), e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to publish domain event " + event.eventId() + " to Kafka: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> envelope(DomainEvent event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.eventId().toString());
        envelope.put("eventType", event.eventType());
        envelope.put("eventVersion", event.eventVersion());
        envelope.put("occurredAt", event.occurredAt().toInstant().toString());
        envelope.put("organizationId", event.organizationId() == null ? null : event.organizationId().toString());
        envelope.put("aggregateType", event.aggregateType());
        envelope.put("aggregateId", event.aggregateId() == null ? null : event.aggregateId().toString());
        envelope.put("actorId", event.actorId());
        envelope.put("correlationId", event.correlationId());
        envelope.put("causationId", event.causationId());
        envelope.put("payload", event.payload() == null ? "{}" : event.payload());
        return envelope;
    }
}