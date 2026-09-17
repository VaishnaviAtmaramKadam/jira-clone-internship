package com.example.jira.model;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attachments")
public class Attachment {

    @Id
    private ObjectId id;

    private String issueId;
    private String projectId;
    private String uploadedBy;

    private String originalFileName;
    private String storedFileName;
    private String fileType;
    private long fileSize;

    private String filePath;

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
    // Issue ID
    // =========================
    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
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
    // Uploaded By
    // =========================
    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    // =========================
    // Original File Name
    // =========================
    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    // =========================
    // Stored File Name
    // =========================
    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    // =========================
    // File Type
    // =========================
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    // =========================
    // File Size
    // =========================
    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    // =========================
    // File Path
    // =========================
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
