package com.example.jira.model;

import java.time.Instant;
import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "work_logs")
public class WorkLog {

    @Id
    private ObjectId id;

    private String issueId;
    private String userId;
    private String projectId;
    private String sprintId;

    private LocalDate workDate;

    private int durationMinutes;

    private String description;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    // =========================================================
    // ID
    // =========================================================

    public String getId() {
        return id != null
                ? id.toHexString()
                : null;
    }

    public ObjectId getObjectId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    // =========================================================
    // ISSUE ID
    // =========================================================

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    // =========================================================
    // USER ID
    // =========================================================

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // =========================================================
    // PROJECT ID
    // =========================================================

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    // =========================================================
    // SPRINT ID
    // =========================================================

    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    // =========================================================
    // WORK DATE
    // =========================================================

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    // =========================================================
    // DURATION
    // =========================================================

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(
            int durationMinutes) {

        this.durationMinutes =
                durationMinutes;
    }

    // =========================================================
    // DESCRIPTION
    // =========================================================

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description) {

        this.description =
                description;
    }

    // =========================================================
    // CREATED AT
    // =========================================================

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            Instant createdAt) {

        this.createdAt =
                createdAt;
    }

    // =========================================================
    // UPDATED AT
    // =========================================================

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            Instant updatedAt) {

        this.updatedAt =
                updatedAt;
    }
}