package com.eqochat.business.chat.api.session;

/**
 * 登录人类主体会话（Redis + 可选 WebSocket 挤下线），供 HTTP 过滤器与认证服务依赖。
 */
public interface UserSessionApi {

    String createSession(Long principalHumanId);

    String getSessionId(Long principalHumanId);

    Long getPrincipalHumanIdBySession(String sessionId);

    boolean validateSession(String sessionId);

    void removeSession(String sessionId);

    void refreshSession(String sessionId);
}
