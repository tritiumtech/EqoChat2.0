package com.eqochat.business.chat.service.impl;

import com.eqochat.business.chat.api.session.UserSessionApi;
import com.eqochat.business.chat.websocket.ChatWebSocketHandler;
import com.eqochat.business.actor.api.model.SubjectType;
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
 * 登录人类主体会话管理服务：单设备登录、会话校验。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService implements UserSessionApi {

    private final StringRedisTemplate redisTemplate;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketSessionManager webSocketSessionManager;

    private static final String PRINCIPAL_HUMAN_SESSION_KEY = "principal_human:session:";
    private static final String SESSION_PRINCIPAL_HUMAN_KEY = "session:principal_human:";
    private static final long SESSION_EXPIRE_SECONDS = 432000L;

    @Override
    public String createSession(Long principalHumanId) {
        String oldSessionId = getSessionId(principalHumanId);
        String newSessionId = UUID.randomUUID().toString().replace("-", "");

        log.info("准备创建会话：principalHumanId={}, oldSessionId={}, newSessionId={}", principalHumanId, oldSessionId, newSessionId);

        if (oldSessionId != null && !oldSessionId.equals(newSessionId)) {
            sendSessionKickedNotification(principalHumanId, oldSessionId);
            redisTemplate.delete(SESSION_PRINCIPAL_HUMAN_KEY + oldSessionId);
        }

        redisTemplate.opsForValue().set(PRINCIPAL_HUMAN_SESSION_KEY + principalHumanId, newSessionId, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(SESSION_PRINCIPAL_HUMAN_KEY + newSessionId, principalHumanId.toString(), SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return newSessionId;
    }

    private void sendSessionKickedNotification(Long principalHumanIdValue, String oldSessionId) {
        String principalHumanId = principalHumanIdValue.toString();
        WebSocketSession oldSession = null;
        try {
            oldSession = webSocketSessionManager.getPrincipalHumanSession(principalHumanId);
            if (oldSession == null || !oldSession.isOpen()) {
                log.info("登录人类主体 {} 没有活跃的 WebSocket 连接，跳过发送踢人通知", principalHumanId);
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
                    .senderSubjectId("0")
                    .senderSubjectType(SubjectType.SYSTEM.name())
                    .timestamp(java.time.LocalDateTime.now())
                    .payload(payload)
                    .build();
            chatWebSocketHandler.sendMessageToPrincipalHuman(principalHumanId, message);
        } catch (Exception e) {
            log.warn("发送挤下线通知失败：principalHumanId={}", principalHumanId, e);
        } finally {
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.warn("关闭旧 WebSocket 连接失败：principalHumanId={}", principalHumanId, e);
                }
            }
        }
    }

    @Override
    public String getSessionId(Long principalHumanId) {
        return redisTemplate.opsForValue().get(PRINCIPAL_HUMAN_SESSION_KEY + principalHumanId);
    }

    @Override
    public Long getPrincipalHumanIdBySession(String sessionId) {
        String principalHumanId = redisTemplate.opsForValue().get(SESSION_PRINCIPAL_HUMAN_KEY + sessionId);
        if (principalHumanId == null || principalHumanId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(principalHumanId);
        } catch (NumberFormatException e) {
            log.error("解析 principalHumanId 失败：sessionId={}", sessionId, e);
            return null;
        }
    }

    @Override
    public boolean validateSession(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_PRINCIPAL_HUMAN_KEY + sessionId));
    }

    @Override
    public void removeSession(String sessionId) {
        String sessionPrincipalHumanKey = SESSION_PRINCIPAL_HUMAN_KEY + sessionId;
        String principalHumanId = redisTemplate.opsForValue().get(sessionPrincipalHumanKey);
        if (principalHumanId != null) {
            redisTemplate.delete(PRINCIPAL_HUMAN_SESSION_KEY + principalHumanId);
        }
        redisTemplate.delete(sessionPrincipalHumanKey);
    }

    @Override
    public void refreshSession(String sessionId) {
        String sessionPrincipalHumanKey = SESSION_PRINCIPAL_HUMAN_KEY + sessionId;
        redisTemplate.expire(sessionPrincipalHumanKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        Long principalHumanId = getPrincipalHumanIdBySession(sessionId);
        if (principalHumanId != null) {
            redisTemplate.expire(PRINCIPAL_HUMAN_SESSION_KEY + principalHumanId, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
