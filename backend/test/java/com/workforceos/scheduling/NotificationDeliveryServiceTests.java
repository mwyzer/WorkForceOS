package com.workforceos.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class NotificationDeliveryServiceTests {

    private static Notification pending(NotificationChannel channel) {
        return new Notification(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.LEAVE_APPROVED,
                "Leave approved", "Your leave was approved", channel, NotificationStatus.PENDING, Instant.now(), 0,
                Instant.now().minusSeconds(10), null);
    }

    @Test
    void inAppChannelIsDeliveredImmediately() {
        InMemoryNotificationStore store = new InMemoryNotificationStore();
        NotificationDeliveryService delivery = new NotificationDeliveryService(store,
                List.of(new InAppNotificationAdapter()));

        Notification result = delivery.deliver(pending(NotificationChannel.IN_APP));

        assertEquals(NotificationStatus.DELIVERED, result.status());
    }

    @Test
    void externalChannelIsMarkedSentByLoggingAdapter() {
        InMemoryNotificationStore store = new InMemoryNotificationStore();
        NotificationDeliveryService delivery = new NotificationDeliveryService(store,
                List.of(new LoggingNotificationAdapter()));

        Notification result = delivery.deliver(pending(NotificationChannel.EMAIL));

        assertEquals(NotificationStatus.SENT, result.status());
    }

    @Test
    void unsupportedChannelMovesToFailedWithRetryWindow() {
        InMemoryNotificationStore store = new InMemoryNotificationStore();
        NotificationDeliveryService delivery = new NotificationDeliveryService(store,
                List.of(new InAppNotificationAdapter()));
        Instant before = Instant.now();

        Notification result = delivery.deliver(pending(NotificationChannel.SMS));

        assertEquals(NotificationStatus.FAILED, result.status());
        assertEquals(1, result.attemptCount());
        assertTrue(result.lastError().contains("No delivery adapter"));
        assertTrue(result.nextAttemptAt().isAfter(before));
    }

    @Test
    void adapterFailuresMoveToFailedAndRetrySucceeds() {
        InMemoryNotificationStore store = new InMemoryNotificationStore();
        AtomicInteger calls = new AtomicInteger();
        NotificationDeliveryService delivery = new NotificationDeliveryService(store,
                List.of(new NotificationDeliveryAdapter() {
                    @Override
                    public Set<NotificationChannel> channels() {
                        return Set.of(NotificationChannel.EMAIL);
                    }

                    @Override
                    public DeliveryOutcome deliver(Notification notification) {
                        if (calls.incrementAndGet() == 1) {
                            throw new IllegalStateException("smtp down");
                        }
                        return DeliveryOutcome.SENT;
                    }
                }));

        Notification failed = delivery.deliver(pending(NotificationChannel.EMAIL));
        assertEquals(NotificationStatus.FAILED, failed.status());
        assertEquals(1, failed.attemptCount());
        assertEquals("smtp down", failed.lastError());

        Notification retried = delivery.deliver(failed);
        assertEquals(NotificationStatus.SENT, retried.status());
        assertEquals(2, calls.get());
    }

    @Test
    void dispatchDueOnlyProcessesDueEntries() {
        InMemoryNotificationStore store = new InMemoryNotificationStore();
        NotificationDeliveryService delivery = new NotificationDeliveryService(store,
                List.of(new InAppNotificationAdapter(), new LoggingNotificationAdapter()));

        Notification due = pending(NotificationChannel.IN_APP);
        Notification future = new Notification(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.ROSTER_PUBLISHED, "Roster", "Roster available", NotificationChannel.EMAIL,
                NotificationStatus.PENDING, Instant.now(), 0, Instant.now().plusSeconds(120), null);
        store.save(due);
        store.save(future);

        int dispatched = delivery.dispatchDue(10);

        assertEquals(1, dispatched);
        assertEquals(NotificationStatus.DELIVERED,
                store.findById(due.id()).orElseThrow().status());
        assertEquals(NotificationStatus.PENDING,
                store.findById(future.id()).orElseThrow().status());
    }
}