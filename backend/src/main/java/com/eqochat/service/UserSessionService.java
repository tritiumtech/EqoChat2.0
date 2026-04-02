package com.eqochat.service;

import com.eqochat.common.I18nUtil;
import com.eqochat.websocket.ChatWebSocketHandler;
import com.eqochat.websocket.WebSocketMessage;
import com.eqochat.websocket.WebSocketSender;
import com.eqochat.websocket.WebSocketSessionManager;
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
 * 用户会话管理服务
 * 实现单设备登录：每个用户只能有一个有效的登录会话
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final StringRedisTemplate redisTemplate;
    private final WebSocketSender webSocketSender;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketSessionManager webSocketSessionManager;
    
    private static final String USER_SESSION_KEY = "user:session:";
    private static final String SESSION_USER_KEY = "session:user:";
    /**
     * Session 过期时间：5 天（与 token 过期时间一致）
     */
    private static final long SESSION_EXPIRE_SECONDS = 432000L;
    
    /**
     * 创建新会话，并使旧会话失效
     * @param userId 用户 ID
     * @return 新的 sessionId
     */
    public String createSession(Long userId) {
        String oldSessionId = getSessionId(userId);

        // 生成新的 sessionId
        String newSessionId = UUID.randomUUID().toString().replace("-", "");

        log.info("准备创建会话：userId={}, oldSessionId={}, newSessionId={}", userId, oldSessionId, newSessionId);

        // 如果存在旧会话，先发送挤下线通知，再删除旧会话
        if (oldSessionId != null && !oldSessionId.equals(newSessionId)) {
            // 先发送通知（旧 session 还在，WebSocket 连接还在）
            sendSessionKickedNotification(userId, oldSessionId);
            // 删除旧 session 的映射（使旧 token 失效）
            Boolean deleted = redisTemplate.delete(SESSION_USER_KEY + oldSessionId);
            log.info("已删除旧 session:user:{} 结果: {}", oldSessionId, deleted);
        }

        // 存储用户->会话映射（覆盖旧的）
        String userSessionKey = USER_SESSION_KEY + userId;
        redisTemplate.opsForValue().set(userSessionKey, newSessionId, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("已设置 user:session:{} = {}", userId, newSessionId);

        // 存储会话->用户映射（用于反向查找）
        String sessionUserKey = SESSION_USER_KEY + newSessionId;
        redisTemplate.opsForValue().set(sessionUserKey, userId.toString(), SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("已设置 session:user:{} = {}", newSessionId, userId);

        return newSessionId;
    }
    
    /**
     * 发送挤下线通知并关闭旧连接
     */
    private void sendSessionKickedNotification(Long userId, String oldSessionId) {
        String userIdStr = userId.toString();
        WebSocketSession oldSession = null;

        try {
            // 获取旧 WebSocket 连接
            oldSession = webSocketSessionManager.getSession(userIdStr);

            if (oldSession == null || !oldSession.isOpen()) {
                log.info("用户 {} 没有活跃的 WebSocket 连接，跳过发送踢人通知", userId);
                return;
            }

            // 使用国际化消息
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

            // 发送踢人通知
            chatWebSocketHandler.sendMessageToUser(userIdStr, message);
            log.info("已发送挤下线通知：userId={}, oldSessionId={}", userId, oldSessionId);

        } catch (Exception e) {
            log.warn("发送挤下线通知失败：userId={}", userId, e);
        } finally {
            // 无论消息是否发送成功，都关闭旧连接
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close(CloseStatus.NORMAL);
                    log.info("已关闭旧 WebSocket 连接：userId={}", userId);
                } catch (IOException e) {
                    log.warn("关闭旧 WebSocket 连接失败：userId={}", userId, e);
                }
            }
        }
    }
    
    /**
     * 获取用户的当前 sessionId
     */
    public String getSessionId(Long userId) {
        String key = USER_SESSION_KEY + userId;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 根据 sessionId 获取 userId
     */
    public Long getUserIdBySession(String sessionId) {
        String key = SESSION_USER_KEY + sessionId;
        String userIdStr = redisTemplate.opsForValue().get(key);
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
    
    /**
     * 验证 sessionId 是否有效
     */
    public boolean validateSession(String sessionId) {
        String key = SESSION_USER_KEY + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * 删除会话（登出时使用）
     */
    public void removeSession(String sessionId) {
        String sessionUserKey = SESSION_USER_KEY + sessionId;
        String userIdStr = redisTemplate.opsForValue().get(sessionUserKey);
        
        if (userIdStr != null) {
            String userSessionKey = USER_SESSION_KEY + userIdStr;
            redisTemplate.delete(userSessionKey);
        }
        
        redisTemplate.delete(sessionUserKey);
        log.info("删除用户会话：sessionId={}", sessionId);
    }
    
    /**
     * 刷新会话过期时间
     */
    public void refreshSession(String sessionId) {
        String sessionUserKey = SESSION_USER_KEY + sessionId;
        redisTemplate.expire(sessionUserKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        Long userId = getUserIdBySession(sessionId);
        if (userId != null) {
            String userSessionKey = USER_SESSION_KEY + userId;
            redisTemplate.expire(userSessionKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
