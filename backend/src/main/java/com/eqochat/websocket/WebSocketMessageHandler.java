package com.eqochat.websocket;

import com.eqochat.domain.entity.Message;
import com.eqochat.service.MessageService;
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
 * WebSocket消息处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageHandler {
    
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final WebSocketSessionManager sessionManager;
    private final ChatWebSocketHandler chatWebSocketHandler;
    
    /**
     * 处理消息
     */
    public void handleMessage(String userId, WebSocketMessage.BaseMessage message, WebSocketSession session) 
            throws IOException {
        
        switch (message.getType()) {
            case CHAT_MESSAGE:
                handleChatMessage(userId, message);
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
                chatWebSocketHandler.sendError(session, 400, "未知消息类型", message.getId());
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String userId, WebSocketMessage.BaseMessage message) {
        try {
            WebSocketMessage.ChatMessagePayload payload = objectMapper.convertValue(
                    message.getPayload(), WebSocketMessage.ChatMessagePayload.class);
            
            log.info("处理聊天消息: userId={}, conversationId={}", userId, payload.getConversationId());
            
            // 保存消息到数据库
            Message msg = Message.builder()
                    .conversationId(Long.parseLong(payload.getConversationId()))
                    .senderId(Long.parseLong(userId))
                    .senderType("USER")
                    .messageType(payload.getMessageType())
                    .content(payload.getContent())
                    .contentMetadata(payload.getMetadata() != null ? 
                            payload.getMetadata().toString() : null)
                    .intentData(payload.getIntentData())
                    .replyToMessageId(payload.getReplyToMessageId() != null ? 
                            Long.parseLong(payload.getReplyToMessageId()) : null)
                    .status("SENT")
                    .build();
            
            messageService.save(msg);
            
            // 广播给会话中的所有用户
            broadcastToConversation(payload.getConversationId(), message);
            
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
                    chatWebSocketHandler.sendMessageToUser(uid, message);
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
            
            log.info("处理已读回执: userId={}, messageId={}", userId, payload.getMessageId());
            
            // 更新消息已读状态
            messageService.markAsRead(Long.parseLong(payload.getMessageId()), Long.parseLong(userId));
            
            // 通知消息发送者
            // TODO: 获取消息发送者并通知
            
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
            chatWebSocketHandler.sendMessageToUser(userId, message);
        }
    }
}
