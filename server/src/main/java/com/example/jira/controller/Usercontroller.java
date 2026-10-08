
package com.example.jira.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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

@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://jira-clone-internship-1.onrender.com"
})
@RestController
@RequestMapping("/api/users")
public class Usercontroller {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================
    // SIGNUP
    // =========================
    @PostMapping("/signup")
    public User signup(@RequestBody User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        user.setRole(
                user.getRole() == null ? "USER" : user.getRole()
        );

        user.setActive(true);
        user.setEmailVerified(true);
        user.setEmailNotificationsEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public User login(@RequestBody User loginRequest) {

        User user = userRepository
                .findByEmail(loginRequest.getEmail())
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        if (!user.isActive()) {
            throw new RuntimeException(
                    "Account is deactivated. Please contact the administrator."
            );
        }

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }

    // =========================
    // GET ALL USERS
    // =========================
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // =========================
    // GET USER BY ID
    // =========================
    @GetMapping("/{id}")
    public User getUserById(
            @PathVariable String id) {

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        return userRepository
                .findById(objectId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );
    }

    // =========================
    // EDIT PROFILE
    // =========================
    @PutMapping("/{id}")
    public User editProfile(
            @PathVariable String id,
            @RequestBody User updatedUser) {

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        User user = userRepository
                .findById(objectId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        if (updatedUser.getName() != null
                && !updatedUser.getName().trim().isEmpty()) {

            user.setName(
                    updatedUser.getName().trim()
            );
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

        System.out.println(
                "Email Notifications saved for "
                        + user.getEmail()
                        + " = "
                        + user.isEmailNotificationsEnabled()
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

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        User user = userRepository
                .findById(objectId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        String newEmail = emailRequest.getEmail();

        if (newEmail == null
                || newEmail.trim().isEmpty()) {

            throw new RuntimeException(
                    "New email is required"
            );
        }

        newEmail = newEmail.trim().toLowerCase();

        if (!newEmail.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            throw new RuntimeException(
                    "Invalid email format"
            );
        }

        if (userRepository
                .findByEmail(newEmail)
                .isPresent()) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        String token = UUID.randomUUID().toString();

        user.setPendingEmail(newEmail);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiry(
                Instant.now().plus(
                        24,
                        ChronoUnit.HOURS
                )
        );
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return "Email verification token generated: "
                + token;
    }

    // =========================
    // VERIFY EMAIL
    // =========================
    @PostMapping("/verify-email/{token}")
    public User verifyEmail(
            @PathVariable String token) {

        User user = userRepository
                .findAll()
                .stream()
                .filter(
                        u -> token.equals(
                                u.getEmailVerificationToken()
                        )
                )
                .findFirst()
                .orElseThrow(
                        () -> new RuntimeException(
                                "Invalid verification token"
                        )
                );

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

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        User user = userRepository
                .findById(objectId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        if (request.currentPassword == null
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
                passwordEncoder.encode(
                        request.newPassword
                )
        );

        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return "Password changed successfully";
    }

    // =========================
    // DEACTIVATE ACCOUNT
    // =========================
    @PutMapping("/{id}/deactivate")
    public String deactivateAccount(
            @PathVariable String id) {

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        User user = userRepository
                .findById(objectId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        user.setActive(false);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return "Account deactivated successfully";
    }

    // =========================
    // ACTIVATE ACCOUNT
    // =========================
    @PutMapping("/{id}/activate")
    public String activateAccount(
            @PathVariable String id) {

        ObjectId objectId;

        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid user id");
        }

        User user = userRepository
                .findById(objectId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        user.setActive(true);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return "Account activated successfully";
    }

    // =========================
    // STRONG PASSWORD VALIDATION
    // =========================
    private void validateStrongPassword(
            String password) {

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
    // PASSWORD REQUEST DTO
    // =========================
    public static class PasswordChangeRequest {

        public String currentPassword;

        public String newPassword;
    }
}

