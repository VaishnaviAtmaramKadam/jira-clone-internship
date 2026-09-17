package com.example.jira.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jira.model.Notification;
import com.example.jira.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {

        this.notificationService
                = notificationService;
    }

    // =========================
    // Get All Notifications
    // =========================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>>
            getUserNotifications(
                    @PathVariable String userId) {

        return ResponseEntity.ok(
                notificationService
                        .getUserNotifications(userId)
        );
    }

    // =========================
    // Get Unread Notifications
    // =========================
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>>
            getUnreadNotifications(
                    @PathVariable String userId) {

        return ResponseEntity.ok(
                notificationService
                        .getUnreadNotifications(userId)
        );
    }

    // =========================
    // Get Unread Count
    // =========================
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long>
            getUnreadCount(
                    @PathVariable String userId) {

        return ResponseEntity.ok(
                notificationService
                        .getUnreadCount(userId)
        );
    }

    // =========================
    // Mark Notification As Read
    // =========================
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification>
            markAsRead(
                    @PathVariable String notificationId) {

        return ResponseEntity.ok(
                notificationService
                        .markAsRead(notificationId)
        );
    }

    // =========================
    // Mark All As Read
    // =========================
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<String>
            markAllAsRead(
                    @PathVariable String userId) {

        notificationService
                .markAllAsRead(userId);

        return ResponseEntity.ok(
                "All notifications marked as read"
        );
    }

    // =========================
    // Project Notifications
    // =========================
    @GetMapping(
            "/user/{userId}/project/{projectId}"
    )
    public ResponseEntity<List<Notification>>
            getProjectNotifications(
                    @PathVariable String userId,
                    @PathVariable String projectId) {

        return ResponseEntity.ok(
                notificationService
                        .getProjectNotifications(
                                userId,
                                projectId
                        )
        );
    }
}
