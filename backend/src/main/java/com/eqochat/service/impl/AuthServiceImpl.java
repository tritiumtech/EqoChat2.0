package com.eqochat.service.impl;

import com.eqochat.common.AuditMetaObjectHandler;
import com.eqochat.domain.entity.UserProfile;
import com.eqochat.dto.request.LoginRequest;
import com.eqochat.dto.request.RegisterRequest;
import com.eqochat.dto.response.LoginResponse;
import com.eqochat.dto.response.UserInfoResponse;
import com.eqochat.security.JwtTokenUtil;
import com.eqochat.service.AuthService;
import com.eqochat.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    private final ReactiveStringRedisTemplate redisTemplate;
    
    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final String DID_PREFIX = "did:eqochat:user:";
    
    @Override
    public Mono<Void> sendVerifyCode(String phone) {
        // 生成6位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        
        // 存储到Redis，5分钟过期
        String key = VERIFY_CODE_PREFIX + phone;
        return redisTemplate.opsForValue()
                .set(key, code, Duration.ofMinutes(5))
                .doOnSuccess(v -> {
                    log.info("验证码已发送: phone={}, code={}", phone, code);
                    // TODO: 调用短信服务发送验证码
                })
                .then();
    }
    
    @Override
    public Mono<LoginResponse> register(RegisterRequest request) {
        // 验证验证码
        String key = VERIFY_CODE_PREFIX + request.getPhone();
        return redisTemplate.opsForValue().get(key)
                .flatMap(code -> {
                    if (!code.equals(request.getVerifyCode())) {
                        return Mono.error(new RuntimeException("验证码错误"));
                    }
                    
                    // 检查手机号是否已注册
                    if (userProfileService.existsByPhone(request.getPhone())) {
                        return Mono.error(new RuntimeException("手机号已注册"));
                    }
                    
                    // 生成DID
                    String did = generateDid();
                    
                    // 创建用户
                    UserProfile user = UserProfile.builder()
                            .did(did)
                            .phone(request.getPhone())
                            .nickname(request.getNickname())
                            .avatarUrl(request.getAvatarUrl())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .status(UserProfile.UserStatus.ACTIVE)
                            .creditScore(50)
                            .build();
                    
                    userProfileService.save(user);
                    
                    // 删除验证码
                    return redisTemplate.delete(key)
                            .thenReturn(user);
                })
                .map(user -> {
                    // 生成token
                    String token = jwtTokenUtil.generateToken(user.getId(), user.getDid());
                    
                    return LoginResponse.builder()
                            .token(token)
                            .tokenType("Bearer")
                            .expiresIn(86400L)
                            .userInfo(convertToUserInfo(user))
                            .build();
                });
    }
    
    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        return Mono.fromCallable(() -> userProfileService.findByPhone(request.getPhone()))
                .flatMap(optUser -> {
                    if (optUser.isEmpty()) {
                        return Mono.error(new RuntimeException("用户不存在"));
                    }
                    
                    UserProfile user = optUser.get();
                    
                    // 检查密码
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new RuntimeException("密码错误"));
                    }
                    
                    // 检查状态
                    if (user.getStatus() != UserProfile.UserStatus.ACTIVE) {
                        return Mono.error(new RuntimeException("账号状态异常"));
                    }
                    
                    // 更新登录时间
                    user.setLastLoginAt(java.time.LocalDateTime.now());
                    userProfileService.updateById(user);
                    
                    // 生成token
                    String token = jwtTokenUtil.generateToken(user.getId(), user.getDid());
                    
                    return Mono.just(LoginResponse.builder()
                            .token(token)
                            .tokenType("Bearer")
                            .expiresIn(86400L)
                            .userInfo(convertToUserInfo(user))
                            .build());
                });
    }
    
    @Override
    public Mono<LoginResponse> refreshToken(String token) {
        return Mono.fromCallable(() -> {
            if (!jwtTokenUtil.validateToken(token)) {
                throw new RuntimeException("Token无效");
            }
            
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            UserProfile user = userProfileService.getById(userId);
            
            if (user == null || user.isDeleted()) {
                throw new RuntimeException("用户不存在");
            }
            
            String newToken = jwtTokenUtil.refreshToken(token);
            
            return LoginResponse.builder()
                    .token(newToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .userInfo(convertToUserInfo(user))
                    .build();
        });
    }
    
    @Override
    public Mono<UserInfoResponse> getUserInfo(Long userId) {
        return Mono.fromCallable(() -> {
            UserProfile user = userProfileService.getById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            return convertToUserInfo(user);
        });
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
