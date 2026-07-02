package com.eqochat.framework.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class WebSocketSessionManager {

    private final Map<String, WebSocketSession> principalHumanSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> conversationPrincipalHumansMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionPrincipalHumanMap = new ConcurrentHashMap<>();

    private final Map<String, Set<WebSocketSession>> subjectSessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionActiveSubjectMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubjectKeysMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> conversationSubjectMap = new ConcurrentHashMap<>();

    public void registerPrincipalHumanSession(String principalHumanId, WebSocketSession session) {
        WebSocketSession replaced = principalHumanSessionMap.put(principalHumanId, session);
        if (replaced != null && !replaced.getId().equals(session.getId())) {
            sessionPrincipalHumanMap.remove(replaced.getId(), principalHumanId);
            unregisterActiveSubjectSession(replaced);
        }
        sessionPrincipalHumanMap.put(session.getId(), principalHumanId);
        log.info("register WebSocket principal session: principalHumanId={}, sessionId={}",
                principalHumanId, session.getId());
    }

    public void unregisterPrincipalHumanSession(String principalHumanId) {
        WebSocketSession session = principalHumanSessionMap.remove(principalHumanId);
        if (session != null) {
            sessionPrincipalHumanMap.remove(session.getId());
            unregisterActiveSubjectSession(session);
        }

        conversationPrincipalHumansMap.forEach((convId, humans) -> humans.remove(principalHumanId));
        log.info("unregister WebSocket principal session: principalHumanId={}", principalHumanId);
    }

    public boolean unregisterPrincipalHumanSession(String principalHumanId, WebSocketSession session) {
        if (session == null) {
            return false;
        }
        boolean removedCurrent = principalHumanSessionMap.remove(principalHumanId, session);
        sessionPrincipalHumanMap.remove(session.getId(), principalHumanId);
        unregisterActiveSubjectSession(session);
        if (removedCurrent) {
            conversationPrincipalHumansMap.forEach((convId, humans) -> humans.remove(principalHumanId));
            log.info("unregister WebSocket principal session: principalHumanId={}, sessionId={}",
                    principalHumanId, session.getId());
        } else {
            log.debug("closed stale WebSocket session ignored for principal map: principalHumanId={}, sessionId={}",
                    principalHumanId, session.getId());
        }
        return removedCurrent;
    }

    public WebSocketSession getPrincipalHumanSession(String principalHumanId) {
        return principalHumanSessionMap.get(principalHumanId);
    }

    public String getPrincipalHumanIdBySessionId(String sessionId) {
        return sessionPrincipalHumanMap.get(sessionId);
    }

    public void registerActiveSubjectSession(
            String principalHumanId,
            String subjectId,
            String subjectType,
            WebSocketSession session
    ) {
        String key = subjectKey(subjectId, subjectType);
        sessionActiveSubjectMap.put(session.getId(), key);
        registerSubjectSession(principalHumanId, subjectId, subjectType, session);
    }

    public void registerSubjectSession(
            String principalHumanId,
            String subjectId,
            String subjectType,
            WebSocketSession session
    ) {
        String key = subjectKey(subjectId, subjectType);
        sessionSubjectKeysMap
                .computeIfAbsent(session.getId(), ignored -> new CopyOnWriteArraySet<>())
                .add(key);
        subjectSessionMap.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("register subject session: principalHumanId={}, subject={}, sessionId={}",
                principalHumanId, key, session.getId());
    }

    public void unregisterActiveSubjectSession(WebSocketSession session) {
        if (session == null) {
            return;
        }
        sessionActiveSubjectMap.remove(session.getId());
        Set<String> subjects = sessionSubjectKeysMap.remove(session.getId());
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        for (String subject : subjects) {
            Set<WebSocketSession> sessions = subjectSessionMap.get(subject);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    subjectSessionMap.remove(subject);
                }
            }
        }
    }

    public Set<WebSocketSession> getSubjectSessions(String subjectId, String subjectType) {
        return getSubjectSessionsByKey(subjectKey(subjectId, subjectType));
    }

    public Set<WebSocketSession> getSubjectSessionsByKey(String subjectKey) {
        return subjectSessionMap.getOrDefault(subjectKey, new CopyOnWriteArraySet<>());
    }

    public boolean isSubjectOnline(String subjectId, String subjectType) {
        return getSubjectSessions(subjectId, subjectType).stream()
                .anyMatch(WebSocketSession::isOpen);
    }

    public String getActiveSubjectKeyBySessionId(String sessionId) {
        return sessionActiveSubjectMap.get(sessionId);
    }

    public Set<String> getSubjectKeysBySessionId(String sessionId) {
        return sessionSubjectKeysMap.getOrDefault(sessionId, new CopyOnWriteArraySet<>());
    }

    public void joinConversationAsPrincipalHuman(String conversationId, String principalHumanId) {
        conversationPrincipalHumansMap
                .computeIfAbsent(conversationId, ignored -> new CopyOnWriteArraySet<>())
                .add(principalHumanId);
        joinConversationAsSubject(conversationId, principalHumanId, "HUMAN");
        log.debug("principal human joined conversation: conversationId={}, principalHumanId={}",
                conversationId, principalHumanId);
    }

    public void leaveConversationAsPrincipalHuman(String conversationId, String principalHumanId) {
        Set<String> humans = conversationPrincipalHumansMap.get(conversationId);
        if (humans != null) {
            humans.remove(principalHumanId);
            if (humans.isEmpty()) {
                conversationPrincipalHumansMap.remove(conversationId);
            }
        }
        leaveConversationAsSubject(conversationId, principalHumanId, "HUMAN");
        log.debug("principal human left conversation: conversationId={}, principalHumanId={}",
                conversationId, principalHumanId);
    }

    public Set<String> getConversationPrincipalHumans(String conversationId) {
        return conversationPrincipalHumansMap.getOrDefault(conversationId, new CopyOnWriteArraySet<>());
    }

    public void joinConversationAsSubject(String conversationId, String subjectId, String subjectType) {
        conversationSubjectMap
                .computeIfAbsent(conversationId, ignored -> new CopyOnWriteArraySet<>())
                .add(subjectKey(subjectId, subjectType));
        log.debug("subject joined conversation: conversationId={}, subject={}",
                conversationId, subjectKey(subjectId, subjectType));
    }

    public void leaveConversationAsSubject(String conversationId, String subjectId, String subjectType) {
        Set<String> subjects = conversationSubjectMap.get(conversationId);
        if (subjects != null) {
            subjects.remove(subjectKey(subjectId, subjectType));
            if (subjects.isEmpty()) {
                conversationSubjectMap.remove(conversationId);
            }
        }
    }

    public Set<String> getConversationSubjectKeys(String conversationId) {
        return conversationSubjectMap.getOrDefault(conversationId, new CopyOnWriteArraySet<>());
    }

    public String subjectKey(String subjectId, String subjectType) {
        if (subjectId == null || subjectId.isBlank() || subjectType == null || subjectType.isBlank()) {
            throw new IllegalArgumentException("subject id and type are required");
        }
        return subjectType.trim().toUpperCase() + ":" + subjectId.trim();
    }

    public String subjectIdFromKey(String subjectKey) {
        int i = subjectKey.indexOf(':');
        return i < 0 ? subjectKey : subjectKey.substring(i + 1);
    }

    public String subjectTypeFromKey(String subjectKey) {
        int i = subjectKey.indexOf(':');
        return i < 0 ? "" : subjectKey.substring(0, i);
    }

    public boolean isPrincipalHumanOnline(String principalHumanId) {
        WebSocketSession session = principalHumanSessionMap.get(principalHumanId);
        return session != null && session.isOpen();
    }

    public int getOnlinePrincipalHumanCount() {
        return (int) principalHumanSessionMap.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

    public Set<String> getAllOnlinePrincipalHumans() {
        return principalHumanSessionMap.keySet();
    }
}
