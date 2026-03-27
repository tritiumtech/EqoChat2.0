package com.eqochat.websocket;

import com.eqochat.domain.entity.Message;
import com.eqochat.domain.entity.ConversationParticipant;
import com.eqochat.domain.entity.MessageReadReceipt;
import com.eqochat.mapper.MessageReadReceiptMapper;
import com.eqochat.service.ConversationService;
import com.eqochat.service.ConversationParticipantService;
import com.eqochat.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * WebSocket消息处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageHandler {
    
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ConversationParticipantService participantService;
    private final MessageReadReceiptMapper messageReadReceiptMapper;
    private final WebSocketSessionManager sessionManager;
    private final WebSocketSender webSocketSender;
    
    /**
     * 处理消息
     */
    public void handleMessage(String userId, WebSocketMessage.BaseMessage message, WebSocketSession session) 
            throws IOException {
        
        switch (message.getType()) {
            case CHAT_MESSAGE:
                handleChatMessage(userId, message, session);
                break;
            case CHAT_TYPING:
                handleTypingMessage(userId, message);
                break;
            case CHAT_READ:
                handleReadReceipt(userId, message);
                break;
            case PING:
                handlePing(session);
                break;
            default:
                log.warn("未知消息类型: {}", message.getType());
                webSocketSender.sendError(session, 400, "未知消息类型", message.getId());
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String userId, WebSocketMessage.BaseMessage message, WebSocketSession session) {
        try {
            WebSocketMessage.ChatMessagePayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ChatMessagePayload.class);
            
            if (payload.getConversationId() == null) {
                webSocketSender.sendError(session, 400, "缺少会话ID", message.getId());
                return;
            }

            Long conversationId = Long.parseLong(payload.getConversationId());
            Long senderId = Long.parseLong(userId);
            log.info("处理聊天消息: userId={}, conversationId={}", userId, payload.getConversationId());

            if (participantService.findByConversationAndParticipant(conversationId, senderId).isEmpty()) {
                webSocketSender.sendError(session, 403, "无权限发送消息", message.getId());
                return;
            }

            String messageType = payload.getMessageType() != null ? payload.getMessageType() : "TEXT";
            payload.setMessageType(messageType);
            boolean isText = messageType == null || "TEXT".equalsIgnoreCase(messageType);
            if (isText) {
                if (payload.getContent() == null || payload.getContent().isBlank()) {
                    webSocketSender.sendError(session, 400, "message.content.required", message.getId());
                    return;
                }
            } else {
                if (payload.getMetadata() == null) {
                    webSocketSender.sendError(session, 400, "message.metadata.required", message.getId());
                    return;
                }
            }

            String contentMetadataJson = null;
            if (payload.getMetadata() != null) {
                contentMetadataJson = objectMapper.writeValueAsString(payload.getMetadata());
            }

            // 保存消息到数据库
            Message msg = Message.builder()
                    .conversationId(conversationId)
                    .senderId(senderId)
                    .senderType("USER")
                    .messageType(messageType)
                    .content(payload.getContent())
                    .contentMetadata(contentMetadataJson)
                    .intentData(payload.getIntentData())
                    .replyToMessageId(payload.getReplyToMessageId() != null ? 
                            Long.parseLong(payload.getReplyToMessageId()) : null)
                    .status("SENT")
                    .build();
            
            messageService.save(msg);
            conversationService.updateLastMessage(
                    conversationId,
                    msg.getId(),
                    msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now()
            );

            participantService.updateLastRead(
                    conversationId,
                    senderId,
                    msg.getId(),
                    msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now()
            );

            // 确保会话用户已加入广播列表
            joinConversationUsers(payload.getConversationId());
            
            // 广播给会话中的所有用户
            WebSocketMessage.BaseMessage outMessage = WebSocketMessage.BaseMessage.builder()
                    .id(String.valueOf(msg.getId()))
                    .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                    .senderId(userId)
                    .senderType("USER")
                    .recipientId(payload.getConversationId())
                    .timestamp(msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now())
                    .payload(payload)
                    .build();
            broadcastToConversation(payload.getConversationId(), outMessage);
            
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
        }
    }
    
    /**
     * 处理正在输入状态
     */
    private void handleTypingMessage(String userId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.TypingPayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.TypingPayload.class);
            
            // 广播给会话中的其他用户
            Set<String> users = sessionManager.getConversationUsers(payload.getConversationId());
            for (String uid : users) {
                if (!uid.equals(userId)) {
                    webSocketSender.sendToUser(uid, message);
                }
            }
            
        } catch (Exception e) {
            log.error("处理输入状态失败", e);
        }
    }
    
    /**
     * 处理已读回执
     */
    private void handleReadReceipt(String userId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.ReadReceiptPayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ReadReceiptPayload.class);
            
            if (payload.getMessageId() == null) {
                return;
            }

            Long messageId = Long.parseLong(payload.getMessageId());
            Message stored = messageService.getById(messageId);
            if (stored == null) {
                return;
            }
            Long readerId = Long.parseLong(userId);

            log.info("处理已读回执: userId={}, messageId={}", userId, payload.getMessageId());
            
            // 更新消息已读状态
            if (!readerId.equals(stored.getSenderId())) {
                messageService.markAsRead(messageId, readerId);
            }

            participantService.updateLastRead(
                    stored.getConversationId(),
                    readerId,
                    messageId,
                    LocalDateTime.now()
            );

            MessageReadReceipt receipt = MessageReadReceipt.builder()
                    .messageId(messageId)
                    .readerId(readerId)
                    .readerType(MessageReadReceipt.ReaderType.USER)
                    .readAt(LocalDateTime.now())
                    .build();

            try {
                messageReadReceiptMapper.insert(receipt);
            } catch (DuplicateKeyException ignore) {
                // 已存在已读记录，忽略
            }
            
            // 通知消息发送者
            if (stored.getSenderId() != null && !stored.getSenderId().equals(readerId)) {
                WebSocketMessage.BaseMessage readMessage = WebSocketMessage.BaseMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .type(WebSocketMessage.MessageType.CHAT_READ)
                        .senderId(userId)
                        .senderType("USER")
                        .recipientId(String.valueOf(stored.getSenderId()))
                        .timestamp(LocalDateTime.now())
                        .payload(WebSocketMessage.ReadReceiptPayload.builder()
                                .conversationId(String.valueOf(stored.getConversationId()))
                                .messageId(String.valueOf(messageId))
                                .readerId(String.valueOf(readerId))
                                .build())
                        .build();
                webSocketSender.sendToUser(String.valueOf(stored.getSenderId()), readMessage);
            }
            
        } catch (Exception e) {
            log.error("处理已读回执失败", e);
        }
    }
    
    /**
     * 处理心跳
     */
    private void handlePing(WebSocketSession session) throws IOException {
        WebSocketMessage.BaseMessage pong = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.PONG)
                .senderId("system")
                .senderType("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();
        
        session.sendMessage(new org.springframework.web.socket.TextMessage(
                objectMapper.writeValueAsString(pong)));
    }
    
    /**
     * 广播消息给会话中的所有用户
     */
    private void broadcastToConversation(String conversationId, WebSocketMessage.BaseMessage message) {
        Set<String> users = sessionManager.getConversationUsers(conversationId);
        for (String userId : users) {
            webSocketSender.sendToUser(userId, message);
        }
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
