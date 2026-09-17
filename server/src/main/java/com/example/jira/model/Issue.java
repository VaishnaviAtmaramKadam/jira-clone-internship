package com.example.jira.model;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "issues")
public class Issue {

    @Id
    private ObjectId id;

    private String key;
    private String title;
    private String description;
    private String type;
    private String status;
    private String priority;

    private String projectId;
    private String reporterId;
    private String assigneeId;

    // =========================
    // Subtask & Dependency
    // =========================
    // Parent task ID.
    // Null means this is a main/parent task.
    private String parentTaskId;

    // Sprint to which this issue belongs.
    private String sprintId;

    // IDs of tasks which must be completed
    // before this task can start.
    private List<String> dependencyIds;

    private int order;

    private List<String> comments;

    // =========================
    // Due Date
    // =========================
    // Deadline of the task.
    // Used for 24-hour due date reminders.
    private Instant dueDate;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

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
    // Key
    // =========================
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    // =========================
    // Title
    // =========================
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // =========================
    // Description
    // =========================
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    // Status
    // =========================
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // =========================
    // Priority
    // =========================
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    // =========================
    // Project
    // =========================
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    // =========================
    // Reporter
    // =========================
    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    // =========================
    // Assignee
    // =========================
    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    // =========================
    // Parent Task
    // =========================
    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    // =========================
    // Sprint
    // =========================
    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    // =========================
    // Dependencies
    // =========================
    public List<String> getDependencyIds() {
        return dependencyIds;
    }

    public void setDependencyIds(List<String> dependencyIds) {
        this.dependencyIds = dependencyIds;
    }

    // =========================
    // Order
    // =========================
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    // =========================
    // Comments
    // =========================
    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    // =========================
    // Due Date
    // =========================
    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    // =========================
    // Dates
    // =========================
    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
