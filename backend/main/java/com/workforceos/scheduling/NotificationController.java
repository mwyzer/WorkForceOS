package com.workforceos.scheduling;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public List<Notification> findAll() {
        return notificationService.findAll();
    }

    @GetMapping("/notifications/{id}")
    public Notification findById(@PathVariable UUID id) {
        return notificationService.findById(id);
    }

    @GetMapping("/notifications/recipient/{recipientId}")
    public List<Notification> findByRecipientId(@PathVariable UUID recipientId) {
        return notificationService.findByRecipientId(recipientId);
    }

    @PostMapping("/notifications")
    public ResponseEntity<Notification> create(@RequestBody NotificationRequest request) {
        Notification notification = notificationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/notifications/" + notification.id()))
                .body(notification);
    }

    @PostMapping("/notifications/{id}/mark-as-read")
    public Notification markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }
}
