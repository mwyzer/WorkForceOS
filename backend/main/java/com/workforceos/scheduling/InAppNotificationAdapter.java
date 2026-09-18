package com.workforceos.scheduling;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
class InAppNotificationAdapter implements NotificationDeliveryAdapter {

    @Override
    public Set<NotificationChannel> channels() {
        return Set.of(NotificationChannel.IN_APP);
    }

    @Override
    public DeliveryOutcome deliver(Notification notification) {
        return DeliveryOutcome.DELIVERED;
    }
}