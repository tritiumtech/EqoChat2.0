package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.chat.api.dto.request.MarkConversationReadRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.ConversationService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageHandler {

    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ConversationParticipantService participantService;
    private final LiabilityPolicyApi liabilityPolicyApi;
    private final WebSocketSessionManager sessionManager;
    private final WebSocketSender webSocketSender;

    public void handleMessage(String principalHumanId, WebSocketMessage.BaseMessage message, WebSocketSession session)
            throws IOException {
        switch (message.getType()) {
            case CHAT_MESSAGE:
                handleChatMessage(principalHumanId, message, session);
                break;
            case CHAT_TYPING:
                handleTypingMessage(principalHumanId, message);
                break;
            case CHAT_READ:
                handleReadReceipt(principalHumanId, message);
                break;
            case SUBJECT_SUBSCRIBE:
                handleSubjectSubscribe(principalHumanId, message, session);
                break;
            case PING:
                handlePing(session);
                break;
            default:
                log.warn("unknown WebSocket message type: {}", message.getType());
                webSocketSender.sendError(session, 400, "unknown message type", message.getId());
        }
    }

    private void handleChatMessage(String principalHumanId, WebSocketMessage.BaseMessage message, WebSocketSession session) {
        try {
            WebSocketMessage.ChatMessagePayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ChatMessagePayload.class);
            if (payload.getConversationId() == null) {
                webSocketSender.sendError(session, 400, "conversation id is required", message.getId());
                return;
            }

            SubjectRef actor = resolveMessageSubject(message);
            SendMessageRequest request = new SendMessageRequest();
            request.setActorSubjectId(actor.id());
            request.setActorSubjectType(actor.type());
            request.setMessageType(payload.getMessageType() != null ? payload.getMessageType() : "TEXT");
            request.setContent(payload.getContent());
            request.setMetadata(payload.getMetadata());
            request.setReplyToMessageId(payload.getReplyToMessageId());
            request.setIntentData(payload.getIntentData());

            MessageResponse saved = conversationService.sendMessage(
                    Long.parseLong(principalHumanId),
                    Long.parseLong(payload.getConversationId()),
                    request
            );
            log.info("WebSocket chat message saved: messageId={}, actor={}", saved.getId(), actor);
            joinConversationSubjects(payload.getConversationId());
        } catch (IllegalArgumentException e) {
            log.warn("invalid WebSocket chat message: {}", e.getMessage());
            webSocketSender.sendError(session, 400, e.getMessage(), message.getId());
        } catch (Exception e) {
            log.error("handle WebSocket chat message failed", e);
            webSocketSender.sendError(session, 500, "message handling failed: " + e.getMessage(), message.getId());
        }
    }

    private void handleTypingMessage(String principalHumanId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.TypingPayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.TypingPayload.class);
            SubjectRef actor = resolveMessageSubject(message);
            requireAuthorizedConversationActor(principalHumanId, payload.getConversationId(), actor);
            payload.setSubjectId(String.valueOf(actor.id()));
            payload.setSubjectType(actor.type().name());
            message.setSenderSubjectId(String.valueOf(actor.id()));
            message.setSenderSubjectType(actor.type().name());
            message.setPayload(payload);

            joinConversationSubjects(payload.getConversationId());
            for (String targetSubjectKey : sessionManager.getConversationSubjectKeys(payload.getConversationId())) {
                String targetSubjectId = sessionManager.subjectIdFromKey(targetSubjectKey);
                String targetSubjectType = sessionManager.subjectTypeFromKey(targetSubjectKey);
                if (targetSubjectId.equals(String.valueOf(actor.id())) && targetSubjectType.equals(actor.type().name())) {
                    continue;
                }
                message.setRecipientSubjectId(targetSubjectId);
                message.setRecipientSubjectType(targetSubjectType);
                webSocketSender.sendToSubject(targetSubjectId, targetSubjectType, message);
            }
        } catch (Exception e) {
            log.error("handle typing message failed", e);
        }
    }

    private void handleReadReceipt(String principalHumanId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.ReadReceiptPayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ReadReceiptPayload.class);
            if (payload.getMessageId() == null || payload.getConversationId() == null) {
                return;
            }

            SubjectRef reader = resolveMessageSubject(message);
            MarkConversationReadRequest request = new MarkConversationReadRequest();
            request.setMessageId(Long.parseLong(payload.getMessageId()));
            request.setReaderSubjectId(reader.id());
            request.setReaderSubjectType(reader.type());
            conversationService.markRead(
                    Long.parseLong(principalHumanId),
                    Long.parseLong(payload.getConversationId()),
                    request
            );

            Message stored = messageService.getById(request.getMessageId());
            if (stored == null || stored.getSenderId() == null || stored.getSenderType() == null) {
                return;
            }
            if (stored.getSenderId().equals(reader.id()) && stored.getSenderType() == reader.type()) {
                return;
            }

            WebSocketMessage.ReadReceiptPayload outPayload = WebSocketMessage.ReadReceiptPayload.builder()
                    .conversationId(String.valueOf(stored.getConversationId()))
                    .messageId(String.valueOf(stored.getId()))
                    .readerSubjectId(String.valueOf(reader.id()))
                    .readerSubjectType(reader.type().name())
                    .build();
            WebSocketMessage.BaseMessage readMessage = WebSocketMessage.BaseMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .type(WebSocketMessage.MessageType.CHAT_READ)
                    .senderSubjectId(String.valueOf(reader.id()))
                    .senderSubjectType(reader.type().name())
                    .recipientSubjectId(String.valueOf(stored.getSenderId()))
                    .recipientSubjectType(stored.getSenderType().name())
                    .timestamp(LocalDateTime.now())
                    .payload(outPayload)
                    .build();
            webSocketSender.sendToSubject(
                    String.valueOf(stored.getSenderId()),
                    stored.getSenderType().name(),
                    readMessage
            );
        } catch (Exception e) {
            log.error("handle read receipt failed", e);
        }
    }

    private void handleSubjectSubscribe(
            String principalHumanId,
            WebSocketMessage.BaseMessage message,
            WebSocketSession session
    ) {
        try {
            WebSocketMessage.SubjectSubscribePayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.SubjectSubscribePayload.class);
            SubjectRef subject = resolveSubscribeSubject(payload);
            requireAuthorizedSubscription(principalHumanId, subject);
            sessionManager.registerActiveSubjectSession(
                    principalHumanId,
                    String.valueOf(subject.id()),
                    subject.type().name(),
                    session
            );

            WebSocketMessage.BaseMessage ack = WebSocketMessage.BaseMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .type(WebSocketMessage.MessageType.SUBJECT_SUBSCRIBED)
                    .senderSubjectId("0")
                    .senderSubjectType(SubjectType.SYSTEM.name())
                    .recipientSubjectId(String.valueOf(subject.id()))
                    .recipientSubjectType(subject.type().name())
                    .timestamp(LocalDateTime.now())
                    .payload(WebSocketMessage.SubjectSubscribePayload.builder()
                            .subjectId(String.valueOf(subject.id()))
                            .subjectType(subject.type().name())
                            .build())
                    .build();
            session.sendMessage(new org.springframework.web.socket.TextMessage(objectMapper.writeValueAsString(ack)));
        } catch (IllegalArgumentException e) {
            webSocketSender.sendError(session, 400, e.getMessage(), message.getId());
        } catch (Exception e) {
            log.error("handle subject subscription failed", e);
            webSocketSender.sendError(session, 500, "subject subscription failed: " + e.getMessage(), message.getId());
        }
    }

    private void handlePing(WebSocketSession session) throws IOException {
        WebSocketMessage.BaseMessage pong = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.PONG)
                .senderSubjectId("0")
                .senderSubjectType(SubjectType.SYSTEM.name())
                .timestamp(LocalDateTime.now())
                .build();

        session.sendMessage(new org.springframework.web.socket.TextMessage(
                objectMapper.writeValueAsString(pong)));
    }

    private SubjectRef resolveMessageSubject(WebSocketMessage.BaseMessage message) {
        String rawId = message.getSenderSubjectId();
        String rawType = message.getSenderSubjectType();
        if (rawId == null || rawType == null) {
            throw new IllegalArgumentException("sender subject is incomplete");
        }
        return new SubjectRef(Long.parseLong(rawId), SubjectType.from(rawType));
    }

    private SubjectRef resolveSubscribeSubject(WebSocketMessage.SubjectSubscribePayload payload) {
        if (payload == null || payload.getSubjectId() == null || payload.getSubjectType() == null) {
            throw new IllegalArgumentException("subject subscription is incomplete");
        }
        return new SubjectRef(Long.parseLong(payload.getSubjectId()), SubjectType.from(payload.getSubjectType()));
    }

    private void requireAuthorizedSubscription(String principalHumanId, SubjectRef subject) {
        if (subject.type() == SubjectType.SYSTEM) {
            throw new IllegalArgumentException("system subject cannot be subscribed");
        }
        if (subject.type() == SubjectType.HUMAN) {
            if (!Objects.equals(subject.id(), Long.parseLong(principalHumanId))) {
                throw new IllegalArgumentException("human subject is not current principal");
            }
            return;
        }
        LiabilityChain chain = liabilityPolicyApi.resolveLiability(subject);
        Long liableHumanId = chain != null ? chain.liableHumanId() : null;
        if (liableHumanId == null || !Objects.equals(liableHumanId, Long.parseLong(principalHumanId))) {
            throw new IllegalArgumentException("subject is not authorized for current principal");
        }
    }

    private void requireAuthorizedConversationActor(String principalHumanId, String conversationId, SubjectRef actor) {
        if (conversationId == null) {
            throw new IllegalArgumentException("conversation id is required");
        }
        requireAuthorizedSubscription(principalHumanId, actor);
        participantService
                .findByConversationAndParticipant(Long.parseLong(conversationId), actor)
                .orElseThrow(() -> new IllegalArgumentException("sender subject is not conversation participant"));
    }

    private void joinConversationSubjects(String conversationId) {
        try {
            Long convId = Long.parseLong(conversationId);
            for (ConversationParticipant participant : participantService.listByConversationId(convId)) {
                if (participant.getParticipantId() == null || participant.getParticipantType() == null) {
                    continue;
                }
                sessionManager.joinConversationAsSubject(
                        conversationId,
                        participant.getParticipantId().toString(),
                        participant.getParticipantType().name()
                );
                if (participant.getParticipantType() == SubjectType.HUMAN) {
                    sessionManager.joinConversationAsPrincipalHuman(
                            conversationId,
                            participant.getParticipantId().toString()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("join conversation subject broadcast list failed: conversationId={}", conversationId, e);
        }
    }
}
