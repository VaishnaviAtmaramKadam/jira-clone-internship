package com.example.jira.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.jira.model.Issue;
import com.example.jira.repository.IssueRepository;

@Service
public class DueDateReminderScheduler {

    private final IssueRepository issueRepository;
    private final NotificationService notificationService;

    public DueDateReminderScheduler(
            IssueRepository issueRepository,
            NotificationService notificationService) {

        this.issueRepository = issueRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 60000)
    public void sendDueDateReminders() {

        Instant now = Instant.now();

        List<Issue> issues = issueRepository.findAll();

        for (Issue issue : issues) {

            // Due date नसल्यास skip
            if (issue.getDueDate() == null) {
                continue;
            }

            // Assignee नसल्यास skip
            if (issue.getAssigneeId() == null
                    || issue.getAssigneeId().isBlank()) {
                continue;
            }

            // Task DONE असल्यास reminder नको
            if ("DONE".equalsIgnoreCase(issue.getStatus())) {
                continue;
            }

            Instant dueDate = issue.getDueDate();

            // Due date च्या 24 hours आधी
            Instant reminderStart
                    = dueDate.minus(Duration.ofHours(24));

            /*
             * Current time:
             * reminderStart नंतर
             * आणि dueDate च्या आधी
             */
            if (!now.isBefore(reminderStart)
                    && now.isBefore(dueDate)) {

                String issueId = issue.getId();

                if (issueId == null || issueId.isBlank()) {
                    continue;
                }

                /*
                 * Same task + same due date साठी
                 * duplicate notification होऊ नये.
                 */
                String eventKey
                        = "DUE_DATE_REMINDER-"
                        + issueId
                        + "-"
                        + dueDate.toEpochMilli();

                String message
                        = "Reminder: Task '"
                        + issue.getTitle()
                        + "' is due in 24 hours.";

                notificationService.createNotification(
                        issue.getAssigneeId(),
                        issue.getProjectId(),
                        issueId,
                        "DUE_DATE_REMINDER",
                        message,
                        eventKey
                );

                System.out.println(
                        "DUE DATE REMINDER CHECKED: "
                        + issue.getTitle()
                );
            }
        }
    }
}
