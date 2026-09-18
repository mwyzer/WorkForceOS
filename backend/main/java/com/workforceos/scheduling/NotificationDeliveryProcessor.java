package com.workforceos.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryProcessor {

    private static final int BATCH_SIZE = 100;

    private final NotificationDeliveryService deliveryService;

    public NotificationDeliveryProcessor(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${workforce.notifications.delivery-poll-ms:5000}")
    public void poll() {
        deliveryService.dispatchDue(BATCH_SIZE);
    }
}