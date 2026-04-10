package com.eqochat.framework.common;

import lombok.Getter;

/**
 * 业务异常统一类型
 */
@Getter
public class BizException extends RuntimeException {
    
    private final int code;
    private final String messageKey;
    
    public BizException(int code, String messageKey) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
    }
    
    public BizException(String messageKey) {
        this(400, messageKey);
    }

    public static BizException of(String messageKey) {
        return new BizException(messageKey);
    }
    
    public static BizException of(int code, String messageKey) {
        return new BizException(code, messageKey);
    }
}
