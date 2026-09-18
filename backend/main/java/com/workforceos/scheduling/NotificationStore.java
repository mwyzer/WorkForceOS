package com.workforceos.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationStore {

    List<Notification> findAll();

    List<Notification> findByRecipientId(UUID recipientId);

    Optional<Notification> findById(UUID id);

    Notification save(Notification notification);

    List<Notification> findDue(Instant now, int limit);
}