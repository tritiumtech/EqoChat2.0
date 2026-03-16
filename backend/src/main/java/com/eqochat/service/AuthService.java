package com.eqochat.service;

import com.eqochat.dto.request.LoginRequest;
import com.eqochat.dto.request.RegisterRequest;
import com.eqochat.dto.response.LoginResponse;
import com.eqochat.dto.response.UserInfoResponse;
public interface AuthService {
    
    /**
     * 发送验证码
     */
    void sendVerifyCode(String phone);
    
    /**
     * 用户注册
     */
    LoginResponse register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 刷新token
     */
    LoginResponse refreshToken(String token);
    
    /**
     * 获取用户信息
     */
    UserInfoResponse getUserInfo(Long userId);
}
