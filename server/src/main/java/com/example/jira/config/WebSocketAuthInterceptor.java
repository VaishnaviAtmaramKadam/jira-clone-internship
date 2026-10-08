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
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;
    private final Projectrepository projectrepository;
    private final WebSocketSessionTracker sessionTracker;

    public WebSocketAuthInterceptor(
            UserRepository userRepository,
            Projectrepository projectrepository,
            WebSocketSessionTracker sessionTracker) {

        this.userRepository = userRepository;
        this.projectrepository = projectrepository;
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

        /*
         * STEP 1:
         * WebSocket CONNECT
         */
        if (StompCommand.CONNECT.equals(command)) {

            String userId
                    = accessor.getFirstNativeHeader("userId");

            if (userId == null || userId.isBlank()) {
                throw new MessagingException(
                        "WebSocket connection rejected: userId is required"
                );
            }

            ObjectId userObjectId;

            try {
                userObjectId
                        = new ObjectId(userId);
            } catch (IllegalArgumentException e) {
                throw new MessagingException(
                        "WebSocket connection rejected: invalid userId"
                );
            }

            User user
                    = userRepository.findById(userObjectId)
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

            /*
             * Store userId inside WebSocket session
             */
            accessor.getSessionAttributes()
                    .put("userId", userId);

            String sessionId
                    = accessor.getSessionId();

            if (sessionId != null) {
                sessionTracker.addSession(
                        sessionId,
                        userId
                );
            }

            System.out.println(
                    "WebSocket authenticated for user: "
                    + userId
            );

            return message;
        }

        /*
         * STEP 2:
         * WebSocket SUBSCRIBE
         */
        if (StompCommand.SUBSCRIBE.equals(command)) {

            String destination
                    = accessor.getDestination();

            if (destination == null) {
                throw new MessagingException(
                        "Subscription rejected: destination missing"
                );
            }

            /*
             * We only secure project topics.
             */
            if (destination.startsWith(
                    "/topic/project/")) {

                String projectId
                        = destination.substring(
                                "/topic/project/".length()
                        );

                String userId = null;

                if (accessor.getSessionAttributes() != null) {
                    userId
                            = (String) accessor
                                    .getSessionAttributes()
                                    .get("userId");
                }

                if (userId == null || userId.isBlank()) {
                    throw new MessagingException(
                            "Subscription rejected: user not authenticated"
                    );
                }

                ObjectId projectObjectId;

                try {
                    projectObjectId
                            = new ObjectId(projectId);
                } catch (IllegalArgumentException e) {
                    throw new MessagingException(
                            "Subscription rejected: invalid project id"
                    );
                }

                Project project
                        = projectrepository
                                .findById(projectObjectId)
                                .orElseThrow(()
                                        -> new MessagingException(
                                        "Subscription rejected: project not found"
                                )
                                );

                boolean isOwner
                        = userId.equals(
                                project.getOwnerId()
                        );

                List<String> memberIds
                        = project.getMemberIds();

                boolean isMember
                        = memberIds != null
                        && memberIds.contains(userId);

                /*
                 * Only project owner or project member
                 * can subscribe.
                 */
                if (!isOwner && !isMember) {

                    throw new MessagingException(
                            "Subscription rejected: user is not a project member"
                    );
                }

                System.out.println(
                        "WebSocket subscription allowed for user: "
                        + userId
                        + " project: "
                        + projectId
                );
            }

            return message;
        }

        /*
         * STEP 3:
         * WebSocket DISCONNECT
         */
        if (StompCommand.DISCONNECT.equals(command)) {

            String sessionId
                    = accessor.getSessionId();

            if (sessionId != null) {
                sessionTracker.removeSession(
                        sessionId
                );
            }

            System.out.println(
                    "WebSocket session disconnected: "
                    + sessionId
            );

            return message;
        }

        return message;
    }
}
