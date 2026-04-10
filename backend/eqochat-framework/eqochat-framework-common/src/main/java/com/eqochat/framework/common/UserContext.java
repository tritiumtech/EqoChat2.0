package com.eqochat.framework.common;

import lombok.extern.slf4j.Slf4j;


/**
 * 当前用户上下文
 * 使用ThreadLocal存储当前登录用户ID，由 UserContextFilter 设置
 */
@Slf4j
public class UserContext {
    
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    
    /**
     * 设置当前用户
     */
    public static void setCurrentUser(Long userId) {
        CURRENT_USER.set(userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUser() {
        Long userId = CURRENT_USER.get();
        return userId;
    }
    
    /**
     * 清除当前用户
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
    
    /**
     * 获取当前用户ID，如果不存在返回默认值
     */
    public static Long getCurrentUserOrDefault(Long defaultValue) {
        Long userId = CURRENT_USER.get();
        return userId != null ? userId : defaultValue;
    }
    
    /**
     * 获取当前用户ID，如果不存在返回0（系统用户）
     */
    public static Long getCurrentUserOrSystem() {
        return getCurrentUserOrDefault(0L);
    }

    /**
     * 获取当前用户ID，未登录时抛出 401
     */
    public static Long requireCurrentUser() {
        Long userId = CURRENT_USER.get();
        if (userId == null) {
            throw BizException.of(ApiErrorCodes.CODE_UNAUTHORIZED, ApiErrorCodes.AUTH_TOKEN_INVALID);
        }
        return userId;
    }
}
