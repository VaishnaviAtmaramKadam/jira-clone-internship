package com.example.jira.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.jira.model.Notification;

public interface NotificationRepository
        extends MongoRepository<Notification, ObjectId> {

    // Get all notifications of a user
    List<Notification> findByUserIdOrderByCreatedAtDesc(
            String userId
    );

    // Get unread notifications of a user
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            String userId
    );

    // Count unread notifications
    long countByUserIdAndReadFalse(
            String userId
    );

    // Find notification using event key
    Optional<Notification> findByEventKey(
            String eventKey
    );

    // Get project notifications for a user
    List<Notification> findByUserIdAndProjectIdOrderByCreatedAtDesc(
            String userId,
            String projectId
    );
}
