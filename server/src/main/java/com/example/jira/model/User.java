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

    private boolean active;
    private boolean emailVerified;
    private boolean emailNotificationsEnabled;

    private String pendingEmail;
    private String emailVerificationToken;
    private Instant emailVerificationExpiry;

    private Instant createdAt;
    private Instant updatedAt;

    // =========================
    // DEFAULT CONSTRUCTOR
    // =========================
    public User() {
    }

    // =========================
    // GET ID
    // =========================
    public ObjectId getId() {
        return id;
    }

    // =========================
    // SET ID
    // =========================
    public void setId(ObjectId id) {
        this.id = id;
    }

    // =========================
    // GET NAME
    // =========================
    public String getName() {
        return name;
    }

    // =========================
    // SET NAME
    // =========================
    public void setName(String name) {
        this.name = name;
    }

    // =========================
    // GET EMAIL
    // =========================
    public String getEmail() {
        return email;
    }

    // =========================
    // SET EMAIL
    // =========================
    public void setEmail(String email) {
        this.email = email;
    }

    // =========================
    // GET PASSWORD
    // =========================
    public String getPassword() {
        return password;
    }

    // =========================
    // SET PASSWORD
    // =========================
    public void setPassword(String password) {
        this.password = password;
    }

    // =========================
    // GET ROLE
    // =========================
    public String getRole() {
        return role;
    }

    // =========================
    // SET ROLE
    // =========================
    public void setRole(String role) {
        this.role = role;
    }

    // =========================
    // GET GROUP
    // =========================
    public String getGroup() {
        return group;
    }

    // =========================
    // SET GROUP
    // =========================
    public void setGroup(String group) {
        this.group = group;
    }

    // =========================
    // GET AVATAR
    // =========================
    public String getAvatar() {
        return avatar;
    }

    // =========================
    // SET AVATAR
    // =========================
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    // =========================
    // GET PHONE
    // =========================
    public String getPhone() {
        return phone;
    }

    // =========================
    // SET PHONE
    // =========================
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // =========================
    // GET ACTIVE
    // =========================
    public boolean isActive() {
        return active;
    }

    // =========================
    // SET ACTIVE
    // =========================
    public void setActive(boolean active) {
        this.active = active;
    }

    // =========================
    // GET EMAIL VERIFIED
    // =========================
    public boolean isEmailVerified() {
        return emailVerified;
    }

    // =========================
    // SET EMAIL VERIFIED
    // =========================
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    // =========================
    // GET EMAIL NOTIFICATIONS
    // =========================
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    // =========================
    // SET EMAIL NOTIFICATIONS
    // =========================
    public void setEmailNotificationsEnabled(
            boolean emailNotificationsEnabled) {

        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    // =========================
    // GET PENDING EMAIL
    // =========================
    public String getPendingEmail() {
        return pendingEmail;
    }

    // =========================
    // SET PENDING EMAIL
    // =========================
    public void setPendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
    }

    // =========================
    // GET EMAIL VERIFICATION TOKEN
    // =========================
    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    // =========================
    // SET EMAIL VERIFICATION TOKEN
    // =========================
    public void setEmailVerificationToken(
            String emailVerificationToken) {

        this.emailVerificationToken = emailVerificationToken;
    }

    // =========================
    // GET EMAIL VERIFICATION EXPIRY
    // =========================
    public Instant getEmailVerificationExpiry() {
        return emailVerificationExpiry;
    }

    // =========================
    // SET EMAIL VERIFICATION EXPIRY
    // =========================
    public void setEmailVerificationExpiry(
            Instant emailVerificationExpiry) {

        this.emailVerificationExpiry = emailVerificationExpiry;
    }

    // =========================
    // GET CREATED AT
    // =========================
    public Instant getCreatedAt() {
        return createdAt;
    }

    // =========================
    // SET CREATED AT
    // =========================
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // =========================
    // GET UPDATED AT
    // =========================
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // =========================
    // SET UPDATED AT
    // =========================
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}