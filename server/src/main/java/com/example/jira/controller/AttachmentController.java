package com.example.jira.controller;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.jira.model.Attachment;
import com.example.jira.model.Issue;
import com.example.jira.model.Project;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.Projectrepository;
import com.example.jira.service.AttachmentService;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "http://localhost:3000")
public class AttachmentController {

        private final AttachmentService attachmentService;
        private final Projectrepository projectrepository;
        private final IssueRepository issueRepository;

        public AttachmentController(
                        AttachmentService attachmentService,
                        Projectrepository projectrepository,
                        IssueRepository issueRepository) {

                this.attachmentService = attachmentService;
                this.projectrepository = projectrepository;
                this.issueRepository = issueRepository;
        }

        /*
         * =========================================================
         * CHECK PROJECT MEMBER
         * =========================================================
         */
        private boolean isProjectMember(
                        String projectId,
                        String userId) {

                if (!ObjectId.isValid(projectId)
                                || userId == null
                                || userId.isBlank()) {

                        return false;
                }

                Project project = projectrepository
                                .findById(new ObjectId(projectId))
                                .orElse(null);

                if (project == null) {
                        return false;
                }

                boolean isOwner = project.getOwnerId() != null
                                && project.getOwnerId()
                                                .equals(userId);

                boolean isMember = project.getMemberIds() != null
                                && project.getMemberIds()
                                                .contains(userId);

                return isOwner || isMember;
        }

        /*
         * =========================================================
         * UPLOAD FILE
         * =========================================================
         */
        @PostMapping("/upload")
        public ResponseEntity<?> uploadFile(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("issueId") String issueId,
                        @RequestParam("projectId") String projectId,
                        @RequestParam("uploadedBy") String uploadedBy) {

                try {

                        if (!ObjectId.isValid(projectId)) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("Invalid project ID");
                        }

                        if (!ObjectId.isValid(issueId)) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("Invalid issue ID");
                        }

