package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageRealtimeNotifier {

    private final WebSocketSender webSocketSender;
    private final WebSocketSessionManager sessionManager;
    private final ConversationParticipantService participantService;
    private final ObjectMapper objectMapper;

    public void notifyChatMessageSaved(Message msg) {
        if (msg == null || msg.getConversationId() == null || msg.getId() == null) {
            return;
        }
        String conversationId = String.valueOf(msg.getConversationId());
        joinConversationSubjects(conversationId);

        Object metadata = null;
        if (msg.getContentMetadata() != null && !msg.getContentMetadata().isBlank()) {
            try {
                metadata = objectMapper.readValue(msg.getContentMetadata(), Object.class);
            } catch (Exception e) {
                log.debug("parse contentMetadata failed, sending null metadata: {}", e.getMessage());
            }
        }

        WebSocketMessage.ChatMessagePayload payload = WebSocketMessage.ChatMessagePayload.builder()
                .conversationId(conversationId)
                .messageType(msg.getMessageType())
                .content(msg.getContent())
                .metadata(metadata)
                .replyToMessageId(msg.getReplyToMessageId() != null ? String.valueOf(msg.getReplyToMessageId()) : null)
                .intentData(msg.getIntentData())
                .build();

        LocalDateTime ts = msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now();
        WebSocketMessage.BaseMessage out = WebSocketMessage.BaseMessage.builder()
                .id(String.valueOf(msg.getId()))
                .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                .senderSubjectId(String.valueOf(msg.getSenderId()))
                .senderSubjectType(msg.getSenderType() != null ? msg.getSenderType().name() : SubjectType.SYSTEM.name())
                .timestamp(ts)
                .payload(payload)
                .build();

        webSocketSender.broadcastToConversationSubjects(conversationId, out);
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
