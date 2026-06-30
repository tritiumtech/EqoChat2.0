package com.eqochat.business.chat.websocket;

import com.eqochat.framework.common.UserContext;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
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
    
    // principalHumanId -> session
    private final ConcurrentHashMap<String, WebSocketSession> principalHumanSessions = new ConcurrentHashMap<>();
    
    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String principalHumanId = extractPrincipalHumanId(session);
        if (principalHumanId == null) {
            log.warn("WebSocket连接未携带登录人类主体ID，关闭连接");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        
        // 存储会话
        principalHumanSessions.put(principalHumanId, session);
        sessionManager.registerPrincipalHumanSession(principalHumanId, session);
        
        // 设置用户上下文
        UserContext.setCurrentUser(Long.parseLong(principalHumanId));
        
        log.info("WebSocket连接建立: principalHumanId={}, sessionId={}", principalHumanId, session.getId());

        // 自动加入已存在会话
        try {
            Long humanId = Long.parseLong(principalHumanId);
            for (ConversationParticipant participant : participantService.listByParticipant(SubjectRef.human(humanId))) {
                if (participant.getConversationId() != null) {
                    sessionManager.joinConversationAsPrincipalHuman(participant.getConversationId().toString(), principalHumanId);
                }
            }
        } catch (Exception e) {
            log.warn("加载会话参与者失败: principalHumanId={}", principalHumanId, e);
        }
        
        // 发送连接确认
        sendConnectAck(session, principalHumanId);
        
        // 广播用户上线状态
        broadcastPresence(principalHumanId, "ONLINE");
    }
    
    /**
     * 收到文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String principalHumanId = extractPrincipalHumanId(session);
        
        log.debug("收到WebSocket消息: principalHumanId={}, payload={}", principalHumanId, payload);
        
        try {
            // 解析消息
            WebSocketMessage.BaseMessage baseMessage = objectMapper.readValue(
                    payload, WebSocketMessage.BaseMessage.class);
            
            // 设置用户上下文
            UserContext.setCurrentUser(Long.parseLong(principalHumanId));
            
            // 处理消息
            messageHandler.handleMessage(principalHumanId, baseMessage, session);
            
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
        String principalHumanId = extractPrincipalHumanId(session);
        if (principalHumanId != null) {
            principalHumanSessions.remove(principalHumanId);
            sessionManager.unregisterPrincipalHumanSession(principalHumanId);
            
            log.info("WebSocket连接关闭: principalHumanId={}, status={}", principalHumanId, status);
            
            // 广播用户离线状态
            broadcastPresence(principalHumanId, "OFFLINE");
        }
        
        // 清理用户上下文
        UserContext.clear();
    }
    
    /**
     * 传输异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String principalHumanId = extractPrincipalHumanId(session);
        log.error("WebSocket传输异常: principalHumanId={}", principalHumanId, exception);
        
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    /**
     * 从Session提取登录人类主体ID
     */
    private String extractPrincipalHumanId(WebSocketSession session) {
        String principalHumanId = (String) session.getAttributes().get("principalHumanId");
        if (principalHumanId != null && !principalHumanId.isEmpty()) {
            return principalHumanId;
        }
        
        log.warn("无法从WebSocketSession提取principalHumanId, sessionId={}", session.getId());
        return null;
    }
    
    /**
     * 发送连接确认
     */
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
                .senderSubjectId("0")
                .senderSubjectType(SubjectType.SYSTEM.name())
                .timestamp(LocalDateTime.now())
                .payload(error)
                .build();
        
        sendMessage(session, errorMessage);
    }
    
    /**
     * 广播人类主体在线状态
     */
    private void broadcastPresence(String principalHumanId, String status) {
        WebSocketMessage.PresencePayload presence = WebSocketMessage.PresencePayload.builder()
                .subjectId(principalHumanId)
                .subjectType(SubjectType.HUMAN.name())
                .status(status)
                .lastSeenAt(System.currentTimeMillis())
                .build();
        
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(status.equals("ONLINE") ? 
                        WebSocketMessage.MessageType.PRESENCE_ONLINE : 
                        WebSocketMessage.MessageType.PRESENCE_OFFLINE)
                .senderSubjectId(principalHumanId)
                .senderSubjectType(SubjectType.HUMAN.name())
                .timestamp(LocalDateTime.now())
                .payload(presence)
                .build();
        
        // 广播给所有在线用户（或好友列表）
        principalHumanSessions.forEach((targetPrincipalHumanId, session) -> {
            if (!targetPrincipalHumanId.equals(principalHumanId) && session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (IOException e) {
                    log.error("发送在线状态失败: targetPrincipalHumanId={}", targetPrincipalHumanId, e);
                }
            }
        });
    }
    
    /**
     * 发送消息给指定在线登录人类主体
     */
    public void sendMessageToPrincipalHuman(String principalHumanId, WebSocketMessage.BaseMessage message) {
        WebSocketSession session = principalHumanSessions.get(principalHumanId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, message);
            } catch (IOException e) {
                log.error("发送消息失败: principalHumanId={}", principalHumanId, e);
            }
        } else {
            log.debug("人类主体不在线: principalHumanId={}", principalHumanId);
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
     * 检查登录人类主体是否在线
     */
    public boolean isPrincipalHumanOnline(String principalHumanId) {
        WebSocketSession session = principalHumanSessions.get(principalHumanId);
        return session != null && session.isOpen();
    }
}