                        if (uploadedBy == null
                                        || uploadedBy.isBlank()) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("User ID is required");
                        }

                        /*
                         * -----------------------------------------
                         * CHECK ISSUE
                         * -----------------------------------------
                         */
                        Issue issue = issueRepository
                                        .findById(
                                                        new ObjectId(issueId))
                                        .orElse(null);

                        if (issue == null) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("Issue not found");
                        }

                        /*
                         * -----------------------------------------
                         * CHECK ISSUE PROJECT
                         * -----------------------------------------
                         */
                        if (issue.getProjectId() == null
                                        || !issue.getProjectId()
                                                        .equals(projectId)) {

                                return ResponseEntity
                                                .badRequest()
                                                .body(
                                                                "Issue does not belong to this project");
                        }

                        /*
                         * -----------------------------------------
                         * CHECK PROJECT MEMBER
                         * -----------------------------------------
                         */
                        if (!isProjectMember(
                                        projectId,
                                        uploadedBy)) {

                                return ResponseEntity
                                                .status(403)
                                                .body(
                                                                "Only project members can upload attachments");
                        }

                        /*
                         * -----------------------------------------
                         * SAVE FILE
                         * -----------------------------------------
                         */
                        Attachment attachment = attachmentService.uploadFile(
                                        file,
                                        issueId,
                                        projectId,
                                        uploadedBy);

                        return ResponseEntity.ok(
                                        attachment);

                } catch (RuntimeException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(e.getMessage());
                }
        }

        /*
         * =========================================================
         * GET ISSUE ATTACHMENTS
         * =========================================================
         */
        @GetMapping("/issue/{issueId}")
        public ResponseEntity<?> getIssueAttachments(
                        @PathVariable String issueId,
                        @RequestParam("userId") String userId) {

                try {

                        if (!ObjectId.isValid(issueId)) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("Invalid issue ID");
                        }

                        if (userId == null
                                        || userId.isBlank()) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("User ID is required");
                        }

                        /*
                         * -----------------------------------------
                         * FIND ISSUE
                         * -----------------------------------------
                         */
                        Issue issue = issueRepository
                                        .findById(
                                                        new ObjectId(issueId))
                                        .orElse(null);

                        if (issue == null) {

                                return ResponseEntity
                                                .status(404)
                                                .body("Issue not found");
                        }

                        String projectId = issue.getProjectId();

                        if (projectId == null
                                        || projectId.isBlank()) {

                                return ResponseEntity
                                                .badRequest()
                                                .body(
                                                                "Project information not available");
                        }

                        /*
                         * -----------------------------------------
                         * CHECK MEMBER
                         * -----------------------------------------
                         */
                        if (!isProjectMember(
                                        projectId,
                                        userId)) {

                                return ResponseEntity
                                                .status(403)
                                                .body(
                                                                "Only project members can view attachments");
                        }

                        /*
                         * -----------------------------------------
                         * RETURN ATTACHMENTS
                         * -----------------------------------------
                         */
                        List<Attachment> attachments = attachmentService
                                        .getAttachmentsByIssue(
                                                        issueId);

                        return ResponseEntity.ok(
                                        attachments);

                } catch (RuntimeException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(e.getMessage());
                }
        }

        /*
         * =========================================================
         * DOWNLOAD FILE
         * =========================================================
         */
        @GetMapping("/download/{attachmentId}")
        public ResponseEntity<?> downloadAttachment(
                        @PathVariable String attachmentId,
                        @RequestParam("userId") String userId) {

                try {

                        if (userId == null
                                        || userId.isBlank()) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("User ID is required");
                        }

                        /*
                         * -----------------------------------------
                         * FIND ATTACHMENT
                         * -----------------------------------------
                         */
                        Attachment attachment = attachmentService
                                        .getAttachment(
                                                        attachmentId);

                        String projectId = attachment.getProjectId();

                        if (!ObjectId.isValid(projectId)) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("Invalid project ID");
                        }

                        /*
                         * -----------------------------------------
                         * CHECK PROJECT
                         * -----------------------------------------
                         */
                        Project project = projectrepository
                                        .findById(
                                                        new ObjectId(projectId))
                                        .orElse(null);

                        if (project == null) {

                                return ResponseEntity
                                                .badRequest()
                                                .body("Project not found");
                        }

                        /*
                         * -----------------------------------------
                         * CHECK MEMBER
                         * -----------------------------------------
                         */
                        boolean isOwner = project.getOwnerId() != null
                                        && project.getOwnerId()
                                                        .equals(userId);

                        boolean isMember = project.getMemberIds() != null
                                        && project.getMemberIds()
                                                        .contains(userId);

                        if (!isOwner && !isMember) {

                                return ResponseEntity
                                                .status(403)
                                                .body(
                                                                "Only project members can view or download attachments");
                        }

                        /*
                         * -----------------------------------------
                         * READ FILE
                         * -----------------------------------------
                         */
                        byte[] fileData = attachmentService
                                        .getAttachmentFile(
                                                        attachmentId);

                        /*
                         * -----------------------------------------
                         * FILE TYPE
                         * -----------------------------------------
                         */
                        String fileType = attachment.getFileType();

                        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

                        if ("pdf".equalsIgnoreCase(
                                        fileType)) {

                                mediaType = MediaType.APPLICATION_PDF;

                        } else if ("png".equalsIgnoreCase(
                                        fileType)) {

                                mediaType = MediaType.IMAGE_PNG;

                        } else if ("jpg".equalsIgnoreCase(
                                        fileType)
                                        || "jpeg".equalsIgnoreCase(
                                                        fileType)) {

                                mediaType = MediaType.IMAGE_JPEG;
                        }

                        /*
                         * -----------------------------------------
                         * RESPONSE
                         * -----------------------------------------
                         */
                        return ResponseEntity
                                        .ok()
                                        .contentType(mediaType)
                                        .header(
                                                        HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\""
                                                                        + attachment
                                                                                        .getOriginalFileName()
                                                                        + "\"")
                                        .body(fileData);

                } catch (RuntimeException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(e.getMessage());
                }
        }
}
