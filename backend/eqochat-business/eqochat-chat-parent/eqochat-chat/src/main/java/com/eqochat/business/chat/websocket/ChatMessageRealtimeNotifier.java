package com.eqochat.business.chat.websocket;

import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * HTTP 等非 WebSocket 通道保存消息后，向会话内在线用户补发与 WS 通道一致的实时推送。
 */
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
        joinConversationUsers(conversationId);

        Object metadata = null;
        if (msg.getContentMetadata() != null && !msg.getContentMetadata().isBlank()) {
            try {
                metadata = objectMapper.readValue(msg.getContentMetadata(), Object.class);
            } catch (Exception e) {
                log.debug("解析 contentMetadata 失败，按无 metadata 推送: {}", e.getMessage());
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
                .senderId(String.valueOf(msg.getSenderId()))
                .senderType(msg.getSenderType() != null ? msg.getSenderType() : "USER")
                .recipientId(conversationId)
                .timestamp(ts)
                .payload(payload)
                .build();

        webSocketSender.broadcastToConversation(conversationId, out);
    }

    private void joinConversationUsers(String conversationId) {
        try {
            Long convId = Long.parseLong(conversationId);
            for (ConversationParticipant participant : participantService.listByConversationId(convId)) {
                if (participant.getParticipantId() != null) {
                    sessionManager.joinConversation(conversationId, participant.getParticipantId().toString());
                }
            }
        } catch (Exception e) {
            log.warn("加入会话广播列表失败: conversationId={}", conversationId, e);
        }
    }
}
