package com.example.jira.config;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.jira.service.WebSocketSessionTracker;

@Component
public class WebSocketSessionEventListener {

    private final WebSocketSessionTracker sessionTracker;

    public WebSocketSessionEventListener(
            WebSocketSessionTracker sessionTracker) {

        this.sessionTracker = sessionTracker;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {

        String sessionId
                = event.getMessage()
                        .getHeaders()
                        .get("simpSessionId", String.class);

        if (sessionId == null) {
            return;
        }

        /*
         * At this stage the WebSocket connection is tracked.
         * User identification will be connected with authentication
         * when the application's WebSocket authentication is added.
         */
        System.out.println(
                "WebSocket session connected: " + sessionId
        );
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        sessionTracker.removeSession(sessionId);

        System.out.println(
                "WebSocket session disconnected: " + sessionId
        );

        System.out.println(
                "Active sessions: "
                + sessionTracker.getActiveSessionCount()
        );
    }
}
