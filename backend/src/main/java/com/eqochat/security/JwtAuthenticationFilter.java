package com.eqochat.security;

import com.eqochat.common.UserContext;
import com.eqochat.service.UserSessionService;
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
import java.util.Date;

/**
 * JWT认证过滤器（支持单设备登录验证）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserSessionService userSessionService;
    
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
            String sessionId = jwtTokenUtil.getSessionIdFromToken(token);

            if (userId == null) {
                log.warn("JWT缺少userId: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // 验证 sessionId 是否有效（单设备登录检查）
            if (sessionId != null && !userSessionService.validateSession(sessionId)) {
                log.warn("Session 已失效（被挤下线）: userId={}, sessionId={}", userId, sessionId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Session expired, please login again\"}");
                return;
            }

            log.debug("JWT验证成功: userId={}, did={}, sessionId={}", userId, did, sessionId);

            // 创建认证对象
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

            // 设置SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 设置用户上下文
            UserContext.setCurrentUser(userId);

            // 每次请求自动续期 JWT（滑动过期）
            try {
                Date exp = jwtTokenUtil.getExpirationDateFromToken(token);
                Date now = new Date();
                // token 仍然有效，则直接刷新一个新的并透出到响应头
                if (exp != null && exp.after(now)) {
                    String newToken = jwtTokenUtil.refreshToken(token);
                    response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newToken);
                    // 允许前端 JS 读取 Authorization 响应头
                    String expose = response.getHeader("Access-Control-Expose-Headers");
                    if (expose == null || expose.isBlank()) {
                        response.setHeader("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION);
                    } else if (!expose.toLowerCase().contains("authorization")) {
                        response.setHeader("Access-Control-Expose-Headers", expose + ", " + HttpHeaders.AUTHORIZATION);
                    }
                }
            } catch (Exception e) {
                log.warn("刷新 JWT 失败（忽略，不影响本次请求）: {}", e.getMessage());
            }
        } else if (token == null) {
            log.warn("请求缺少Authorization头: {}", path);
        } else {
            log.warn("JWT验证失败: {}", path);
        }

        filterChain.doFilter(request, response);

        // 清理用户上下文
        UserContext.clear();
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
               path.equals("/api/v1/auth/login/email") ||
               path.equals("/api/v1/auth/register") ||
               path.equals("/api/v1/auth/register/email") ||
               path.equals("/api/v1/auth/verify-code") ||
               path.equals("/api/v1/auth/verify-code/email") ||
               path.equals("/api/v1/auth/refresh") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/ws/") ||
               path.equals("/api/v1/health");
    }
}
