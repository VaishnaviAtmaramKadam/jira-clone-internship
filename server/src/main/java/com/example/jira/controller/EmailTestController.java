package com.example.jira.controller;

import com.example.jira.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/api/test-email")
    public String testEmail(@RequestParam String to) {

        emailService.sendEmail(
                to,
                "Jira Clone Email Test",
                "This is a test email from Jira Clone."
        );

        return "Test email sent successfully.";
    }
}