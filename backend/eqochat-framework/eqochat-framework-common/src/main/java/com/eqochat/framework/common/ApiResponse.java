package com.eqochat.framework.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.eqochat.framework.common.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private Integer code;
    private String message;
    private String errorCode;
    private T data;
    private Long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(I18nUtil.getOrDefault("common.success", "success"))
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        String safe = (message == null || message.isBlank()) ? "error.system" : message;
        boolean looksLikeMessageKey = safe.matches("[a-zA-Z0-9_.-]+");
        String resolvedMessage = looksLikeMessageKey
                ? I18nUtil.getOrDefault(safe, safe)
                : safe;
        return ApiResponse.<T>builder()
                .code(code)
                .message(resolvedMessage)
                .errorCode(looksLikeMessageKey ? safe : null)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }
}
