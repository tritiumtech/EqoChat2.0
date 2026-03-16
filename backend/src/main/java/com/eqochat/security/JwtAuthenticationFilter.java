package com.eqochat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenUtil jwtTokenUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // 放行公开接口
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取token
        String token = extractToken(request);
        if (log.isDebugEnabled()) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String preview = token == null ? "null" : token.substring(0, Math.min(12, token.length())) + "...";
            log.debug("Auth header present: {}, token preview: {} (path={})",
                    authHeader != null, preview, path);
        }
        
        if (token != null && jwtTokenUtil.validateToken(token)) {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String did = jwtTokenUtil.getDidFromToken(token);
            
            if (userId == null) {
                log.warn("JWT缺少userId: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT验证成功: userId={}, did={}", userId, did);
            
            // 创建认证对象
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

            // 设置SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (token == null) {
            log.warn("请求缺少Authorization头: {}", path);
        } else {
            log.warn("JWT验证失败: {}", path);
        }

        filterChain.doFilter(request, response);
    }
    
    /**
     * 提取token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }
        String trimmed = bearerToken.trim();
        if (trimmed.toLowerCase().startsWith("bearer")) {
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                return parts[1].trim();
            }
            return null;
        }
        return trimmed;
    }
    
    /**
     * 检查是否是公开路径
     */
    private boolean isPublicPath(String path) {
        return path.equals("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/register") ||
               path.equals("/api/v1/auth/verify-code") ||
               path.equals("/api/v1/auth/refresh") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/ws/") ||
               path.equals("/api/v1/health");
    }
}
