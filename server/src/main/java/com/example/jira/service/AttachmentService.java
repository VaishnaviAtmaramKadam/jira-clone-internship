package com.example.jira.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.jira.model.Attachment;
import com.example.jira.repository.AttachmentRepository;

@Service
public class AttachmentService {

    // =========================
    // Constants
    // =========================
    private static final long MAX_FILE_SIZE
            = 10 * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS
            = Arrays.asList(
                    "pdf",
                    "png",
                    "jpg",
                    "jpeg",
                    "docx"
            );

    // =========================
    // Repository
    // =========================
    private final AttachmentRepository attachmentRepository;

    // =========================
    // Upload Directory
    // =========================
    private final Path uploadDirectory
            = Paths.get("uploads/attachments");

    // =========================
    // Constructor
    // =========================
    public AttachmentService(
            AttachmentRepository attachmentRepository) {

        this.attachmentRepository
                = attachmentRepository;

        try {

            Files.createDirectories(
                    uploadDirectory
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to create upload directory",
                    e
            );
        }
    }

    // =========================
    // Upload File
    // =========================
    public Attachment uploadFile(
            MultipartFile file,
            String issueId,
            String projectId,
            String uploadedBy) {

        // -------------------------
        // File validation
        // -------------------------
        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "File is required"
            );
        }

        // -------------------------
        // File size validation
        // -------------------------
        if (file.getSize() > MAX_FILE_SIZE) {

            throw new RuntimeException(
                    "File size must not exceed 10 MB"
            );
        }

        // -------------------------
        // Original file name
        // -------------------------
        String originalFileName
                = file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new RuntimeException(
                    "Invalid file name"
            );
        }

        // -------------------------
        // Get extension
        // -------------------------
        String extension = "";

        int lastDot
                = originalFileName.lastIndexOf(".");

        if (lastDot > 0
                && lastDot < originalFileName.length() - 1) {

            extension
                    = originalFileName
                            .substring(lastDot + 1)
                            .toLowerCase();
        }

        // -------------------------
        // Extension validation
        // -------------------------
        if (!ALLOWED_EXTENSIONS.contains(extension)) {

            throw new RuntimeException(
                    "Only PDF, PNG, JPG, JPEG and DOCX files are allowed"
            );
        }

        // =========================
        // Generate unique filename
        // =========================
        String storedFileName
                = UUID.randomUUID()
                        .toString()
                + "."
                + extension;

        Path targetPath
                = uploadDirectory.resolve(
                        storedFileName
                );

        // =========================
        // Store file
        // =========================
        try {

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to store file",
                    e
            );
        }

        // =========================
        // Create Attachment object
        // =========================
        Attachment attachment
                = new Attachment();

        attachment.setIssueId(
                issueId
        );

        attachment.setProjectId(
                projectId
        );

        attachment.setUploadedBy(
                uploadedBy
        );

        attachment.setOriginalFileName(
                originalFileName
        );

        attachment.setStoredFileName(
                storedFileName
        );

        attachment.setFileType(
                extension
        );

        attachment.setFileSize(
                file.getSize()
        );

        attachment.setFilePath(
                targetPath.toString()
        );

        // =========================
        // Save metadata in MongoDB
        // =========================
        return attachmentRepository.save(
                attachment
        );
    }

    // =========================
    // Get Attachments By Issue
    // =========================
    public List<Attachment> getAttachmentsByIssue(
            String issueId) {

        return attachmentRepository
                .findByIssueId(issueId);
    }

    // =========================
    // Get Attachment
    // =========================
    public Attachment getAttachment(
            String attachmentId) {

        if (!ObjectId.isValid(attachmentId)) {

            throw new RuntimeException(
                    "Invalid attachment ID"
            );
        }

        return attachmentRepository
                .findById(
                        new ObjectId(attachmentId)
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Attachment not found"
                        )
                );
    }

    // =========================
    // Read Attachment File
    // =========================
    public byte[] getAttachmentFile(
            String attachmentId) {

        Attachment attachment
                = getAttachment(
                        attachmentId
                );

        Path filePath
                = Paths.get(
                        attachment.getFilePath()
                );

        try {

            if (!Files.exists(filePath)) {

                throw new RuntimeException(
                        "Attachment file not found on server"
                );
            }

            return Files.readAllBytes(
                    filePath
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to read attachment file",
                    e
            );
        }
    }

    // =========================
    // Delete Attachments By Issue
    // =========================
    public void deleteAttachmentsByIssue(
            String issueId) {

        List<Attachment> attachments
                = attachmentRepository
                        .findByIssueId(issueId);

        // -------------------------
        // Delete physical files
        // -------------------------
        for (Attachment attachment : attachments) {

            String filePath
                    = attachment.getFilePath();

            if (filePath != null
                    && !filePath.isBlank()) {

                try {

                    Files.deleteIfExists(
                            Paths.get(filePath)
                    );

                } catch (IOException e) {

                    throw new RuntimeException(
                            "Failed to delete attachment file: "
                            + attachment
                                    .getOriginalFileName(),
                            e
                    );
                }
            }
        }

        // -------------------------
        // Delete MongoDB records
        // -------------------------
        attachmentRepository
                .deleteByIssueId(issueId);
    }
}
