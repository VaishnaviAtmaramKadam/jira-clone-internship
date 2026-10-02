
package com.example.jira.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jira.model.User;
import com.example.jira.repository.UserRepository;

@CrossOrigin(
    origins = {
        "http://localhost:3000",
        "https://jira-clone-internship-1.onrender.com"
    }
)
@RestController
@RequestMapping("/api/users")
public class Usercontroller {

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // =========================
    // SIGNUP
    // =========================
    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody User user) {

        if (user.getEmail() == null
                || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (user.getPassword() == null) {
            throw new RuntimeException("Password is required");
        }

        String email = normalizeEmail(user.getEmail());

        if (!email.matches(EMAIL_REGEX)) {
            throw new RuntimeException("Invalid email format");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        validateStrongPassword(user.getPassword());

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole("USER");
        user.setActive(true);
        user.setEmailVerified(true);
        user.setEmailNotificationsEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);

        return Map.of(
                "message", "Account created successfully. Please login.",
                "user", saved
        );
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public User login(@RequestBody User loginRequest) {

        if (loginRequest.getEmail() == null
                || loginRequest.getPassword() == null) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userRepository
                .findByEmail(normalizeEmail(loginRequest.getEmail()))
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException(
                    "Account is deactivated. Please contact the administrator."
            );
        }

        return user;
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(
            @RequestBody User request) {

        String genericMessage =
                "If the email is registered, a password reset link has been sent.";

        String email = normalizeEmail(
                request == null ? null : request.getEmail()
        );

        if (email.isEmpty() || !email.matches(EMAIL_REGEX)) {
            return Map.of("message", genericMessage);
        }

        userRepository.findByEmail(email).ifPresent(user -> {

            if (!user.isActive()) {
                return;
            }

            String token = generateToken();

            user.setPasswordResetTokenHash(sha256(token));
            user.setPasswordResetExpiry(
                    Instant.now().plus(1, ChronoUnit.HOURS)
            );
            user.setUpdatedAt(Instant.now());

            userRepository.save(user);

            String baseUrl = frontendUrl.replaceAll("/+$", "");
            String link = baseUrl
                    + "/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(mailUsername);
            message.setTo(email);
            message.setSubject("ProjectHub Password Reset");

            message.setText(
                    "Hello " + user.getName() + ",\n\n"
                    + "We received a request to reset your password.\n\n"
                    + "Click the link below to reset your password:\n"
                    + link + "\n\n"
                    + "This link will expire in 1 hour.\n\n"
                    + "If you did not request a password reset, "
                    + "please ignore this email.\n\n"
                    + "Regards,\n"
                    + "ProjectHub Team"
            );

            mailSender.send(message);
        });

        return Map.of("message", genericMessage);
    }

    // =========================
    // RESET PASSWORD
    // =========================
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        if (request == null
                || request.token == null
                || request.newPassword == null) {
            throw new RuntimeException(
                    "Token and new password are required"
            );
        }

        User user = userRepository
                .findByPasswordResetTokenHash(sha256(request.token))
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid or expired reset token"
                        ));

        if (user.getPasswordResetExpiry() == null
                || Instant.now().isAfter(
                        user.getPasswordResetExpiry())) {
            throw new RuntimeException(
                    "Invalid or expired reset token"
            );
        }

        validateStrongPassword(request.newPassword);

        user.setPassword(
                passwordEncoder.encode(request.newPassword)
        );
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiry(null);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return Map.of(
                "message",
                "Password reset successfully. Please login."
        );
    }

    // =========================
    // GET ALL USERS
    // =========================
    // TODO: Restrict to ADMIN once authentication is added
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // =========================
    // GET USER BY ID
    // =========================
    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return findUserOrThrow(id);
    }

    // =========================
    // EDIT PROFILE
    // =========================
    @PutMapping("/{id}")
    public User editProfile(
            @PathVariable String id,
            @RequestBody User updatedUser) {

        User user = findUserOrThrow(id);

        if (updatedUser.getName() != null
                && !updatedUser.getName().trim().isEmpty()) {
            user.setName(updatedUser.getName().trim());
        }

        if (updatedUser.getGroup() != null) {
            user.setGroup(updatedUser.getGroup());
        }

        if (updatedUser.getAvatar() != null) {
            user.setAvatar(updatedUser.getAvatar());
        }

        if (updatedUser.getPhone() != null) {
            String phone = updatedUser.getPhone().trim();

            if (!phone.matches("^[0-9]{10}$")) {
                throw new RuntimeException(
                        "Phone number must contain exactly 10 digits"
                );
            }

            user.setPhone(phone);
        }

        user.setEmailNotificationsEnabled(
                updatedUser.isEmailNotificationsEnabled()
        );
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    // =========================
    // REQUEST EMAIL CHANGE
    // =========================
    @PostMapping("/{id}/change-email")
    public String requestEmailChange(
            @PathVariable String id,
            @RequestBody User emailRequest) {

        User user = findUserOrThrow(id);

        String newEmail = emailRequest.getEmail();

        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new RuntimeException("New email is required");
        }

        newEmail = normalizeEmail(newEmail);

        if (!newEmail.matches(EMAIL_REGEX)) {
            throw new RuntimeException("Invalid email format");
        }

        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        String token = UUID.randomUUID().toString();

        user.setPendingEmail(newEmail);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiry(
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        // Development/testing response
        return "Email verification token generated: " + token;
    }

    // =========================
    // VERIFY EMAIL
    // =========================
    @PostMapping("/verify-email/{token}")
    public User verifyEmail(@PathVariable String token) {

        User user = userRepository
                .findByEmailVerificationToken(token)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid verification token"
                        ));

        if (user.getEmailVerificationExpiry() == null
                || Instant.now().isAfter(
                        user.getEmailVerificationExpiry())) {
            throw new RuntimeException(
                    "Email verification token has expired"
            );
        }

        if (user.getPendingEmail() == null) {
            throw new RuntimeException(
                    "No pending email change found"
            );
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        user.setEmailVerified(true);
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    // =========================
    // CHANGE PASSWORD
    // =========================
    @PostMapping("/{id}/change-password")
    public String changePassword(
            @PathVariable String id,
            @RequestBody PasswordChangeRequest request) {

        User user = findUserOrThrow(id);

        if (request == null
                || request.currentPassword == null
                || request.newPassword == null) {
            throw new RuntimeException(
                    "Current password and new password are required"
            );
        }

        if (!passwordEncoder.matches(
                request.currentPassword,
                user.getPassword())) {
            throw new RuntimeException(
                    "Current password is incorrect"
            );
        }

        validateStrongPassword(request.newPassword);

        if (passwordEncoder.matches(
                request.newPassword,
                user.getPassword())) {
            throw new RuntimeException(
                    "New password must be different from current password"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.newPassword)
        );
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return "Password changed successfully";
    }

    // =========================
    // DEACTIVATE ACCOUNT
    // =========================
    // TODO: Restrict to the owner or an ADMIN
    @PutMapping("/{id}/deactivate")
    public String deactivateAccount(@PathVariable String id) {

        User user = findUserOrThrow(id);

        user.setActive(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return "Account deactivated successfully";
    }

    // =========================
    // ACTIVATE ACCOUNT
    // =========================
    // TODO: Restrict to ADMIN
    @PutMapping("/{id}/activate")
    public String activateAccount(@PathVariable String id) {

        User user = findUserOrThrow(id);

        user.setActive(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return "Account activated successfully";
    }

    // =========================
    // HELPERS
    // =========================
    private User findUserOrThrow(String id) {

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        return userRepository
                .findById(objectId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Hashing failed");
        }
    }

    private void validateStrongPassword(String password) {

        if (password.length() < 8) {
            throw new RuntimeException(
                    "Password must contain at least 8 characters"
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException(
                    "Password must contain at least one uppercase letter"
            );
        }

        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException(
                    "Password must contain at least one lowercase letter"
            );
        }

        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException(
                    "Password must contain at least one number"
            );
        }

        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new RuntimeException(
                    "Password must contain at least one special character"
            );
        }
    }

    // =========================
    // DTOs
    // =========================
    public static class PasswordChangeRequest {
        public String currentPassword;
        public String newPassword;
    }

    public static class ResetPasswordRequest {
        public String token;
        public String newPassword;
    }
}

