package com.eqochat.security;

import com.eqochat.common.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
 * WebSocket JWT认证拦截器
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        // 从URL参数获取token ?token=xxx
        String query = request.getURI().getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            token = query.substring(query.indexOf("token=") + 6);
            if (token.contains("&")) {
                token = token.substring(0, token.indexOf("&"));
            }
        }
        
        // 从Header获取
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
            log.warn("WebSocket连接缺少token");
            return false;
        }
        
        try {
            // 验证token
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String userId = claims.getSubject();
            attributes.put("userId", userId);
            
            // 设置用户上下文
            UserContext.setCurrentUser(Long.parseLong(userId));
            
            log.info("WebSocket认证成功: userId={}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("WebSocket认证失败: {}", e.getMessage());
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
