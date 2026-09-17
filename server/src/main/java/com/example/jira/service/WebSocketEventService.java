package com.example.jira.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendProjectEvent(
            String projectId,
            String eventType,
            Object data) {

        if (projectId == null || projectId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> event = new HashMap<>();

        event.put("eventType", eventType);
        event.put("projectId", projectId);
        event.put("data", data);

        String destination = "/topic/project/" + projectId;

        messagingTemplate.convertAndSend(
                destination,
                (Object) event
        );
    }
}
