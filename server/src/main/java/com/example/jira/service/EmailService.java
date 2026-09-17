package com.example.jira.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(
            String to,
            String subject,
            String text) {

        if (to == null || to.isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            System.out.println(
                    "EMAIL SENT SUCCESSFULLY TO: " + to
            );

        } catch (Exception e) {

            System.err.println(
                    "EMAIL SENDING FAILED TO: " + to
            );

            e.printStackTrace();
        }
    }
}
