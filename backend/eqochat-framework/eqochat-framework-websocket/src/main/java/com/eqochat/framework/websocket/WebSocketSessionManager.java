package com.eqochat.framework.websocket;

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
    
    // principalHumanId -> WebSocketSession
    private final Map<String, WebSocketSession> principalHumanSessionMap = new ConcurrentHashMap<>();
    
    // conversationId -> Set<principalHumanId>
    private final Map<String, Set<String>> conversationPrincipalHumansMap = new ConcurrentHashMap<>();
    
    // sessionId -> principalHumanId
    private final Map<String, String> sessionPrincipalHumanMap = new ConcurrentHashMap<>();
    
    /**
     * 注册会话
     */
    public void registerPrincipalHumanSession(String principalHumanId, WebSocketSession session) {
        principalHumanSessionMap.put(principalHumanId, session);
        sessionPrincipalHumanMap.put(session.getId(), principalHumanId);
        log.info("注册WebSocket会话: principalHumanId={}, sessionId={}", principalHumanId, session.getId());
    }
    
    /**
     * 注销会话
     */
    public void unregisterPrincipalHumanSession(String principalHumanId) {
        WebSocketSession session = principalHumanSessionMap.remove(principalHumanId);
        if (session != null) {
            sessionPrincipalHumanMap.remove(session.getId());
        }
        
        // 从所有会话中移除
        conversationPrincipalHumansMap.forEach((convId, humans) -> humans.remove(principalHumanId));
        
        log.info("注销WebSocket会话: principalHumanId={}", principalHumanId);
    }
    
    /**
     * 获取登录人类主体会话
     */
    public WebSocketSession getPrincipalHumanSession(String principalHumanId) {
        return principalHumanSessionMap.get(principalHumanId);
    }
    
    /**
     * 根据 sessionId 获取登录人类主体 ID
     */
    public String getPrincipalHumanIdBySessionId(String sessionId) {
        return sessionPrincipalHumanMap.get(sessionId);
    }
    
    /**
     * 登录人类主体加入会话
     */
    public void joinConversationAsPrincipalHuman(String conversationId, String principalHumanId) {
        conversationPrincipalHumansMap
                .computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>())
                .add(principalHumanId);
        log.debug("人类主体加入会话: conversationId={}, principalHumanId={}", conversationId, principalHumanId);
    }
    
    /**
     * 登录人类主体离开会话
     */
    public void leaveConversationAsPrincipalHuman(String conversationId, String principalHumanId) {
        Set<String> humans = conversationPrincipalHumansMap.get(conversationId);
        if (humans != null) {
            humans.remove(principalHumanId);
            if (humans.isEmpty()) {
                conversationPrincipalHumansMap.remove(conversationId);
            }
        }
        log.debug("人类主体离开会话: conversationId={}, principalHumanId={}", conversationId, principalHumanId);
    }
    
    /**
     * 获取会话中的所有登录人类主体
     */
    public Set<String> getConversationPrincipalHumans(String conversationId) {
        return conversationPrincipalHumansMap.getOrDefault(conversationId, new CopyOnWriteArraySet<>());
    }
    
    /**
     * 检查登录人类主体是否在线
     */
    public boolean isPrincipalHumanOnline(String principalHumanId) {
        WebSocketSession session = principalHumanSessionMap.get(principalHumanId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线登录人类主体数量
     */
    public int getOnlinePrincipalHumanCount() {
        return (int) principalHumanSessionMap.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
    
    /**
     * 获取所有在线登录人类主体 ID
     */
    public Set<String> getAllOnlinePrincipalHumans() {
        return principalHumanSessionMap.keySet();
    }
}
