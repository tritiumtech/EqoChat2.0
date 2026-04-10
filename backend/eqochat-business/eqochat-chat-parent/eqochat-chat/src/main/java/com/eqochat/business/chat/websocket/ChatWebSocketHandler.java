package com.eqochat.business.chat.websocket;

import com.eqochat.framework.common.UserContext;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天WebSocket处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageHandler messageHandler;
    private final ConversationParticipantService participantService;
    
    // 存储用户会话 userId -> session
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId == null) {
            log.warn("WebSocket连接未携带用户ID，关闭连接");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        
        // 存储会话
        userSessions.put(userId, session);
        sessionManager.registerSession(userId, session);
        
        // 设置用户上下文
        UserContext.setCurrentUser(Long.parseLong(userId));
        
        log.info("WebSocket连接建立: userId={}, sessionId={}", userId, session.getId());

        // 自动加入已存在会话
        try {
            Long uid = Long.parseLong(userId);
            for (ConversationParticipant participant : participantService.listByParticipantId(uid)) {
                if (participant.getConversationId() != null) {
                    sessionManager.joinConversation(participant.getConversationId().toString(), userId);
                }
            }
        } catch (Exception e) {
            log.warn("加载会话参与者失败: userId={}", userId, e);
        }
        
        // 发送连接确认
        sendConnectAck(session, userId);
        
        // 广播用户上线状态
        broadcastPresence(userId, "ONLINE");
    }
    
    /**
     * 收到文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String userId = extractUserId(session);
        
        log.debug("收到WebSocket消息: userId={}, payload={}", userId, payload);
        
        try {
            // 解析消息
            WebSocketMessage.BaseMessage baseMessage = objectMapper.readValue(
                    payload, WebSocketMessage.BaseMessage.class);
            
            // 设置用户上下文
            UserContext.setCurrentUser(Long.parseLong(userId));
            
            // 处理消息
            messageHandler.handleMessage(userId, baseMessage, session);
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", payload, e);
            sendError(session, 500, "消息处理失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 连接关闭后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
            sessionManager.unregisterSession(userId);
            
            log.info("WebSocket连接关闭: userId={}, status={}", userId, status);
            
            // 广播用户离线状态
            broadcastPresence(userId, "OFFLINE");
        }
        
        // 清理用户上下文
        UserContext.clear();
    }
    
    /**
     * 传输异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserId(session);
        log.error("WebSocket传输异常: userId={}", userId, exception);
        
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    /**
     * 从Session提取用户ID
     */
    private String extractUserId(WebSocketSession session) {
        // 优先从attributes获取（通过WebSocketAuthInterceptor拦截器设置）
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        
        // 从URI参数获取 ?userId=xxx (兼容旧版本)
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("userId=")) {
            return query.substring(7);
        }
        
        log.warn("无法从WebSocketSession提取userId, sessionId={}", session.getId());
        return null;
    }
    
    /**
     * 发送连接确认
     */
    private void sendConnectAck(WebSocketSession session, String userId) throws IOException {
        WebSocketMessage.ConnectAckPayload ack = WebSocketMessage.ConnectAckPayload.builder()
                .userId(userId)
                .connectionId(UUID.randomUUID().toString())
                .serverTime(System.currentTimeMillis())
                .build();
        
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.CONNECT_ACK)
                .senderId("system")
                .senderType("SYSTEM")
                .recipientId(userId)
                .timestamp(LocalDateTime.now())
                .payload(ack)
                .build();
        
        sendMessage(session, message);
    }
    
    /**
     * 发送错误消息
     */
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
                .senderId("system")
                .senderType("SYSTEM")
                .timestamp(LocalDateTime.now())
                .payload(error)
                .build();
        
        sendMessage(session, errorMessage);
    }
    
    /**
     * 广播用户在线状态
     */
    private void broadcastPresence(String userId, String status) {
        WebSocketMessage.PresencePayload presence = WebSocketMessage.PresencePayload.builder()
                .userId(userId)
                .status(status)
                .lastSeenAt(System.currentTimeMillis())
                .build();
        
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(status.equals("ONLINE") ? 
                        WebSocketMessage.MessageType.PRESENCE_ONLINE : 
                        WebSocketMessage.MessageType.PRESENCE_OFFLINE)
                .senderId(userId)
                .senderType("USER")
                .timestamp(LocalDateTime.now())
                .payload(presence)
                .build();
        
        // 广播给所有在线用户（或好友列表）
        userSessions.forEach((uid, session) -> {
            if (!uid.equals(userId) && session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (IOException e) {
                    log.error("发送在线状态失败: targetUserId={}", uid, e);
                }
            }
        });
    }
    
    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(String userId, WebSocketMessage.BaseMessage message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, message);
            } catch (IOException e) {
                log.error("发送消息失败: userId={}", userId, e);
            }
        } else {
            log.debug("用户不在线: userId={}", userId);
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage.BaseMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(payload));
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
