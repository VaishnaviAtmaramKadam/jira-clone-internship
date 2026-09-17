package com.example.jira.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.springframework.web.bind.annotation.RestController;

import com.example.jira.model.Issue;
import com.example.jira.repository.IssueRepository;
import com.example.jira.service.NotificationService;
import com.example.jira.service.WebSocketEventService;
import com.example.jira.service.AttachmentService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final WebSocketEventService webSocketEventService;
    private final NotificationService notificationService;
    private final AttachmentService attachmentService;

    public IssueController(
            IssueRepository issueRepository,
            WebSocketEventService webSocketEventService,
            NotificationService notificationService,
            AttachmentService attachmentService) {

        this.issueRepository = issueRepository;
        this.webSocketEventService = webSocketEventService;
        this.notificationService = notificationService;
        this.attachmentService = attachmentService;
    }

    // =========================
    // CREATE ISSUE
    // =========================
    @PostMapping
    public ResponseEntity<?> createIssue(
            @RequestBody Issue issue) {

        if (issue.getTitle() == null
                || issue.getTitle().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body("Issue title is required");
        }

        if (issue.getProjectId() == null
                || issue.getProjectId().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body("Project is required");
        }

        // =========================
        // SUBTASK VALIDATION
        // =========================
        if (issue.getParentTaskId() != null
                && !issue.getParentTaskId().trim().isEmpty()) {

            Issue parentTask;

            try {

                parentTask = issueRepository
                        .findById(
                                new ObjectId(
                                        issue.getParentTaskId()))
                        .orElse(null);

            } catch (IllegalArgumentException e) {

                return ResponseEntity
                        .badRequest()
                        .body("Invalid parent task ID");
            }

            if (parentTask == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Parent task not found");
            }

            if (parentTask.getParentTaskId() != null
                    && !parentTask.getParentTaskId()
                            .trim()
                            .isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "A subtask cannot be used as a parent task");
            }

            if (!issue.getProjectId()
                    .equals(parentTask.getProjectId())) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Subtask must belong to the same project as parent task");
            }

            // Subtask inherits project from parent
            issue.setProjectId(
                    parentTask.getProjectId());

            // Subtask inherits sprint from parent
            issue.setSprintId(
                    parentTask.getSprintId());

        } else {

            issue.setParentTaskId(null);
        }

        // =========================
        // DEFAULT VALUES
        // =========================
        if (issue.getStatus() == null
                || issue.getStatus().trim().isEmpty()) {

            issue.setStatus("TODO");
        }

        if (issue.getComments() == null) {

            issue.setComments(
                    Collections.emptyList());
        }

        if (issue.getDependencyIds() == null) {

            issue.setDependencyIds(
                    new ArrayList<>());
        }

        // =========================
        // DEPENDENCY VALIDATION
        // =========================
        for (String dependencyId
                : issue.getDependencyIds()) {

            if (dependencyId == null
                    || dependencyId.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body("Invalid dependency ID");
            }

            try {

                Issue dependency
                        = issueRepository
                                .findById(
                                        new ObjectId(
                                                dependencyId))
                                .orElse(null);

                if (dependency == null) {

                    return ResponseEntity
                            .status(
                                    HttpStatus.NOT_FOUND)
                            .body(
                                    "Dependency task not found: "
                                    + dependencyId);
                }

                if (!issue.getProjectId()
                        .equals(
                                dependency.getProjectId())) {

                    return ResponseEntity
                            .badRequest()
                            .body(
                                    "Dependency task must belong to the same project");
                }

            } catch (IllegalArgumentException e) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid dependency ID: "
                                + dependencyId);
            }
        }

        // =========================
        // TIMESTAMP
        // =========================
        issue.setUpdatedAt(
                Instant.now());

        Issue savedIssue
                = issueRepository.save(issue);

        // =========================
        // WEBSOCKET EVENT
        // =========================
        webSocketEventService.sendProjectEvent(
                savedIssue.getProjectId(),
                "ISSUE_CREATED",
                savedIssue);

        // =========================
        // TASK ASSIGNED NOTIFICATION
        // =========================
        if (savedIssue.getAssigneeId() != null
                && !savedIssue.getAssigneeId()
                        .isBlank()) {

            String eventKey
                    = "TASK_ASSIGNED-"
                    + savedIssue.getId()
                    + "-"
                    + savedIssue.getAssigneeId();

            notificationService.createNotification(
                    savedIssue.getAssigneeId(),
                    savedIssue.getProjectId(),
                    savedIssue.getId(),
                    "TASK_ASSIGNED",
                    "You have been assigned task: "
                    + savedIssue.getTitle(),
                    eventKey);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedIssue);
    }

    // =========================
    // GET ISSUES BY PROJECT
    // =========================
    @GetMapping("/project/{projectId}")
    public List<Issue> getIssuesByProject(
            @PathVariable String projectId) {

        return issueRepository
                .findByProjectId(projectId);
    }

    // =========================
    // GET SUBTASKS
    // =========================
    @GetMapping("/{id}/subtasks")
    public ResponseEntity<?> getSubtasks(
            @PathVariable String id) {

        try {

            if (!issueRepository.existsById(
                    new ObjectId(id))) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Parent task not found");
            }

            List<Issue> subtasks
                    = issueRepository
                            .findByParentTaskId(id);

            return ResponseEntity.ok(subtasks);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid issue ID");
        }
    }

    // =========================
    // GET DEPENDENT ISSUES
    // =========================
    @GetMapping("/{id}/dependents")
    public ResponseEntity<?> getDependentIssues(
            @PathVariable String id) {

        try {

            if (!issueRepository.existsById(
                    new ObjectId(id))) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Issue not found");
            }

            List<Issue> dependentIssues
                    = issueRepository
                            .findByDependencyIdsContaining(id);

            return ResponseEntity.ok(
                    dependentIssues);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid issue ID");
        }
    }

    // =========================
    // GET ISSUE BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<?> getIssueById(
            @PathVariable String id) {

        try {

            return issueRepository
                    .findById(
                            new ObjectId(id))
                    .map(ResponseEntity::ok)
                    .orElseGet(
                            () -> ResponseEntity
                                    .status(
                                            HttpStatus.NOT_FOUND)
                                    .body(null));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid issue ID");
        }
    }

    // =========================
    // UPDATE ISSUE
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateIssue(
            @PathVariable String id,
            @RequestBody Issue updated) {

        try {

            Issue issue
                    = issueRepository
                            .findById(
                                    new ObjectId(id))
                            .orElse(null);

            if (issue == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Issue not found");
            }

            // =========================
            // STORE OLD VALUES
            // =========================
            String oldStatus
                    = issue.getStatus();

            String oldAssigneeId
                    = issue.getAssigneeId();

            int oldCommentCount
                    = issue.getComments() == null
                    ? 0
                    : issue.getComments().size();

            // =========================
            // BASIC FIELDS
            // =========================
            if (updated.getTitle() != null) {

                issue.setTitle(
                        updated.getTitle());
            }

            if (updated.getDescription() != null) {

                issue.setDescription(
                        updated.getDescription());
            }

            if (updated.getPriority() != null) {

                issue.setPriority(
                        updated.getPriority());
            }

            if (updated.getAssigneeId() != null) {

                issue.setAssigneeId(
                        updated.getAssigneeId());
            }

            // =========================
            // DUE DATE
            // =========================
            if (updated.getDueDate() != null) {

                issue.setDueDate(
                        updated.getDueDate());
            }

            issue.setOrder(
                    updated.getOrder());

            if (updated.getComments() != null) {

                issue.setComments(
                        updated.getComments());
            }

            // =========================
            // PARENT TASK
            // =========================
            if (updated.getParentTaskId() != null
                    && !updated.getParentTaskId()
                            .trim()
                            .isEmpty()) {

                Issue parentTask;

                try {

                    parentTask
                            = issueRepository
                                    .findById(
                                            new ObjectId(
                                                    updated.getParentTaskId()))
                                    .orElse(null);

                } catch (IllegalArgumentException e) {

                    return ResponseEntity
                            .badRequest()
                            .body(
                                    "Invalid parent task ID");
                }

                if (parentTask == null) {

                    return ResponseEntity
                            .status(
                                    HttpStatus.NOT_FOUND)
                            .body(
                                    "Parent task not found");
                }

                if (id.equals(
                        updated.getParentTaskId())) {

                    return ResponseEntity
                            .badRequest()
                            .body(
                                    "A task cannot be its own parent");
                }

                if (parentTask.getParentTaskId() != null
                        && !parentTask
                                .getParentTaskId()
                                .trim()
                                .isEmpty()) {

                    return ResponseEntity
                            .badRequest()
                            .body(
                                    "A subtask cannot be used as a parent task");
                }

                if (!issue.getProjectId()
                        .equals(
                                parentTask.getProjectId())) {

                    return ResponseEntity
                            .badRequest()
                            .body(
                                    "Subtask must belong to the same project as parent task");
                }

                issue.setParentTaskId(
                        parentTask.getId());

                issue.setProjectId(
                        parentTask.getProjectId());

                issue.setSprintId(
                        parentTask.getSprintId());

            } else {

                if (issue.getParentTaskId() == null
                        && updated.getSprintId() != null) {

                    issue.setSprintId(
                            updated.getSprintId());
                }
            }

            // =========================
            // DEPENDENCIES
            // =========================
            if (updated.getDependencyIds() != null) {

                List<String> dependencyIds
                        = updated.getDependencyIds();

                if (dependencyIds.contains(id)) {

                    return ResponseEntity
                            .badRequest()
                            .body(
                                    "A task cannot depend on itself");
                }

                dependencyIds
                        = new ArrayList<>(
                                new HashSet<>(
                                        dependencyIds));

                for (String dependencyId
                        : dependencyIds) {

                    try {

                        Issue dependency
                                = issueRepository
                                        .findById(
                                                new ObjectId(
                                                        dependencyId))
                                        .orElse(null);

                        if (dependency == null) {

                            return ResponseEntity
                                    .status(
                                            HttpStatus.NOT_FOUND)
                                    .body(
                                            "Dependency task not found: "
                                            + dependencyId);
                        }

                        if (!issue.getProjectId()
                                .equals(
                                        dependency
                                                .getProjectId())) {

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Dependency task must belong to the same project");
                        }

                    } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Invalid dependency ID: "
                                        + dependencyId);
                    }
                }

                // =========================
                // CIRCULAR DEPENDENCY CHECK
                // =========================
                for (String dependencyId
                        : dependencyIds) {

                    if (hasDependencyPath(
                            dependencyId,
                            id,
                            new HashSet<>())) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Circular dependency is not allowed");
                    }
                }

                issue.setDependencyIds(
                        dependencyIds);
            }

            // =========================
            // STATUS
            // =========================
            String newStatus
                    = updated.getStatus();

            if (newStatus != null) {

                // =========================
                // PARENT TASK DONE VALIDATION
                // =========================
                if ("DONE".equalsIgnoreCase(
                        newStatus)) {

                    List<Issue> subtasks
                            = issueRepository
                                    .findByParentTaskId(
                                            id);

                    for (Issue subtask
                            : subtasks) {

                        if (!"DONE".equalsIgnoreCase(
                                subtask.getStatus())) {

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Parent task cannot be marked as DONE until all subtasks are completed");
                        }
                    }
                }

                // =========================
                // DEPENDENCY STATUS VALIDATION
                // =========================
                if ("IN_PROGRESS"
                        .equalsIgnoreCase(newStatus)
                        || "IN PROGRESS"
                                .equalsIgnoreCase(
                                        newStatus)
                        || "DONE"
                                .equalsIgnoreCase(
                                        newStatus)) {

                    List<String> dependencies
                            = issue.getDependencyIds();

                    if (dependencies != null) {

                        for (String dependencyId
                                : dependencies) {

                            Issue dependency
                                    = issueRepository
                                            .findById(
                                                    new ObjectId(
                                                            dependencyId))
                                            .orElse(null);

                            if (dependency != null
                                    && !"DONE"
                                            .equalsIgnoreCase(
                                                    dependency
                                                            .getStatus())) {

                                return ResponseEntity
                                        .badRequest()
                                        .body(
                                                "Task cannot start because dependency task is not completed: "
                                                + dependency
                                                        .getTitle());
                            }
                        }
                    }
                }

                issue.setStatus(newStatus);
            }

            // =========================
            // UPDATE TIMESTAMP
            // =========================
            issue.setUpdatedAt(
                    Instant.now());

            Issue savedIssue
                    = issueRepository.save(issue);

            // =========================
            // TASK ASSIGNMENT NOTIFICATION
            // =========================
            if (savedIssue.getAssigneeId() != null
                    && !savedIssue.getAssigneeId().isBlank()
                    && !savedIssue.getAssigneeId().equals(
                            oldAssigneeId)) {

                String eventKey
                        = "TASK_ASSIGNED-"
                        + savedIssue.getId()
                        + "-"
                        + savedIssue.getAssigneeId();

                notificationService.createNotification(
                        savedIssue.getAssigneeId(),
                        savedIssue.getProjectId(),
                        savedIssue.getId(),
                        "TASK_ASSIGNED",
                        "You have been assigned task: "
                        + savedIssue.getTitle(),
                        eventKey
                );
            }

            // =========================
            // CHECK STATUS CHANGE
            // =========================
            boolean statusChanged
                    = newStatus != null
                    && !newStatus.equalsIgnoreCase(
                            oldStatus == null
                                    ? ""
                                    : oldStatus);

            // =========================
            // STATUS CHANGE
            // =========================
            if (statusChanged) {

                // =========================
                // WEBSOCKET STATUS EVENT
                // =========================
                webSocketEventService.sendProjectEvent(
                        savedIssue.getProjectId(),
                        "ISSUE_STATUS_CHANGED",
                        savedIssue
                );

                // =================================================
                // STATUS CHANGE NOTIFICATION
                // SEND TO ASSIGNEE
                // =================================================
                if (savedIssue.getAssigneeId() != null
                        && !savedIssue.getAssigneeId().isBlank()) {

                    String assigneeEventKey
                            = "STATUS_CHANGED-"
                            + savedIssue.getId()
                            + "-"
                            + newStatus
                            + "-"
                            + savedIssue.getAssigneeId();

                    notificationService.createNotification(
                            savedIssue.getAssigneeId(),
                            savedIssue.getProjectId(),
                            savedIssue.getId(),
                            "STATUS_CHANGED",
                            "Task '"
                            + savedIssue.getTitle()
                            + "' status changed to "
                            + newStatus,
                            assigneeEventKey
                    );
                }

                // =================================================
                // STATUS CHANGE NOTIFICATION
                // SEND TO REPORTER
                // =================================================
                if (savedIssue.getReporterId() != null
                        && !savedIssue.getReporterId().isBlank()
                        && !savedIssue.getReporterId().equals(
                                savedIssue.getAssigneeId())) {

                    String reporterEventKey
                            = "STATUS_CHANGED-"
                            + savedIssue.getId()
                            + "-"
                            + newStatus
                            + "-"
                            + savedIssue.getReporterId();

                    notificationService.createNotification(
                            savedIssue.getReporterId(),
                            savedIssue.getProjectId(),
                            savedIssue.getId(),
                            "STATUS_CHANGED",
                            "Task '"
                            + savedIssue.getTitle()
                            + "' status changed to "
                            + newStatus,
                            reporterEventKey
                    );
                }

            } else {

                // =========================
                // GENERAL ISSUE UPDATE
                // =========================
                webSocketEventService.sendProjectEvent(
                        savedIssue.getProjectId(),
                        "ISSUE_UPDATED",
                        savedIssue
                );
            }

            // =========================
            // NEW COMMENT ADDED
            // =========================
            int newCommentCount
                    = savedIssue.getComments() == null
                    ? 0
                    : savedIssue.getComments().size();

            if (newCommentCount > oldCommentCount) {

                webSocketEventService.sendProjectEvent(
                        savedIssue.getProjectId(),
                        "COMMENT_ADDED",
                        savedIssue
                );
            }

            return ResponseEntity.ok(
                    savedIssue);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid issue ID");
        }
    }

    // =========================
    // CIRCULAR DEPENDENCY HELPER
    // =========================
    private boolean hasDependencyPath(
            String currentIssueId,
            String targetIssueId,
            Set<String> visited) {

        if (currentIssueId.equals(
                targetIssueId)) {

            return true;
        }

        if (!visited.add(
                currentIssueId)) {

            return false;
        }

        try {

            Issue currentIssue
                    = issueRepository
                            .findById(
                                    new ObjectId(
                                            currentIssueId))
                            .orElse(null);

            if (currentIssue == null) {

                return false;
            }

            List<String> dependencies
                    = currentIssue
                            .getDependencyIds();

            if (dependencies == null) {

                return false;
            }

            for (String dependencyId
                    : dependencies) {

                if (hasDependencyPath(
                        dependencyId,
                        targetIssueId,
                        visited)) {

                    return true;
                }
            }

        } catch (IllegalArgumentException e) {

            return false;
        }

        return false;
    }

    // =========================
    // DELETE ISSUE
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIssue(
            @PathVariable String id) {

        try {

            ObjectId objectId =
                    new ObjectId(id);

            Issue issue =
                    issueRepository
                            .findById(objectId)
                            .orElse(null);

            if (issue == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Issue not found");
            }

            // =========================
            // DELETE ATTACHMENTS
            // =========================

            attachmentService
                    .deleteAttachmentsByIssue(
                            issue.getId()
                    );

            // =========================
            // DELETE ISSUE
            // =========================

            issueRepository.deleteById(
                    objectId
            );

            // =========================
            // WEBSOCKET DELETE EVENT
            // =========================

            webSocketEventService
                    .sendProjectEvent(
                            issue.getProjectId(),
                            "ISSUE_DELETED",
                            issue
                    );

            return ResponseEntity.ok(
                    "Issue and its attachments deleted successfully"
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid issue ID");

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}
