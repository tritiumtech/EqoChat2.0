package com.eqochat.business.chat.service.impl;

import com.eqochat.business.chat.api.session.UserSessionApi;
import com.eqochat.business.chat.websocket.ChatWebSocketHandler;
import com.eqochat.framework.common.I18nUtil;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户会话管理服务：单设备登录、会话校验。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService implements UserSessionApi {

    private final StringRedisTemplate redisTemplate;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketSessionManager webSocketSessionManager;

    private static final String USER_SESSION_KEY = "user:session:";
    private static final String SESSION_USER_KEY = "session:user:";
    private static final long SESSION_EXPIRE_SECONDS = 432000L;

    @Override
    public String createSession(Long userId) {
        String oldSessionId = getSessionId(userId);
        String newSessionId = UUID.randomUUID().toString().replace("-", "");

        log.info("准备创建会话：userId={}, oldSessionId={}, newSessionId={}", userId, oldSessionId, newSessionId);

        if (oldSessionId != null && !oldSessionId.equals(newSessionId)) {
            sendSessionKickedNotification(userId, oldSessionId);
            redisTemplate.delete(SESSION_USER_KEY + oldSessionId);
        }

        redisTemplate.opsForValue().set(USER_SESSION_KEY + userId, newSessionId, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(SESSION_USER_KEY + newSessionId, userId.toString(), SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return newSessionId;
    }

    private void sendSessionKickedNotification(Long userId, String oldSessionId) {
        String userIdStr = userId.toString();
        WebSocketSession oldSession = null;
        try {
            oldSession = webSocketSessionManager.getSession(userIdStr);
            if (oldSession == null || !oldSession.isOpen()) {
                log.info("用户 {} 没有活跃的 WebSocket 连接，跳过发送踢人通知", userId);
                return;
            }
            String reason = I18nUtil.get("auth.session.kicked");
            WebSocketMessage.SessionKickedPayload payload = WebSocketMessage.SessionKickedPayload.builder()
                    .reason(reason)
                    .kickedAt(java.time.LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .build();
            WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .type(WebSocketMessage.MessageType.SESSION_KICKED)
                    .senderId("system")
                    .senderType("SYSTEM")
                    .recipientId(userIdStr)
                    .timestamp(java.time.LocalDateTime.now())
                    .payload(payload)
                    .build();
            chatWebSocketHandler.sendMessageToUser(userIdStr, message);
        } catch (Exception e) {
            log.warn("发送挤下线通知失败：userId={}", userId, e);
        } finally {
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.warn("关闭旧 WebSocket 连接失败：userId={}", userId, e);
                }
            }
        }
    }

    @Override
    public String getSessionId(Long userId) {
        return redisTemplate.opsForValue().get(USER_SESSION_KEY + userId);
    }

    @Override
    public Long getUserIdBySession(String sessionId) {
        String userIdStr = redisTemplate.opsForValue().get(SESSION_USER_KEY + sessionId);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.error("解析 userId 失败：sessionId={}", sessionId, e);
            return null;
        }
    }

    @Override
    public boolean validateSession(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_USER_KEY + sessionId));
    }

    @Override
    public void removeSession(String sessionId) {
        String sessionUserKey = SESSION_USER_KEY + sessionId;
        String userIdStr = redisTemplate.opsForValue().get(sessionUserKey);
        if (userIdStr != null) {
            redisTemplate.delete(USER_SESSION_KEY + userIdStr);
        }
        redisTemplate.delete(sessionUserKey);
    }

    @Override
    public void refreshSession(String sessionId) {
        String sessionUserKey = SESSION_USER_KEY + sessionId;
        redisTemplate.expire(sessionUserKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        Long userId = getUserIdBySession(sessionId);
        if (userId != null) {
            redisTemplate.expire(USER_SESSION_KEY + userId, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
