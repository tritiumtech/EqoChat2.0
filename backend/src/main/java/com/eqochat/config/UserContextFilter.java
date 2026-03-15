package com.eqochat.config;

import com.eqochat.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 用户上下文过滤器
 * 从请求中提取用户ID并设置到ThreadLocal
 */
@Slf4j
@Component
public class UserContextFilter implements WebFilter {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userIdStr = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                UserContext.setCurrentUser(userId);
                log.debug("从请求头提取用户ID: {}", userId);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID格式: {}", userIdStr);
            }
        }
        
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // 请求结束后清理ThreadLocal
                    UserContext.clear();
                    log.debug("清理用户上下文");
                });
    }
}
