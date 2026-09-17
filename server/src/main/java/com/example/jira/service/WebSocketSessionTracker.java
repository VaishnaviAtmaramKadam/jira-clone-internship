package com.example.jira.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class WebSocketSessionTracker {

    // sessionId -> userId
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    // userId -> set of sessionIds
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void addSession(String sessionId, String userId) {

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        if (userId == null || userId.isBlank()) {
            return;
        }

        activeSessions.put(sessionId, userId);

        userSessions
                .computeIfAbsent(
                        userId,
                        key -> ConcurrentHashMap.newKeySet()
                )
                .add(sessionId);
    }

    public void removeSession(String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        String userId = activeSessions.remove(sessionId);

        if (userId == null) {
            return;
        }

        Set<String> sessions = userSessions.get(userId);

        if (sessions != null) {

            sessions.remove(sessionId);

            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    public boolean isUserActive(String userId) {

        if (userId == null || userId.isBlank()) {
            return false;
        }

        Set<String> sessions = userSessions.get(userId);

        return sessions != null && !sessions.isEmpty();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public int getActiveUserCount() {
        return userSessions.size();
    }
}
