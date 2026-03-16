package com.eqochat.config;

import com.eqochat.common.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文过滤器
 * 从请求中提取用户ID并设置到ThreadLocal
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Long userId = resolveUserId(request);
            if (userId != null) {
                UserContext.setCurrentUser(userId);
                log.debug("设置用户上下文: {}", userId);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            log.debug("清理用户上下文");
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
            if (principal instanceof String) {
                try {
                    return Long.parseLong((String) principal);
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
        }
        String userIdStr = request.getHeader(USER_ID_HEADER);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userIdStr);
            return null;
        }
    }
}
