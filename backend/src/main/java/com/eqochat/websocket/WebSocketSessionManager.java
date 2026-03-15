package com.eqochat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket会话管理器
 */
@Component
@Slf4j
public class WebSocketSessionManager {
    
    // userId -> WebSocketSession
    private final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
    
    // conversationId -> Set<userId>
    private final Map<String, Set<String>> conversationUsersMap = new ConcurrentHashMap<>();
    
    // sessionId -> userId
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    /**
     * 注册会话
     */
    public void registerSession(String userId, WebSocketSession session) {
        userSessionMap.put(userId, session);
        sessionUserMap.put(session.getId(), userId);
        log.info("注册WebSocket会话: userId={}, sessionId={}", userId, session.getId());
    }
    
    /**
     * 注销会话
     */
    public void unregisterSession(String userId) {
        WebSocketSession session = userSessionMap.remove(userId);
        if (session != null) {
            sessionUserMap.remove(session.getId());
        }
        
        // 从所有会话中移除
        conversationUsersMap.forEach((convId, users) -> users.remove(userId));
        
        log.info("注销WebSocket会话: userId={}", userId);
    }
    
    /**
     * 获取用户会话
     */
    public WebSocketSession getSession(String userId) {
        return userSessionMap.get(userId);
    }
    
    /**
     * 根据sessionId获取userId
     */
    public String getUserIdBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
    
    /**
     * 用户加入会话
     */
    public void joinConversation(String conversationId, String userId) {
        conversationUsersMap
                .computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>())
                .add(userId);
        log.debug("用户加入会话: conversationId={}, userId={}", conversationId, userId);
    }
    
    /**
     * 用户离开会话
     */
    public void leaveConversation(String conversationId, String userId) {
        Set<String> users = conversationUsersMap.get(conversationId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                conversationUsersMap.remove(conversationId);
            }
        }
        log.debug("用户离开会话: conversationId={}, userId={}", conversationId, userId);
    }
    
    /**
     * 获取会话中的所有用户
     */
    public Set<String> getConversationUsers(String conversationId) {
        return conversationUsersMap.getOrDefault(conversationId, new CopyOnWriteArraySet<>());
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isOnline(String userId) {
        WebSocketSession session = userSessionMap.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return (int) userSessionMap.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
    
    /**
     * 获取所有在线用户ID
     */
    public Set<String> getAllOnlineUsers() {
        return userSessionMap.keySet();
    }
}
