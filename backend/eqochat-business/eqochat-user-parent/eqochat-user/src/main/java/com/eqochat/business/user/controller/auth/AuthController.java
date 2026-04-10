package com.eqochat.business.user.controller.auth;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.user.api.dto.request.*;
import com.eqochat.business.user.api.dto.response.LoginResponse;
import com.eqochat.business.user.api.dto.response.UserInfoResponse;
import com.eqochat.business.user.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 发送邮箱验证码
     */
    @PostMapping("/verify-code/email")
    public ApiResponse<Void> sendEmailVerifyCode(@RequestBody @Valid EmailVerifyCodeRequest request) {
        log.info("发送邮箱验证码: email={}", request.getEmail());
        authService.sendEmailVerifyCode(request.getEmail());
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
     * 邮箱注册
     */
    @PostMapping("/register/email")
    public ApiResponse<LoginResponse> emailRegister(@RequestBody @Valid EmailRegisterRequest request) {
        log.info("用户注册: email={}", request.getEmail());
        LoginResponse response = authService.emailRegister(request);
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
     * 邮箱登录
     */
    @PostMapping("/login/email")
    public ApiResponse<LoginResponse> emailLogin(@RequestBody @Valid EmailLoginRequest request) {
        log.info("用户登录: email={}", request.getEmail());
        LoginResponse response = authService.emailLogin(request);
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
        return ApiResponse.success(authService.getUserInfo(UserContext.requireCurrentUser()));
    }
}
