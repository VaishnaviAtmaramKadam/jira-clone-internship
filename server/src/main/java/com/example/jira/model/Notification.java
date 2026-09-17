package com.example.jira.model;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
public class Notification {

    @Id
    private ObjectId id;

    // User who should receive the notification
    private String userId;

    // Project related to the notification
    private String projectId;

    // Issue related to the notification
    private String issueId;

    // Type of notification
    // Examples:
    // TASK_ASSIGNED
    // STATUS_CHANGED
    // COMMENT_ADDED
    // DUE_DATE_REMINDER
    // SPRINT_STARTED
    // SPRINT_ENDED
    private String type;

    // Notification message shown to the user
    private String message;

    // false = unread
    // true  = read
    private boolean read = false;

    // Used to prevent duplicate notifications
    private String eventKey;

    private Instant createdAt = Instant.now();

    // =========================
    // ID
    // =========================
    public String getId() {
        return id != null ? id.toHexString() : null;
    }

    public ObjectId getObjectId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    // =========================
    // User ID
    // =========================
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // =========================
    // Project ID
    // =========================
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    // =========================
    // Issue ID
    // =========================
    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    // =========================
    // Type
    // =========================
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // =========================
    // Message
    // =========================
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // =========================
    // Read
    // =========================
    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    // =========================
    // Event Key
    // =========================
    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    // =========================
    // Created At
    // =========================
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
