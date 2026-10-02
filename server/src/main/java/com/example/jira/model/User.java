package com.example.jira.model;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private ObjectId id;

    private String name;
    private String email;
    private String password;
    private String role;
    private String group;
    private String avatar;
    private String phone;

    // Account status
    private boolean active = true;

    // Email verification
    private boolean emailVerified = true;
    private String pendingEmail;
    private String emailVerificationToken;
    private Instant emailVerificationExpiry;

    // Password Reset
    private String passwordResetTokenHash;
    private Instant passwordResetExpiry;

    // Email notification preference
    private boolean emailNotificationsEnabled = true;

    // Timestamps
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // =====================================================
    // ID
    // =====================================================
    public String getId() {
        return id != null ? id.toHexString() : null;
    }

    public ObjectId getObjectId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    // =====================================================
    // NAME
    // =====================================================
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // =====================================================
    // EMAIL
    // =====================================================
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // =====================================================
    // PASSWORD
    // =====================================================
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // =====================================================
    // ROLE
    // =====================================================
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // =====================================================
    // GROUP
    // =====================================================
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    // =====================================================
    // AVATAR
    // =====================================================
    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    // =====================================================
    // PHONE
    // =====================================================
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // =====================================================
    // ACTIVE
    // =====================================================
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // =====================================================
    // EMAIL VERIFIED
    // =====================================================
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    // =====================================================
    // PENDING EMAIL
    // =====================================================
    public String getPendingEmail() {
        return pendingEmail;
    }

    public void setPendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
    }

    // =====================================================
    // EMAIL VERIFICATION TOKEN
    // =====================================================
    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    // =====================================================
    // EMAIL VERIFICATION EXPIRY
    // =====================================================
    public Instant getEmailVerificationExpiry() {
        return emailVerificationExpiry;
    }

    public void setEmailVerificationExpiry(
            Instant emailVerificationExpiry) {

        this.emailVerificationExpiry = emailVerificationExpiry;
    }

    // =====================================================
    // EMAIL NOTIFICATIONS ENABLED
    // =====================================================
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(
            boolean emailNotificationsEnabled) {

        this.emailNotificationsEnabled
                = emailNotificationsEnabled;
    }

    // =====================================================
    // CREATED AT
    // =====================================================
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // =====================================================
    // UPDATED AT
    // =====================================================
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }


    // PASSWORD RESET TOKEN HASH
    public String getPasswordResetTokenHash() {
        return passwordResetTokenHash;
    }

    public void setPasswordResetTokenHash(String passwordResetTokenHash) {
        this.passwordResetTokenHash = passwordResetTokenHash;
    }

    // PASSWORD RESET EXPIRY
    public Instant getPasswordResetExpiry() {
        return passwordResetExpiry;
    }

    public void setPasswordResetExpiry(Instant passwordResetExpiry) {
        this.passwordResetExpiry = passwordResetExpiry;
    }
}
