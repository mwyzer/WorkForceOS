package com.workforceos.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.workforceos.event.OutboxStatus;
import com.workforceos.event.OutboxStore;

@Component
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxStore outbox;
    private final int failedThreshold;

    public OutboxHealthIndicator(OutboxStore outbox,
            @Value("${workforce.observability.outbox-failed-threshold:100}") int failedThreshold) {
        this.outbox = outbox;
        this.failedThreshold = failedThreshold;
    }

    @Override
    public Health health() {
        long pending = outbox.countByStatus(OutboxStatus.PENDING);
        long failed = outbox.countByStatus(OutboxStatus.FAILED);
        long delivered = outbox.countByStatus(OutboxStatus.DELIVERED);
        Health.Builder builder = failed >= failedThreshold ? Health.down() : Health.up();
        return builder
                .withDetail("pending", pending)
                .withDetail("failed", failed)
                .withDetail("delivered", delivered)
                .withDetail("failedThreshold", failedThreshold)
                .build();
    }
}