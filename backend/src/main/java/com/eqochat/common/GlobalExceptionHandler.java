package com.eqochat.common;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException ex, HttpServletRequest request) {
        log.error("业务异常 [{} {}] code={}, key={}",
                request.getMethod(), request.getRequestURI(), ex.getCode(), ex.getMessageKey(), ex);
        return ApiResponse.error(ex.getCode(), ex.getMessageKey());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("参数校验异常 [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex, HttpServletRequest request) {
        log.error("参数绑定异常 [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        log.error("运行时异常 [{} {}]: {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getClass().getName(), message, ex);
        if (message == null || message.isBlank()) {
            return ApiResponse.error("error.system");
        }
        return ApiResponse.error(message);
    }
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        return ApiResponse.error("error.system");
    }
}
