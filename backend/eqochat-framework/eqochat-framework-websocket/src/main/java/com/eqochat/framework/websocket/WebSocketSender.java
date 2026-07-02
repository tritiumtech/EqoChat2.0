package com.eqochat.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSender {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;

    public void sendToPrincipalHuman(String principalHumanId, WebSocketMessage.BaseMessage message) {
        WebSocketSession session = sessionManager.getPrincipalHumanSession(principalHumanId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, message);
            } catch (IOException e) {
                log.error("send WebSocket message failed: principalHumanId={}", principalHumanId, e);
            }
        } else {
            log.debug("principal human is offline: principalHumanId={}", principalHumanId);
        }
    }

    public void sendToSubject(String subjectId, String subjectType, WebSocketMessage.BaseMessage message) {
        for (WebSocketSession session : sessionManager.getSubjectSessions(subjectId, subjectType)) {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (IOException e) {
                    log.error("send WebSocket message failed: subjectType={}, subjectId={}",
                            subjectType, subjectId, e);
                }
            }
        }
    }

    public void broadcastToConversationHumans(String conversationId, WebSocketMessage.BaseMessage message) {
        Set<String> principalHumanIds = sessionManager.getConversationPrincipalHumans(conversationId);
        for (String principalHumanId : principalHumanIds) {
            sendToPrincipalHuman(principalHumanId, message);
        }
    }

    public void broadcastToConversationSubjects(String conversationId, WebSocketMessage.BaseMessage message) {
        Set<String> subjectKeys = sessionManager.getConversationSubjectKeys(conversationId);
        Map<String, TargetMessage> targets = new LinkedHashMap<>();
        for (String subjectKey : subjectKeys) {
            String subjectId = sessionManager.subjectIdFromKey(subjectKey);
            String subjectType = sessionManager.subjectTypeFromKey(subjectKey);
            WebSocketMessage.BaseMessage out = WebSocketMessage.BaseMessage.builder()
                    .id(message.getId())
                    .type(message.getType())
                    .senderSubjectId(message.getSenderSubjectId())
                    .senderSubjectType(message.getSenderSubjectType())
                    .recipientSubjectId(subjectId)
                    .recipientSubjectType(subjectType)
                    .timestamp(message.getTimestamp())
                    .payload(message.getPayload())
                    .build();
            for (WebSocketSession session : sessionManager.getSubjectSessions(subjectId, subjectType)) {
                targets.putIfAbsent(session.getId(), new TargetMessage(session, out));
            }
        }
        for (TargetMessage target : targets.values()) {
            if (!target.session().isOpen()) {
                continue;
            }
            try {
                sendMessage(target.session(), target.message());
            } catch (IOException e) {
                log.error("broadcast WebSocket message failed: conversationId={}", conversationId, e);
            }
        }
    }

    public void sendError(WebSocketSession session, Integer code, String message, String originalMessageId) {
        WebSocketMessage.ErrorPayload error = WebSocketMessage.ErrorPayload.builder()
                .code(code)
                .message(message)
                .originalMessageId(originalMessageId)
                .build();

        WebSocketMessage.BaseMessage errorMessage = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.ERROR)
                .senderSubjectId("0")
                .senderSubjectType("SYSTEM")
                .timestamp(LocalDateTime.now())
                .payload(error)
                .build();

        try {
            sendMessage(session, errorMessage);
        } catch (IOException e) {
            log.error("send WebSocket error message failed", e);
        }
    }

    protected void sendMessage(WebSocketSession session, WebSocketMessage.BaseMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(payload));
    }

    private record TargetMessage(WebSocketSession session, WebSocketMessage.BaseMessage message) {
    }
}
