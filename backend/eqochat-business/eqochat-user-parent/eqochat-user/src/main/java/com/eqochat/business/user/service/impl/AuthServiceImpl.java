package com.eqochat.business.user.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.framework.common.ApiErrorCodes;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.I18nUtil;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.user.api.dto.request.EmailLoginRequest;
import com.eqochat.business.user.api.dto.request.EmailRegisterRequest;
import com.eqochat.business.user.api.dto.request.LoginRequest;
import com.eqochat.business.user.api.dto.request.RegisterRequest;
import com.eqochat.business.user.api.dto.response.LoginResponse;
import com.eqochat.business.user.api.dto.response.UserInfoResponse;
import com.eqochat.framework.security.JwtTokenUtil;
import com.eqochat.business.user.api.service.AuthService;
import com.eqochat.business.user.api.service.UserProfileService;
import com.eqochat.business.chat.api.session.UserSessionApi;
import com.eqochat.framework.sms.SmsSender;
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
    private final UserSessionApi userSessionApi;
    private final SubjectDirectoryApi subjectDirectoryApi;
    
    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final String VERIFY_EMAIL_CODE_PREFIX = "verify:email:";
    private static final String DID_PREFIX = "did:eqochat:user:";
    /**
     * token 过期时间（秒），与 jwt.expiration(毫秒) 保持一致：5 天
     */
    private static final long TOKEN_EXPIRES_IN_SECONDS = 432000L;
    
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
    public void sendEmailVerifyCode(String email) {
        // 生成6位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 存储到Redis，5分钟过期
        String key = VERIFY_EMAIL_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        log.info("准备发送邮箱验证码: email={}", email);
        // 这里暂时只记录日志，后续可接入真实邮件服务
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
                .locale(I18nUtil.getLocaleTag())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserProfile.UserStatus.ACTIVE)
                .creditScore(50)
                .build();
        
        userProfileService.save(user);
        
        // 删除验证码
        redisTemplate.delete(key);
        
        // 生成token
        String sessionId = userSessionApi.createSession(user.getId());
        String token = jwtTokenUtil.generateToken(user.getId(), user.getDid(), sessionId);
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .expiresIn(TOKEN_EXPIRES_IN_SECONDS)
                .userInfo(convertToUserInfo(user))
                .build();
    }

    @Override
    public LoginResponse emailRegister(EmailRegisterRequest request) {
        // 验证邮箱验证码
        String key = VERIFY_EMAIL_CODE_PREFIX + request.getEmail();
        String code = redisTemplate.opsForValue().get(key);
        if (code == null || code.isBlank()) {
            throw BizException.of("auth.verify_code.expired");
        }
        if (!code.equals(request.getVerifyCode())) {
            throw BizException.of("auth.verify_code.invalid");
        }

        // 检查邮箱是否已注册
        if (userProfileService.findByEmail(request.getEmail()).isPresent()) {
            throw BizException.of("auth.email.exists");
        }

        String did = generateDid();

        UserProfile user = UserProfile.builder()
                .did(did)
                .email(request.getEmail())
                .nickname(request.getNickname())
                .avatarUrl(request.getAvatarUrl())
                .locale(I18nUtil.getLocaleTag())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserProfile.UserStatus.ACTIVE)
                .creditScore(50)
                .build();

        userProfileService.save(user);

        redisTemplate.delete(key);

        String sessionId = userSessionApi.createSession(user.getId());
        String token = jwtTokenUtil.generateToken(user.getId(), user.getDid(), sessionId);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .expiresIn(TOKEN_EXPIRES_IN_SECONDS)
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
        String sessionId = userSessionApi.createSession(user.getId());
        String token = jwtTokenUtil.generateToken(user.getId(), user.getDid(), sessionId);
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .expiresIn(TOKEN_EXPIRES_IN_SECONDS)
                .userInfo(convertToUserInfo(user))
                .build();
    }

    @Override
    public LoginResponse emailLogin(EmailLoginRequest request) {
        var optUser = userProfileService.findByEmail(request.getEmail());
        if (optUser.isEmpty()) {
            throw BizException.of("auth.user.not_found");
        }

        UserProfile user = optUser.get();

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw BizException.of("auth.password.unset");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BizException.of("auth.password.invalid");
        }

        if (user.getStatus() != UserProfile.UserStatus.ACTIVE) {
            throw BizException.of("auth.account.status.invalid");
        }

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userProfileService.updateById(user);

        String sessionId = userSessionApi.createSession(user.getId());
        String token = jwtTokenUtil.generateToken(user.getId(), user.getDid(), sessionId);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .expiresIn(TOKEN_EXPIRES_IN_SECONDS)
                .userInfo(convertToUserInfo(user))
                .build();
    }
    
    @Override
    public LoginResponse refreshToken(String token) {
        if (!jwtTokenUtil.validateToken(token)) {
            throw BizException.of(ApiErrorCodes.CODE_UNAUTHORIZED, ApiErrorCodes.AUTH_TOKEN_INVALID);
        }
        
        Long principalHumanId = jwtTokenUtil.getPrincipalHumanIdFromToken(token);
        UserProfile user = userProfileService.getById(principalHumanId);
        
        if (user == null || (user.getDelToken() != null && user.getDelToken() != 0L)) {
            throw BizException.of("auth.user.not_found");
        }
        
        // 刷新 token 时保持 sessionId 不变
        String oldSessionId = jwtTokenUtil.getSessionIdFromToken(token);
        String sessionId = oldSessionId;
        
        // 如果原 sessionId 无效，说明用户已被挤下线或 session 已过期
        if (sessionId == null || !userSessionApi.validateSession(sessionId)) {
            log.warn("refreshToken: session 已失效，拒绝刷新 token: principalHumanId={}, sessionId={}", principalHumanId, oldSessionId);
            throw BizException.of(ApiErrorCodes.CODE_UNAUTHORIZED, "auth.session.expired");
        }
        
        // 生成带 sessionId 的新 token
        String newToken = jwtTokenUtil.generateToken(user.getId(), user.getDid(), sessionId);
        
        return LoginResponse.builder()
                .token(newToken)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .expiresIn(TOKEN_EXPIRES_IN_SECONDS)
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
        SubjectSummaryResponse subject = resolveHumanSubject(user);
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
                .creditScore(resolveCreditScore(user, subject))
                .points(resolvePoints(subject))
                .lastLoginAt(user.getLastLoginAt())
                .createTime(user.getCreateTime())
                .build();
    }

    private SubjectSummaryResponse resolveHumanSubject(UserProfile user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        try {
            return subjectDirectoryApi.getSubject(SubjectRef.human(user.getId()));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Integer resolveCreditScore(UserProfile user, SubjectSummaryResponse subject) {
        if (subject != null && subject.getCreditScore() != null) {
            return subject.getCreditScore();
        }
        return adaptCreditScore(user != null ? user.getCreditScore() : null);
    }

    private Integer resolvePoints(SubjectSummaryResponse subject) {
        return subject != null && subject.getPoints() != null ? Math.max(0, subject.getPoints()) : 0;
    }

    private Integer adaptCreditScore(Integer score) {
        if (score == null) {
            return 300;
        }
        if (score >= 300 && score <= 850) {
            return score;
        }
        if (score >= 0 && score <= 100) {
            return Math.min(850, Math.max(300, Math.round(300 + (score * 5.5f))));
        }
        return Math.min(850, Math.max(300, score));
    }
}
