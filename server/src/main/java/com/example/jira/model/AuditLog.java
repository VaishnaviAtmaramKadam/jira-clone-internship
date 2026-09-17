package com.example.jira.model;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private ObjectId id;

    private String workLogId;
    private String issueId;
    private String userId;

    // CREATE, UPDATE, DELETE
    private String action;

    // Details about what was changed
    private String details;

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
    // WorkLog ID
    // =========================

    public String getWorkLogId() {
        return workLogId;
    }

    public void setWorkLogId(String workLogId) {
        this.workLogId = workLogId;
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
    // User ID
    // =========================

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // =========================
    // Action
    // =========================

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // =========================
    // Details
    // =========================

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    // =========================
    // Created At
    // =========================

    public Instant getCreatedAt() {
        return createdAt;
    }
}