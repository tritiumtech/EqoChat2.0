package com.eqochat.framework.security;

import com.eqochat.business.chat.api.session.UserSessionApi;
import com.eqochat.framework.common.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * WebSocket JWT 认证拦截器（支持单设备登录验证）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private final UserSessionApi userSessionApi;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        // 从 URL 参数获取 token ?token=xxx
        String query = request.getURI().getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            token = query.substring(query.indexOf("token=") + 6);
            if (token.contains("&")) {
                token = token.substring(0, token.indexOf("&"));
            }
        }
        
        // 从 Header 获取
        if (token == null) {
            List<String> authHeaders = request.getHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String auth = authHeaders.get(0);
                if (auth.startsWith("Bearer ")) {
                    token = auth.substring(7);
                }
            }
        }
        
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 连接缺少 token");
            return false;
        }
        
        try {
            // 验证 token
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String userId = claims.getSubject();
            String sessionId = claims.get("sessionId", String.class);
            
            // 验证 sessionId 是否有效（单设备登录检查）
            if (sessionId != null && !userSessionApi.validateSession(sessionId)) {
                log.warn("WebSocket 连接失败：sessionId 已失效 (被挤下线), userId={}, sessionId={}", userId, sessionId);
                // 可以在这里设置特殊的状态码或响应头，通知前端是被挤下线的
                return false;
            }
            
            attributes.put("userId", userId);
            attributes.put("sessionId", sessionId);
            
            // 设置用户上下文
            UserContext.setCurrentUser(Long.parseLong(userId));
            
            log.info("WebSocket 认证成功：userId={}, sessionId={}", userId, sessionId);
            return true;
            
        } catch (Exception e) {
            log.error("WebSocket 认证失败：{}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后清理上下文
        UserContext.clear();
    }
}
