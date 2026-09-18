package com.workforceos.event;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(name = "workforce.events.kafka.enabled", havingValue = "true")
public class KafkaOutboxConfiguration {

    @Bean
    public ProducerFactory<String, String> domainEventProducerFactory(
            @Value("${workforce.events.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> domainEventKafkaTemplate(
            ProducerFactory<String, String> domainEventProducerFactory) {
        return new KafkaTemplate<>(domainEventProducerFactory);
    }

    @Bean
    public KafkaEventWriter domainEventKafkaWriter(
            KafkaTemplate<String, String> domainEventKafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${workforce.events.kafka.topic:workforce.domain.events}") String topic,
            @Value("${workforce.events.kafka.send-timeout-ms:5000}") long timeoutMs) {
        return new SpringKafkaEventWriter(domainEventKafkaTemplate, objectMapper, topic, Duration.ofMillis(timeoutMs));
    }

    @Bean
    public KafkaRelayer kafkaRelayer(OutboxStore outbox, KafkaEventWriter domainEventKafkaWriter) {
        return new KafkaRelayer(outbox, domainEventKafkaWriter);
    }
}