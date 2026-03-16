package com.eqochat.service.impl;

import com.eqochat.common.BizException;
import com.eqochat.domain.entity.UserProfile;
import com.eqochat.dto.request.LoginRequest;
import com.eqochat.dto.request.RegisterRequest;
import com.eqochat.dto.response.LoginResponse;
import com.eqochat.dto.response.UserInfoResponse;
import com.eqochat.security.JwtTokenUtil;
import com.eqochat.service.AuthService;
import com.eqochat.service.UserProfileService;
import com.eqochat.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final UserProfileService userProfileService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;
    
    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final String DID_PREFIX = "did:eqochat:user:";
    
    @Override
    public void sendVerifyCode(String phone) {
        // 生成6位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        
        // 存储到Redis，5分钟过期
        String key = VERIFY_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        log.info("准备发送验证码: phone={}", phone);
        smsSender.sendVerificationCode(phone, code);
    }
    
    @Override
    public LoginResponse register(RegisterRequest request) {
        // 验证验证码
        String key = VERIFY_CODE_PREFIX + request.getPhone();
        String code = redisTemplate.opsForValue().get(key);
        if (code == null || code.isBlank()) {
            throw BizException.of("auth.verify_code.expired");
        }
        if (!code.equals(request.getVerifyCode())) {
            throw BizException.of("auth.verify_code.invalid");
        }
        
        // 检查手机号是否已注册
        if (userProfileService.existsByPhone(request.getPhone())) {
            throw BizException.of("auth.phone.exists");
        }
        
        // 生成DID
        String did = generateDid();
        
        // 创建用户
        UserProfile user = UserProfile.builder()
                .did(did)
                .phone(request.getPhone())
                .nickname(request.getNickname())
                .avatarUrl(request.getAvatarUrl())
                .locale(com.eqochat.common.I18nUtil.getLocaleTag())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserProfile.UserStatus.ACTIVE)
                .creditScore(50)
                .build();
        
        userProfileService.save(user);
        
        // 删除验证码
        redisTemplate.delete(key);
        
        // 生成token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getDid());
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userInfo(convertToUserInfo(user))
                .build();
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        var optUser = userProfileService.findByPhone(request.getPhone());
        if (optUser.isEmpty()) {
            throw BizException.of("auth.user.not_found");
        }
        
        UserProfile user = optUser.get();

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw BizException.of("auth.password.unset");
        }
        
        // 检查密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BizException.of("auth.password.invalid");
        }
        
        // 检查状态
        if (user.getStatus() != UserProfile.UserStatus.ACTIVE) {
            throw BizException.of("auth.account.status.invalid");
        }
        
        // 更新登录时间
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userProfileService.updateById(user);
        
        // 生成token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getDid());
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userInfo(convertToUserInfo(user))
                .build();
    }
    
    @Override
    public LoginResponse refreshToken(String token) {
        if (!jwtTokenUtil.validateToken(token)) {
            throw BizException.of("auth.token.invalid");
        }
        
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        UserProfile user = userProfileService.getById(userId);
        
        if (user == null || (user.getDelToken() != null && user.getDelToken() != 0L)) {
            throw BizException.of("auth.user.not_found");
        }
        
        String newToken = jwtTokenUtil.refreshToken(token);
        
        return LoginResponse.builder()
                .token(newToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userInfo(convertToUserInfo(user))
                .build();
    }
    
    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        UserProfile user = userProfileService.getById(userId);
        if (user == null) {
            throw BizException.of("auth.user.not_found");
        }
        return convertToUserInfo(user);
    }
    
    /**
     * 生成DID
     */
    private String generateDid() {
        return DID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 转换为UserInfoResponse
     */
    private UserInfoResponse convertToUserInfo(UserProfile user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .did(user.getDid())
                .phone(user.getPhone())
                .email(user.getEmail())
                .locale(user.getLocale())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .status(user.getStatus().name())
                .creditScore(user.getCreditScore())
                .lastLoginAt(user.getLastLoginAt())
                .createTime(user.getCreateTime())
                .build();
    }
}
