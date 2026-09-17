import {
    Client,
    IMessage,
    StompSubscription,
} from "@stomp/stompjs";

let stompClient: Client | null = null;
let projectSubscription: StompSubscription | null = null;

export function connectWebSocket(
    projectId: string,
    userId: string,
    onMessage: (message: any) => void
) {
    if (!projectId || !userId) {
        console.error(
            "WebSocket connection skipped: projectId or userId missing"
        );
        return;
    }

    // If already connected/connecting, do not create another connection.
    if (stompClient?.active) {
        console.log(
            "WebSocket already active",
            "| user:",
            userId,
            "| project:",
            projectId
        );
        return;
    }

    // Clean old client if any.
    if (stompClient) {
        try {
            stompClient.deactivate();
        } catch (error) {
            console.error(
                "Failed to deactivate old WebSocket client:",
                error
            );
        }

        stompClient = null;
        projectSubscription = null;
    }

    const client = new Client({
        brokerURL: "ws://localhost:8080/ws",

        // Automatically reconnect after temporary connection failure.
        reconnectDelay: 5000,

        connectHeaders: {
            userId: userId,
        },

        debug: (message) => {
            console.log("[STOMP]", message);
        },

        // =====================================================
        // CONNECTED
        // =====================================================
        onConnect: () => {
            console.log(
                "WebSocket CONNECTED",
                "| user:",
                userId,
                "| project:",
                projectId
            );

            const destination =
                `/topic/project/${projectId}`;

            // Prevent duplicate subscription.
            if (projectSubscription) {
                console.log(
                    "WebSocket subscription already exists:",
                    destination
                );

                return;
            }

            console.log(
                "Attempting WebSocket SUBSCRIBE:",
                destination
            );

            projectSubscription =
                client.subscribe(
                    destination,
                    (message: IMessage) => {
                        try {
                            if (!message.body) {
                                console.warn(
                                    "WebSocket message body is empty"
                                );
                                return;
                            }

                            const data =
                                JSON.parse(message.body);

                            console.log(
                                "WebSocket event received:",
                                data
                            );

                            onMessage(data);

                        } catch (error) {

                            console.error(
                                "Invalid WebSocket message:",
                                error
                            );
                        }
                    }
                );

            console.log(
                "WebSocket SUBSCRIBED:",
                destination
            );
        },

        // =====================================================
        // DISCONNECTED
        // =====================================================
        onDisconnect: () => {
            console.log(
                "WebSocket DISCONNECTED",
                "| user:",
                userId
            );

            projectSubscription = null;
        },

        // =====================================================
        // STOMP ERROR
        // =====================================================
        onStompError: (frame) => {
            console.error(
                "WebSocket STOMP ERROR:",
                frame.headers["message"]
            );

            console.error(
                "STOMP ERROR DETAILS:",
                frame.body
            );
        },

        // =====================================================
        // WEBSOCKET ERROR
        // =====================================================
        onWebSocketError: (error) => {
            console.error(
                "WebSocket ERROR:",
                error
            );
        },

        // =====================================================
        // WEBSOCKET CLOSED
        // =====================================================
        onWebSocketClose: (event) => {
            console.log(
                "WebSocket CLOSED",
                "| code:",
                event.code,
                "| reason:",
                event.reason
            );

            projectSubscription = null;
        },
    });

    stompClient = client;

    console.log(
        "Activating WebSocket...",
        "| user:",
        userId,
        "| project:",
        projectId
    );

    client.activate();
}

// =============================================================
// DISCONNECT
// =============================================================
export function disconnectWebSocket() {

    if (projectSubscription) {

        try {
            projectSubscription.unsubscribe();

        } catch (error) {

            console.error(
                "Failed to unsubscribe WebSocket:",
                error
            );
        }

        projectSubscription = null;
    }

    if (stompClient) {

        try {
            stompClient.deactivate();

        } catch (error) {

            console.error(
                "Failed to deactivate WebSocket:",
                error
            );
        }

        stompClient = null;
    }

    console.log(
        "WebSocket manually disconnected"
    );
}