package com.example.jira.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.jira.model.AuditLog;
import com.example.jira.model.Issue;
import com.example.jira.model.User;
import com.example.jira.model.WorkLog;
import com.example.jira.repository.AuditLogRepository;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.UserRepository;
import com.example.jira.repository.WorkLogRepository;

@RestController
@RequestMapping("/api/worklogs")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://jira-clone-internship-1.onrender.com"
})
public class WorkLogController {

    private final WorkLogRepository workLogRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public WorkLogController(
            WorkLogRepository workLogRepository,
            IssueRepository issueRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {

        this.workLogRepository = workLogRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // =========================================================
    // CREATE WORK LOG
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createWorkLog(
            @RequestBody WorkLog workLog) {

        if (workLog.getIssueId() == null
                || workLog.getIssueId().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Issue ID is required");
        }

        if (!ObjectId.isValid(workLog.getIssueId())) {

            return ResponseEntity.badRequest()
                    .body("Invalid issue ID");
        }

        Issue issue = issueRepository.findById(
                new ObjectId(workLog.getIssueId())
        ).orElse(null);

        if (issue == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Issue not found");
        }

        if (workLog.getUserId() == null
                || workLog.getUserId().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("User ID is required");
        }

        if (!ObjectId.isValid(workLog.getUserId())) {

            return ResponseEntity.badRequest()
                    .body("Invalid user ID");
        }

        User user = userRepository.findById(
                new ObjectId(workLog.getUserId())
        ).orElse(null);

        if (user == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        if (workLog.getDurationMinutes() <= 0) {

            return ResponseEntity.badRequest()
                    .body("Duration must be greater than 0");
        }

        if (workLog.getWorkDate() == null) {

            return ResponseEntity.badRequest()
                    .body("Work date is required");
        }

        if (workLog.getWorkDate().isAfter(LocalDate.now())) {

            return ResponseEntity.badRequest()
                    .body("Work date cannot be in the future");
        }

        if (workLog.getDescription() == null
                || workLog.getDescription().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Description is required");
        }

        // Automatically take project and sprint from issue
        workLog.setProjectId(issue.getProjectId());
        workLog.setSprintId(issue.getSprintId());
        workLog.setUpdatedAt(Instant.now());

        WorkLog saved = workLogRepository.save(workLog);

        createAuditLog(
                saved.getId(),
                saved.getIssueId(),
                user.getId() != null ? user.getId().toString() : null,
                "CREATE",
                "Work log created: "
                        + saved.getDurationMinutes()
                        + " minutes, date "
                        + saved.getWorkDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saved);
    }

    // =========================================================
    // GET WORK LOGS BY ISSUE
    // =========================================================

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<?> getWorkLogsByIssue(
            @PathVariable String issueId) {

        if (!ObjectId.isValid(issueId)) {

            return ResponseEntity.badRequest()
                    .body("Invalid issue ID");
        }

        return ResponseEntity.ok(
                workLogRepository.findByIssueId(issueId)
        );
    }

    // =========================================================
    // TOTAL HOURS FOR ISSUE
    // =========================================================

    @GetMapping("/issue/{issueId}/total")
    public ResponseEntity<?> getIssueTotal(
            @PathVariable String issueId) {

        if (!ObjectId.isValid(issueId)) {

            return ResponseEntity.badRequest()
                    .body("Invalid issue ID");
        }

        List<WorkLog> logs =
                workLogRepository.findByIssueId(issueId);

        return buildTotalResponse(logs);
    }

    // =========================================================
    // GET WORK LOGS BY SPRINT
    // =========================================================

    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<?> getWorkLogsBySprint(
            @PathVariable String sprintId) {

        return ResponseEntity.ok(
                workLogRepository.findBySprintId(sprintId)
        );
    }

    // =========================================================
    // TOTAL HOURS FOR SPRINT
    // =========================================================

    @GetMapping("/sprint/{sprintId}/total")
    public ResponseEntity<?> getSprintTotal(
            @PathVariable String sprintId) {

        List<WorkLog> logs =
                workLogRepository.findBySprintId(sprintId);

        return buildTotalResponse(logs);
    }

    // =========================================================
    // UPDATE WORK LOG
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkLog(
            @PathVariable String id,
            @RequestBody WorkLog updatedWorkLog) {

        if (!ObjectId.isValid(id)) {

            return ResponseEntity.badRequest()
                    .body("Invalid work log ID");
        }

        WorkLog existing = workLogRepository.findById(
                new ObjectId(id)
        ).orElse(null);

        if (existing == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Work log not found");
        }

        if (updatedWorkLog.getUserId() == null
                || updatedWorkLog.getUserId().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("User ID is required");
        }

        if (!ObjectId.isValid(updatedWorkLog.getUserId())) {

            return ResponseEntity.badRequest()
                    .body("Invalid user ID");
        }

        User user = userRepository.findById(
                new ObjectId(updatedWorkLog.getUserId())
        ).orElse(null);

        if (user == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        if (existing.getIssueId() == null
                || !ObjectId.isValid(existing.getIssueId())) {

            return ResponseEntity.badRequest()
                    .body("Invalid issue ID in work log");
        }

        Issue issue = issueRepository.findById(
                new ObjectId(existing.getIssueId())
        ).orElse(null);

        if (issue == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Issue not found");
        }

        // Only Assignee or Project Manager can modify
        if (!hasWorkLogPermission(issue, user)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only the task assignee or Project Manager "
                                    + "can modify time entries"
                    );
        }

        if (updatedWorkLog.getDurationMinutes() <= 0) {

            return ResponseEntity.badRequest()
                    .body("Duration must be greater than 0");
        }

        if (updatedWorkLog.getWorkDate() == null) {

            return ResponseEntity.badRequest()
                    .body("Work date is required");
        }

        if (updatedWorkLog.getWorkDate().isAfter(LocalDate.now())) {

            return ResponseEntity.badRequest()
                    .body("Work date cannot be in the future");
        }

        if (updatedWorkLog.getDescription() == null
                || updatedWorkLog.getDescription().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Description is required");
        }

        String oldDetails =
                "Duration: "
                        + existing.getDurationMinutes()
                        + " minutes, Date: "
                        + existing.getWorkDate()
                        + ", Description: "
                        + existing.getDescription();

        existing.setDurationMinutes(
                updatedWorkLog.getDurationMinutes()
        );

        existing.setWorkDate(
                updatedWorkLog.getWorkDate()
        );

        existing.setDescription(
                updatedWorkLog.getDescription()
        );

        existing.setUpdatedAt(Instant.now());

        WorkLog saved =
                workLogRepository.save(existing);

        String newDetails =
                "Duration: "
                        + saved.getDurationMinutes()
                        + " minutes, Date: "
                        + saved.getWorkDate()
                        + ", Description: "
                        + saved.getDescription();

        createAuditLog(
                saved.getId(),
                saved.getIssueId(),
                user.getId() != null ? user.getId().toString() : null,
                "UPDATE",
                "Old: ["
                        + oldDetails
                        + "] New: ["
                        + newDetails
                        + "]"
        );

        return ResponseEntity.ok(saved);
    }

    // =========================================================
    // DELETE WORK LOG
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkLog(
            @PathVariable String id,
            @RequestParam String userId) {

        if (!ObjectId.isValid(id)) {

            return ResponseEntity.badRequest()
                    .body("Invalid work log ID");
        }

        WorkLog existing = workLogRepository.findById(
                new ObjectId(id)
        ).orElse(null);

        if (existing == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Work log not found");
        }

        if (userId == null || userId.isBlank()) {

            return ResponseEntity.badRequest()
                    .body("User ID is required");
        }

        if (!ObjectId.isValid(userId)) {

            return ResponseEntity.badRequest()
                    .body("Invalid user ID");
        }

        User user = userRepository.findById(
                new ObjectId(userId)
        ).orElse(null);

        if (user == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        if (existing.getIssueId() == null
                || !ObjectId.isValid(existing.getIssueId())) {

            return ResponseEntity.badRequest()
                    .body("Invalid issue ID in work log");
        }

        Issue issue = issueRepository.findById(
                new ObjectId(existing.getIssueId())
        ).orElse(null);

        if (issue == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Issue not found");
        }

        // Only Assignee or Project Manager can delete
        if (!hasWorkLogPermission(issue, user)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only the task assignee or Project Manager "
                                    + "can modify time entries"
                    );
        }

        String details =
                "Deleted work log: "
                        + existing.getDurationMinutes()
                        + " minutes, Date: "
                        + existing.getWorkDate()
                        + ", Description: "
                        + existing.getDescription();

        workLogRepository.delete(existing);

        createAuditLog(
                existing.getId(),
                existing.getIssueId(),
                user.getId() != null ? user.getId().toString() : null,
                "DELETE",
                details
        );

        return ResponseEntity.ok(
                "Work log deleted successfully"
        );
    }

    // =========================================================
    // GET AUDIT LOGS FOR WORK LOG
    // =========================================================

    @GetMapping("/{id}/audit")
    public ResponseEntity<?> getAuditLogs(
            @PathVariable String id) {

        if (!ObjectId.isValid(id)) {

            return ResponseEntity.badRequest()
                    .body("Invalid work log ID");
        }

        return ResponseEntity.ok(
                auditLogRepository.findByWorkLogId(id)
        );
    }

    // =========================================================
    // PERMISSION CHECK
    // =========================================================

    private boolean hasWorkLogPermission(
            Issue issue,
            User user) {

        // Task Assignee
        if (issue.getAssigneeId() != null
                && user.getId() != null
                && issue.getAssigneeId().equals(
                        user.getId().toString())) {

            return true;
        }

        // Project Manager
        if (user.getRole() != null
                && user.getRole().equalsIgnoreCase(
                        "PROJECT_MANAGER")) {

            return true;
        }

        if (user.getRole() != null
                && user.getRole().equalsIgnoreCase(
                        "PROJECT MANAGER")) {

            return true;
        }

        return false;
    }

    // =========================================================
    // CREATE AUDIT LOG
    // =========================================================

    private void createAuditLog(
            String workLogId,
            String issueId,
            String userId,
            String action,
            String details) {

        AuditLog auditLog = new AuditLog();

        auditLog.setWorkLogId(workLogId);
        auditLog.setIssueId(issueId);
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    // =========================================================
    // TOTAL RESPONSE
    // =========================================================

    private ResponseEntity<?> buildTotalResponse(
            List<WorkLog> logs) {

        int totalMinutes =
                logs.stream()
                        .mapToInt(WorkLog::getDurationMinutes)
                        .sum();

        double totalHours =
                totalMinutes / 60.0;

        Map<String, Object> response =
                new HashMap<>();

        response.put("totalMinutes", totalMinutes);
        response.put("totalHours", totalHours);

        return ResponseEntity.ok(response);
    }
}