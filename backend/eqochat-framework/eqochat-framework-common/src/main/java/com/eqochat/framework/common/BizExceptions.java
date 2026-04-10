package com.eqochat.framework.common;

/**
 * 业务异常统一抛出入口
 */
public final class BizExceptions {
    
    private BizExceptions() {
    }
    
    public static void throwBiz(String messageKey) {
        throw BizException.of(messageKey);
    }
    
    public static void throwBiz(int code, String messageKey) {
        throw BizException.of(code, messageKey);
    }
}
