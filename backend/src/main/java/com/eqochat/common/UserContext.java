package com.eqochat.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 当前用户上下文
 * 使用ThreadLocal存储当前登录用户ID
 */
@Slf4j
public class UserContext {
    
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    
    /**
     * 设置当前用户
     */
    public static void setCurrentUser(Long userId) {
        CURRENT_USER.set(userId);
        log.debug("设置当前用户: {}", userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUser() {
        Long userId = CURRENT_USER.get();
        log.debug("获取当前用户: {}", userId);
        return userId;
    }
    
    /**
     * 清除当前用户
     */
    public static void clear() {
        CURRENT_USER.remove();
        log.debug("清除当前用户上下文");
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
}
