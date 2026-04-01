package com.eqochat.service;

import com.eqochat.common.I18nUtil;
import com.eqochat.websocket.ChatWebSocketHandler;
import com.eqochat.websocket.WebSocketMessage;
import com.eqochat.websocket.WebSocketSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
        
        // 存储用户->会话映射
        String userSessionKey = USER_SESSION_KEY + userId;
        redisTemplate.opsForValue().set(userSessionKey, newSessionId, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        // 存储会话->用户映射（用于反向查找）
        String sessionUserKey = SESSION_USER_KEY + newSessionId;
        redisTemplate.opsForValue().set(sessionUserKey, userId.toString(), SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        log.info("创建用户会话：userId={}, oldSessionId={}, newSessionId={}", userId, oldSessionId, newSessionId);
        
        // 如果存在旧会话，发送挤下线通知
        if (oldSessionId != null && !oldSessionId.equals(newSessionId)) {
            sendSessionKickedNotification(userId, oldSessionId);
        }
        
        return newSessionId;
    }
    
    /**
     * 发送挤下线通知
     */
    private void sendSessionKickedNotification(Long userId, String oldSessionId) {
        try {
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
                    .recipientId(userId.toString())
                    .timestamp(java.time.LocalDateTime.now())
                    .payload(payload)
                    .build();

            // 通过 WebSocketHandler 直接发送给旧会话
            chatWebSocketHandler.sendMessageToUser(userId.toString(), message);

            log.info("发送挤下线通知：userId={}, oldSessionId={}", userId, oldSessionId);
        } catch (Exception e) {
            log.warn("发送挤下线通知失败：userId={}", userId, e);
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
