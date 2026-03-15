package com.eqochat.service;

import com.eqochat.dto.request.LoginRequest;
import com.eqochat.dto.request.RegisterRequest;
import com.eqochat.dto.response.LoginResponse;
import com.eqochat.dto.response.UserInfoResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
    
    /**
     * 发送验证码
     */
    Mono<Void> sendVerifyCode(String phone);
    
    /**
     * 用户注册
     */
    Mono<LoginResponse> register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    Mono<LoginResponse> login(LoginRequest request);
    
    /**
     * 刷新token
     */
    Mono<LoginResponse> refreshToken(String token);
    
    /**
     * 获取用户信息
     */
    Mono<UserInfoResponse> getUserInfo(Long userId);
}
