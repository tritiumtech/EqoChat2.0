package com.eqochat.framework.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class WebSocketMessage {

    public enum MessageType {
        CHAT_MESSAGE,
        CHAT_TYPING,
        CHAT_READ,
        NOTIFICATION,
        SESSION_KICKED,
        AGENT_INTENT,
        AGENT_RESPONSE,
        PRESENCE_ONLINE,
        PRESENCE_OFFLINE,
        PRESENCE_TYPING,
        CONNECT_ACK,
        SUBJECT_SUBSCRIBE,
        SUBJECT_SUBSCRIBED,
        PING,
        PONG,
        ERROR
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseMessage {
        private String id;
        private MessageType type;
        private String senderSubjectId;
        private String senderSubjectType;
        private String recipientSubjectId;
        private String recipientSubjectType;
        private LocalDateTime timestamp;
        private Object payload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessagePayload {
        private String conversationId;
        private String messageType;
        private String content;
        private Object metadata;
        private String replyToMessageId;
        private String intentData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadReceiptPayload {
        private String conversationId;
        private String messageId;
        private String readerSubjectId;
        private String readerSubjectType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypingPayload {
        private String conversationId;
        private String subjectId;
        private String subjectType;
        private boolean isTyping;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresencePayload {
        private String subjectId;
        private String subjectType;
        private String status;
        private Long lastSeenAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectAckPayload {
        private String principalHumanId;
        private String connectionId;
        private Long serverTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectSubscribePayload {
        private String subjectId;
        private String subjectType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPayload {
        private Integer code;
        private String message;
        private String originalMessageId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionKickedPayload {
        private String reason;
        private Long kickedAt;
        private String newDeviceId;
    }
}
