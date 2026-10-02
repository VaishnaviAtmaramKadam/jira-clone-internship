package com.example.jira.config;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.example.jira.model.Project;
import com.example.jira.model.User;
import com.example.jira.repository.Projectrepository;
import com.example.jira.repository.UserRepository;
import com.example.jira.service.WebSocketSessionTracker;

@Component
public class WebSocketSubscriptionInterceptor
        implements ChannelInterceptor {

    private final UserRepository userRepository;
    private final Projectrepository projectRepository;
    private final WebSocketSessionTracker sessionTracker;

    public WebSocketSubscriptionInterceptor(
            UserRepository userRepository,
            Projectrepository projectRepository,
            WebSocketSessionTracker sessionTracker) {

        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.sessionTracker = sessionTracker;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor
                = StompHeaderAccessor.wrap(message);

        StompCommand command
                = accessor.getCommand();

        if (command == null) {
            return message;
        }

        // =========================================
        // 1. CONNECT
        // =========================================
        if (StompCommand.CONNECT.equals(command)) {

            String userId
                    = accessor.getFirstNativeHeader("userId");

            if (userId == null || userId.isBlank()) {
                throw new MessagingException(
                        "WebSocket connection rejected: userId is required"
                );
            }

            final String authenticatedUserId
                    = userId.trim();

            ObjectId userObjectId;

            try {
                userObjectId
                        = new ObjectId(authenticatedUserId);

            } catch (IllegalArgumentException e) {

                throw new MessagingException(
                        "WebSocket connection rejected: invalid userId"
                );
            }

            User user
                    = userRepository
                            .findById(userObjectId)
                            .orElseThrow(()
                                    -> new MessagingException(
                                    "WebSocket connection rejected: user not found"
                            )
                            );

            if (!user.isActive()) {
                throw new MessagingException(
                        "WebSocket connection rejected: account is deactivated"
                );
            }

            // Store userId in WebSocket session
            accessor.getSessionAttributes()
                    .put(
                            "userId",
                            authenticatedUserId
                    );

            String sessionId
                    = accessor.getSessionId();

            if (sessionId != null) {

                sessionTracker.addSession(
                        sessionId,
                        authenticatedUserId
                );
            }

            System.out.println(
                    "WebSocket authenticated for user: "
                    + authenticatedUserId
            );

            return message;
        }

        // =========================================
        // 2. SUBSCRIBE
        // =========================================
        if (StompCommand.SUBSCRIBE.equals(command)) {

            String destination
                    = accessor.getDestination();

            if (destination == null
                    || destination.isBlank()) {

                throw new MessagingException(
                        "Subscription rejected: destination missing"
                );
            }

            // Project WebSocket destination
            if (destination.startsWith(
                    "/topic/project/")) {

                String projectId
                        = destination
                                .substring(
                                        "/topic/project/"
                                                .length()
                                )
                                .trim();

                // -----------------------------------------
                // Get authenticated user from session
                // -----------------------------------------
                String userId = null;

                if (accessor.getSessionAttributes() != null) {

                    Object storedUserId
                            = accessor.getSessionAttributes()
                                    .get("userId");

                    if (storedUserId != null) {

                        userId
                                = storedUserId
                                        .toString()
                                        .trim();
                    }
                }

                if (userId == null
                        || userId.isBlank()) {

                    throw new MessagingException(
                            "Subscription rejected: user not authenticated"
                    );
                }

                final String authenticatedUserId
                        = userId;

                // -----------------------------------------
                // Validate project ID
                // -----------------------------------------
                ObjectId projectObjectId;

                try {

                    projectObjectId
                            = new ObjectId(projectId);

                } catch (IllegalArgumentException e) {

                    throw new MessagingException(
                            "Subscription rejected: invalid project id"
                    );
                }

                // -----------------------------------------
                // Find project
                // -----------------------------------------
                Project project
                        = projectRepository
                                .findById(projectObjectId)
                                .orElseThrow(()
                                        -> new MessagingException(
                                        "Subscription rejected: project not found"
                                )
                                );

                // -----------------------------------------
                // Check project owner
                // -----------------------------------------
                boolean isOwner
                        = authenticatedUserId.equals(
                                project.getOwnerId()
                        );

                // -----------------------------------------
                // Check project member
                // -----------------------------------------
                List<String> memberIds
                        = project.getMemberIds();

                boolean isMember = false;

                if (memberIds != null) {

                    isMember
                            = memberIds.stream()
                                    .filter(
                                            id -> id != null
                                    )
                                    .map(id -> id.trim())
                                    .anyMatch(
                                            id -> id.equals(
                                                    authenticatedUserId
                                            )
                                    );
                }

                // -----------------------------------------
                // Authorization
                // -----------------------------------------
                if (!isOwner && !isMember) {

                    throw new MessagingException(
                            "Subscription rejected: user is not a project member"
                    );
                }

                System.out.println(
                        "WebSocket subscription allowed"
                        + " | user: "
                        + authenticatedUserId
                        + " | project: "
                        + projectId
                );
            }

            return message;
        }

        // =========================================
        // Other STOMP commands
        // =========================================
        return message;
    }
}
