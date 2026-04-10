package com.eqochat.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
@Slf4j
public class JwtTokenUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成 token（带 sessionId）
     */
    public String generateToken(Long userId, String did, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("did", did);
        claims.put("sessionId", sessionId);
        
        return createToken(claims, userId.toString());
    }
    
    /**
     * 生成 token（旧版本，兼容用）
     */
    public String generateToken(Long userId, String did) {
        return generateToken(userId, did, null);
    }
    
    /**
     * 从 token 获取 sessionId
     */
    public String getSessionIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("sessionId", String.class);
    }
    
    /**
     * 创建 token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 从 token 获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object raw = claims.get("userId");
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String subject = claims.getSubject();
        if (subject != null) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 从 token 获取 DID
     */
    public String getDidFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("did", String.class);
    }
    
    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT 验证失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查 token 是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * 获取 token 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }
    
    /**
     * 获取所有 Claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 刷新 token
     */
    public String refreshToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return createToken(claims, claims.getSubject());
    }
}
