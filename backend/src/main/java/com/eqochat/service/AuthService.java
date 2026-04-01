package com.eqochat.service;

import com.eqochat.dto.request.EmailLoginRequest;
import com.eqochat.dto.request.EmailRegisterRequest;
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
     * 发送邮箱验证码
     */
    void sendEmailVerifyCode(String email);
    
    /**
     * 用户注册
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 邮箱注册
     */
    LoginResponse emailRegister(EmailRegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 邮箱登录
     */
    LoginResponse emailLogin(EmailLoginRequest request);
    
    /**
     * 刷新token
     */
    LoginResponse refreshToken(String token);
    
    /**
     * 获取用户信息
     */
    UserInfoResponse getUserInfo(Long userId);
}
