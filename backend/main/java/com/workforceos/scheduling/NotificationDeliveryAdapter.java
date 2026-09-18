package com.workforceos.scheduling;

import java.util.Set;

public interface NotificationDeliveryAdapter {

    Set<NotificationChannel> channels();

    DeliveryOutcome deliver(Notification notification);
}