package com.eqochat.business.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.user.mapper.UserProfileMapper;
import com.eqochat.business.user.api.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> 
        implements UserProfileService {
    
    @Override
    public Optional<UserProfile> findByDid(String did) {
        return baseMapper.findByDid(did);
    }
    
    @Override
    public Optional<UserProfile> findByPhone(String phone) {
        return baseMapper.findByPhone(phone);
    }
    
    @Override
    public Optional<UserProfile> findByEmail(String email) {
        return baseMapper.findByEmail(email);
    }
    
    @Override
    public boolean existsByDid(String did) {
        return baseMapper.existsByDid(did);
    }
    
    @Override
    public boolean existsByPhone(String phone) {
        return baseMapper.existsByPhone(phone);
    }
    
    @Override
    public UserProfile createUser(UserProfile user) {
        // 设置默认值
        if (user.getCreditScore() == null) {
            user.setCreditScore(50);
        }
        if (user.getStatus() == null) {
            user.setStatus(UserProfile.UserStatus.ACTIVE);
        }
        
        baseMapper.insert(user);
        return user;
    }
}
