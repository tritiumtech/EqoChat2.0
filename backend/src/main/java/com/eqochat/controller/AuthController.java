package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.BizException;
import com.eqochat.dto.request.LoginRequest;
import com.eqochat.dto.request.RegisterRequest;
import com.eqochat.dto.request.VerifyCodeRequest;
import com.eqochat.dto.response.LoginResponse;
import com.eqochat.dto.response.UserInfoResponse;
import com.eqochat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 发送验证码
     */
    @PostMapping("/verify-code")
    public ApiResponse<Void> sendVerifyCode(@RequestBody @Valid VerifyCodeRequest request) {
        log.info("发送验证码: phone={}", request.getPhone());
        authService.sendVerifyCode(request.getPhone());
        return ApiResponse.success();
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@RequestBody @Valid RegisterRequest request) {
        log.info("用户注册: phone={}", request.getPhone());
        LoginResponse response = authService.register(request);
        return ApiResponse.success(response);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("用户登录: phone={}", request.getPhone());
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }
    
    /**
     * 刷新token
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        LoginResponse response = authService.refreshToken(token);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser() {
        UserInfoResponse response = authService.getUserInfo(resolveUserId());
        return ApiResponse.success(response);
    }

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw BizException.of(401, "auth.token.invalid");
        }
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
        throw BizException.of(401, "auth.token.invalid");
    }
}
