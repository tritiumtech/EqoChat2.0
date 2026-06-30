package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
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
import java.util.Set;
import java.util.UUID;

/**
 * WebSocket chat message handler. Persistence is delegated to ConversationService
 * so HTTP and realtime writes share actor/liability rules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageHandler {

    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ConversationParticipantService participantService;
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
            case PING:
                handlePing(session);
                break;
            default:
                log.warn("未知消息类型: {}", message.getType());
                webSocketSender.sendError(session, 400, "未知消息类型", message.getId());
        }
    }

    private void handleChatMessage(String principalHumanId, WebSocketMessage.BaseMessage message, WebSocketSession session) {
        try {
            WebSocketMessage.ChatMessagePayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ChatMessagePayload.class);
            if (payload.getConversationId() == null) {
                webSocketSender.sendError(session, 400, "缺少会话ID", message.getId());
                return;
            }

            SubjectRef actor = resolveMessageSubject(principalHumanId, message);
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
            log.info("WebSocket聊天消息已保存: messageId={}, actor={}", saved.getId(), actor);
            joinConversationHumans(payload.getConversationId());
        } catch (IllegalArgumentException e) {
            log.warn("WebSocket聊天消息参数非法: {}", e.getMessage());
            webSocketSender.sendError(session, 400, e.getMessage(), message.getId());
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            webSocketSender.sendError(session, 500, "消息处理失败: " + e.getMessage(), message.getId());
        }
    }

    private void handleTypingMessage(String principalHumanId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.TypingPayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.TypingPayload.class);
            SubjectRef actor = resolveMessageSubject(principalHumanId, message);
            payload.setSubjectId(String.valueOf(actor.id()));
            payload.setSubjectType(actor.type().name());
            message.setSenderSubjectId(String.valueOf(actor.id()));
            message.setSenderSubjectType(actor.type().name());
            message.setPayload(payload);

            Set<String> principalHumanIds = sessionManager.getConversationPrincipalHumans(payload.getConversationId());
            for (String targetPrincipalHumanId : principalHumanIds) {
                if (!targetPrincipalHumanId.equals(principalHumanId)) {
                    webSocketSender.sendToPrincipalHuman(targetPrincipalHumanId, message);
                }
            }
        } catch (Exception e) {
            log.error("处理输入状态失败", e);
        }
    }

    private void handleReadReceipt(String principalHumanId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.ReadReceiptPayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ReadReceiptPayload.class);
            if (payload.getMessageId() == null || payload.getConversationId() == null) {
                return;
            }

            SubjectRef reader = resolveMessageSubject(principalHumanId, message);
            MarkConversationReadRequest request = new MarkConversationReadRequest();
            request.setMessageId(Long.parseLong(payload.getMessageId()));
            request.setReaderSubjectId(reader.id());
            request.setReaderSubjectType(reader.type());
            conversationService.markRead(Long.parseLong(principalHumanId), Long.parseLong(payload.getConversationId()), request);

            Message stored = messageService.getById(request.getMessageId());
            if (stored == null) {
                return;
            }
            if (stored.getSenderType() == SubjectType.HUMAN
                    && (!stored.getSenderId().equals(reader.id()) || stored.getSenderType() != reader.type())) {
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
                        .timestamp(LocalDateTime.now())
                        .payload(outPayload)
                        .build();
                webSocketSender.sendToPrincipalHuman(String.valueOf(stored.getSenderId()), readMessage);
            }
        } catch (Exception e) {
            log.error("处理已读回执失败", e);
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

    private SubjectRef resolveMessageSubject(String principalHumanId, WebSocketMessage.BaseMessage message) {
        String rawId = message.getSenderSubjectId();
        String rawType = message.getSenderSubjectType();
        if (rawId == null || rawType == null) {
            throw new IllegalArgumentException("sender subject is incomplete");
        }
        return new SubjectRef(Long.parseLong(rawId), SubjectType.from(rawType));
    }

    private void joinConversationHumans(String conversationId) {
        try {
            Long convId = Long.parseLong(conversationId);
            for (ConversationParticipant participant : participantService.listByConversationId(convId)) {
                if (participant.getParticipantId() != null && participant.getParticipantType() == SubjectType.HUMAN) {
                    sessionManager.joinConversationAsPrincipalHuman(conversationId, participant.getParticipantId().toString());
                }
            }
        } catch (Exception e) {
            log.warn("加入会话广播列表失败: conversationId={}", conversationId, e);
        }
    }
}
