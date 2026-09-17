package com.example.jira.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.example.jira.model.Notification;
import com.example.jira.repository.NotificationRepository;
import com.example.jira.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================
    public Notification createNotification(
            String userId,
            String projectId,
            String issueId,
            String type,
            String message,
            String eventKey) {

        // -----------------------------------------------------
        // 1. Validate User ID
        // -----------------------------------------------------
        if (userId == null || userId.isBlank()) {

            System.out.println(
                    "Notification skipped: User ID missing"
            );

            return null;
        }

        // -----------------------------------------------------
        // 2. Validate Message
        // -----------------------------------------------------
        if (message == null || message.isBlank()) {

            System.out.println(
                    "Notification skipped: Message missing"
            );

            return null;
        }

        // -----------------------------------------------------
        // 3. Prevent Duplicate Notification
        // -----------------------------------------------------
        if (eventKey != null
                && !eventKey.isBlank()
                && notificationRepository
                        .findByEventKey(eventKey)
                        .isPresent()) {

            System.out.println(
                    "Duplicate notification ignored: "
                    + eventKey
            );

            return null;
        }

        // -----------------------------------------------------
        // 4. Create IN-APP Notification
        // -----------------------------------------------------
        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setProjectId(projectId);
        notification.setIssueId(issueId);
        notification.setType(type);
        notification.setMessage(message);

        // false = unread
        notification.setRead(false);

        notification.setEventKey(eventKey);

        Notification savedNotification
                = notificationRepository.save(notification);

        System.out.println(
                "IN-APP NOTIFICATION CREATED: "
                + message
        );

        // -----------------------------------------------------
        // 5. Send Email ONLY if Email Notifications are ON
        // -----------------------------------------------------
        try {

            ObjectId userObjectId
                    = new ObjectId(userId);

            userRepository
                    .findById(userObjectId)
                    .ifPresent(user -> {

                        // =================================================
                        // EMAIL NOTIFICATIONS OFF
                        // =================================================
                        if (!user.isEmailNotificationsEnabled()) {

                            System.out.println(
                                    "EMAIL NOT SENT: "
                                    + "Email Notifications are OFF for "
                                    + user.getEmail()
                            );

                            return;
                        }

                        // =================================================
                        // EMAIL ADDRESS CHECK
                        // =================================================
                        String email = user.getEmail();

                        if (email == null || email.isBlank()) {

                            System.out.println(
                                    "EMAIL NOT SENT: "
                                    + "User email is not available"
                            );

                            return;
                        }

                        // =================================================
                        // SEND EMAIL
                        // =================================================
                        try {

                            emailService.sendEmail(
                                    email,
                                    "Jira Clone Notification",
                                    message
                            );

                            System.out.println(
                                    "EMAIL SENT SUCCESSFULLY TO: "
                                    + email
                            );

                        } catch (Exception e) {

                            System.err.println(
                                    "FAILED TO SEND EMAIL TO: "
                                    + email
                            );

                            e.printStackTrace();
                        }
                    });

        } catch (IllegalArgumentException e) {

            System.err.println(
                    "Invalid userId for email notification: "
                    + userId
            );
        }

        return savedNotification;
    }

    // =========================================================
    // GET ALL USER NOTIFICATIONS
    // =========================================================
    public List<Notification> getUserNotifications(
            String userId) {

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }

    // =========================================================
    // GET UNREAD NOTIFICATIONS
    // =========================================================
    public List<Notification> getUnreadNotifications(
            String userId) {

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                        userId
                );
    }

    // =========================================================
    // GET UNREAD COUNT
    // =========================================================
    public long getUnreadCount(
            String userId) {

        return notificationRepository
                .countByUserIdAndReadFalse(
                        userId
                );
    }

    // =========================================================
    // MARK ONE NOTIFICATION AS READ
    // =========================================================
    public Notification markAsRead(
            String notificationId) {

        Notification notification
                = notificationRepository
                        .findById(
                                new ObjectId(notificationId)
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Notification not found"
                                )
                        );

        notification.setRead(true);

        return notificationRepository.save(
                notification
        );
    }

    // =========================================================
    // MARK ALL NOTIFICATIONS AS READ
    // =========================================================
    public void markAllAsRead(
            String userId) {

        List<Notification> notifications
                = notificationRepository
                        .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                                userId
                        );

        for (Notification notification
                : notifications) {

            notification.setRead(true);

            notificationRepository.save(
                    notification
            );
        }
    }

    // =========================================================
    // GET PROJECT NOTIFICATIONS
    // =========================================================
    public List<Notification> getProjectNotifications(
            String userId,
            String projectId) {

        return notificationRepository
                .findByUserIdAndProjectIdOrderByCreatedAtDesc(
                        userId,
                        projectId
                );
    }
}
