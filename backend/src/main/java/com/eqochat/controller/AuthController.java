package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.dto.request.LoginRequest;
import com.eqochat.dto.request.RegisterRequest;
import com.eqochat.dto.response.LoginResponse;
import com.eqochat.dto.response.UserInfoResponse;
import com.eqochat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<ApiResponse<Void>> sendVerifyCode(@RequestParam String phone) {
        log.info("发送验证码: phone={}", phone);
        return authService.sendVerifyCode(phone)
                .then(Mono.just(ApiResponse.success()));
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Mono<ApiResponse<LoginResponse>> register(@RequestBody @Valid RegisterRequest request) {
        log.info("用户注册: phone={}", request.getPhone());
        return authService.register(request)
                .map(ApiResponse::success);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Mono<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        log.info("用户登录: phone={}", request.getPhone());
        return authService.login(request)
                .map(ApiResponse::success);
    }
    
    /**
     * 刷新token
     */
    @PostMapping("/refresh")
    public Mono<ApiResponse<LoginResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        return authService.refreshToken(token)
                .map(ApiResponse::success);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Mono<ApiResponse<UserInfoResponse>> getCurrentUser(@RequestAttribute("userId") Long userId) {
        return authService.getUserInfo(userId)
                .map(ApiResponse::success);
    }
}
