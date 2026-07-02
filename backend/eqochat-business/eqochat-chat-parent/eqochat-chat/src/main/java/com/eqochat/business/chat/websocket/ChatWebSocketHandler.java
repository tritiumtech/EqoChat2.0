package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.framework.common.UserContext;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageHandler messageHandler;
    private final ConversationParticipantService participantService;
    private final SubjectDirectoryApi subjectDirectoryApi;

    // principalHumanId -> session
    private final ConcurrentHashMap<String, WebSocketSession> principalHumanSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String principalHumanId = extractPrincipalHumanId(session);
        if (principalHumanId == null) {
            log.warn("WebSocket connection missing principalHumanId, closing session");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        WebSocketSession replaced = principalHumanSessions.put(principalHumanId, session);
        if (replaced != null && !replaced.getId().equals(session.getId())) {
            sessionManager.unregisterPrincipalHumanSession(principalHumanId, replaced);
        }
        sessionManager.registerPrincipalHumanSession(principalHumanId, session);

        try {
            UserContext.setCurrentUser(Long.parseLong(principalHumanId));

            log.info("WebSocket connection established: principalHumanId={}, sessionId={}",
                    principalHumanId, session.getId());

            registerAssociatedSubjects(principalHumanId, session);
            sendConnectAck(session, principalHumanId);
            broadcastSessionPresence(principalHumanId, session, "ONLINE");
        } finally {
            UserContext.clear();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String principalHumanId = extractPrincipalHumanId(session);

        log.debug("received WebSocket message: principalHumanId={}, payload={}", principalHumanId, payload);

        try {
            WebSocketMessage.BaseMessage baseMessage = objectMapper.readValue(
                    payload, WebSocketMessage.BaseMessage.class);

            UserContext.setCurrentUser(Long.parseLong(principalHumanId));
            messageHandler.handleMessage(principalHumanId, baseMessage, session);
        } catch (Exception e) {
            log.error("handle WebSocket message failed: {}", payload, e);
            sendError(session, 500, "message handling failed: " + e.getMessage(), null);
        } finally {
            UserContext.clear();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String principalHumanId = extractPrincipalHumanId(session);
        if (principalHumanId != null) {
            Set<String> subjectKeys = Set.copyOf(sessionManager.getSubjectKeysBySessionId(session.getId()));
            boolean removedCurrent = principalHumanSessions.remove(principalHumanId, session);
            boolean managerRemovedCurrent = sessionManager.unregisterPrincipalHumanSession(principalHumanId, session);

            log.info("WebSocket connection closed: principalHumanId={}, status={}", principalHumanId, status);
            if (removedCurrent && managerRemovedCurrent) {
                broadcastSessionPresence(principalHumanId, subjectKeys, "OFFLINE");
            }
        }

        UserContext.clear();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String principalHumanId = extractPrincipalHumanId(session);
        log.error("WebSocket transport error: principalHumanId={}", principalHumanId, exception);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private String extractPrincipalHumanId(WebSocketSession session) {
        String principalHumanId = (String) session.getAttributes().get("principalHumanId");
        if (principalHumanId != null && !principalHumanId.isEmpty()) {
            return principalHumanId;
        }

        log.warn("cannot extract principalHumanId from WebSocketSession, sessionId={}", session.getId());
        return null;
    }

    private void registerAssociatedSubjects(String principalHumanId, WebSocketSession session) {
        try {
            Long humanId = Long.parseLong(principalHumanId);
            for (SubjectRef subject : associatedSubjects(humanId)) {
                if (subject == null || subject.id() == null || subject.type() == null) {
                    continue;
                }

                String subjectId = subject.id().toString();
                String subjectType = subject.type().name();
                sessionManager.registerSubjectSession(principalHumanId, subjectId, subjectType, session);

                for (ConversationParticipant participant : participantService.listByParticipant(subject)) {
                    if (participant.getConversationId() == null) {
                        continue;
                    }
                    String conversationId = participant.getConversationId().toString();
                    sessionManager.joinConversationAsSubject(conversationId, subjectId, subjectType);
                    if (subject.type() == SubjectType.HUMAN) {
                        sessionManager.joinConversationAsPrincipalHuman(conversationId, principalHumanId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("load WebSocket subject subscriptions failed: principalHumanId={}", principalHumanId, e);
        }
    }

    private List<SubjectRef> associatedSubjects(Long principalHumanId) {
        List<SubjectRef> subjects = subjectDirectoryApi.listAssociatedSubjects(principalHumanId);
        return subjects == null ? List.of() : subjects;
    }

    private void sendConnectAck(WebSocketSession session, String principalHumanId) throws IOException {
        WebSocketMessage.ConnectAckPayload ack = WebSocketMessage.ConnectAckPayload.builder()
                .principalHumanId(principalHumanId)
                .connectionId(UUID.randomUUID().toString())
                .serverTime(System.currentTimeMillis())
                .build();

        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.CONNECT_ACK)
                .senderSubjectId("0")
                .senderSubjectType(SubjectType.SYSTEM.name())
                .timestamp(LocalDateTime.now())
                .payload(ack)
                .build();

        sendMessage(session, message);
    }

    public void sendError(WebSocketSession session, Integer code, String message, String originalMessageId)
            throws IOException {
        WebSocketMessage.ErrorPayload error = WebSocketMessage.ErrorPayload.builder()
                .code(code)
                .message(message)
                .originalMessageId(originalMessageId)
                .build();

        WebSocketMessage.BaseMessage errorMessage = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.ERROR)
                .senderSubjectId("0")
                .senderSubjectType(SubjectType.SYSTEM.name())
                .timestamp(LocalDateTime.now())
                .payload(error)
                .build();

        sendMessage(session, errorMessage);
    }

    private void broadcastSessionPresence(String principalHumanId, WebSocketSession session, String status) {
        broadcastSessionPresence(
                principalHumanId,
                Set.copyOf(sessionManager.getSubjectKeysBySessionId(session.getId())),
                status
        );
    }

    private void broadcastSessionPresence(String principalHumanId, Set<String> subjectKeys, String status) {
        if (subjectKeys == null || subjectKeys.isEmpty()) {
            return;
        }

        for (String subjectKey : subjectKeys) {
            String subjectId = sessionManager.subjectIdFromKey(subjectKey);
            String subjectType = sessionManager.subjectTypeFromKey(subjectKey);
            if ("OFFLINE".equals(status) && sessionManager.isSubjectOnline(subjectId, subjectType)) {
                continue;
            }
            broadcastPresence(principalHumanId, subjectId, subjectType, status);
        }
    }

    private void broadcastPresence(String principalHumanId, String subjectId, String subjectType, String status) {
        WebSocketMessage.PresencePayload presence = WebSocketMessage.PresencePayload.builder()
                .subjectId(subjectId)
                .subjectType(subjectType)
                .status(status)
                .lastSeenAt(System.currentTimeMillis())
                .build();

        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(status.equals("ONLINE")
                        ? WebSocketMessage.MessageType.PRESENCE_ONLINE
                        : WebSocketMessage.MessageType.PRESENCE_OFFLINE)
                .senderSubjectId(subjectId)
                .senderSubjectType(subjectType)
                .timestamp(LocalDateTime.now())
                .payload(presence)
                .build();

        for (WebSocketSession session : presenceRecipients(principalHumanId, subjectId, subjectType)) {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (IOException e) {
                    log.error("send presence failed: subjectType={}, subjectId={}, sessionId={}",
                            subjectType, subjectId, session.getId(), e);
                }
            }
        }
    }

    private Set<WebSocketSession> presenceRecipients(String principalHumanId, String subjectId, String subjectType) {
        Set<WebSocketSession> recipients = new LinkedHashSet<>();
        SubjectRef subject = presenceSubject(subjectId, subjectType);
        if (subject == null) {
            return recipients;
        }

        String sourceKey = sessionManager.subjectKey(subjectId, subjectType);
        try {
            for (ConversationParticipant participant : participantService.listByParticipant(subject)) {
                if (participant.getConversationId() == null) {
                    continue;
                }
                String conversationId = participant.getConversationId().toString();
                for (String targetSubjectKey : sessionManager.getConversationSubjectKeys(conversationId)) {
                    if (sourceKey.equals(targetSubjectKey)) {
                        continue;
                    }
                    for (WebSocketSession session : sessionManager.getSubjectSessionsByKey(targetSubjectKey)) {
                        String targetPrincipalHumanId = sessionManager.getPrincipalHumanIdBySessionId(session.getId());
                        if (!principalHumanId.equals(targetPrincipalHumanId)) {
                            recipients.add(session);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("load presence recipients failed: subjectType={}, subjectId={}", subjectType, subjectId, e);
        }
        return recipients;
    }

    private SubjectRef presenceSubject(String subjectId, String subjectType) {
        try {
            return new SubjectRef(Long.parseLong(subjectId), SubjectType.valueOf(subjectType));
        } catch (Exception e) {
            log.warn("invalid presence subject: subjectType={}, subjectId={}", subjectType, subjectId, e);
            return null;
        }
    }

    public void sendMessageToPrincipalHuman(String principalHumanId, WebSocketMessage.BaseMessage message) {
        WebSocketSession session = principalHumanSessions.get(principalHumanId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, message);
            } catch (IOException e) {
                log.error("send message failed: principalHumanId={}", principalHumanId, e);
            }
        } else {
            log.debug("principal human is offline: principalHumanId={}", principalHumanId);
        }
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage.BaseMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(payload));
    }

    public boolean isPrincipalHumanOnline(String principalHumanId) {
        WebSocketSession session = principalHumanSessions.get(principalHumanId);
        return session != null && session.isOpen();
    }
}
