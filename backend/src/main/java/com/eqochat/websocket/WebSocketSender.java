package com.eqochat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket发送工具，解耦消息处理与具体处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSender {
    
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    
    public void sendToUser(String userId, WebSocketMessage.BaseMessage message) {
        WebSocketSession session = sessionManager.getSession(userId);
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
    
    public void sendError(WebSocketSession session, Integer code, String message, String originalMessageId) {
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
        
        try {
            sendMessage(session, errorMessage);
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }
    
    private void sendMessage(WebSocketSession session, WebSocketMessage.BaseMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(payload));
    }
}
