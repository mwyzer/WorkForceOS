package com.workforceos.scheduling;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingNotificationAdapter implements NotificationDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public Set<NotificationChannel> channels() {
        return Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH,
                NotificationChannel.WEBHOOK);
    }

    @Override
    public DeliveryOutcome deliver(Notification notification) {
        log.info("Dispatching notification id={} recipient={} channel={} type={} title={}",
                notification.id(), notification.recipientId(), notification.channel(), notification.type(),
                notification.title());
        return DeliveryOutcome.SENT;
    }
}