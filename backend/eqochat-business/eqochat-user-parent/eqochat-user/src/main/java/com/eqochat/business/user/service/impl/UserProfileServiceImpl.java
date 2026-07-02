package com.eqochat.business.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.business.actor.api.service.SubjectRegistrySyncApi;
import com.eqochat.framework.common.AuditMetaObjectHandler;
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
        syncHuman(user);
        return user;
    }

    @Override
    public boolean save(UserProfile entity) {
        boolean saved = super.save(entity);
        if (saved) {
            syncHuman(entity);
        }
        return saved;
    }

    @Override
    public boolean updateById(UserProfile entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            syncHuman(entity);
        }
        return updated;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean removed = super.removeById(id);
        if (removed && id instanceof Long humanId) {
            retireHuman(humanId);
        }
        return removed;
    }

    @Override
    public boolean logicDelete(Long id) {
        UserProfile user = getById(id);
        if (user == null) {
            return false;
        }
        user.setDelToken(AuditMetaObjectHandler.generateDelToken());
        boolean updated = super.updateById(user);
        if (updated) {
            retireHuman(id);
        }
        return updated;
    }

    @Override
    public boolean restore(Long id) {
        UserProfile user = getById(id);
        if (user == null) {
            return false;
        }
        user.setDelToken(0L);
        boolean updated = super.updateById(user);
        if (updated) {
            syncHuman(user);
        }
        return updated;
    }

    private final SubjectRegistrySyncApi subjectRegistrySyncApi;

    private void syncHuman(UserProfile user) {
        try {
            if (user != null && user.getId() != null) {
                subjectRegistrySyncApi.syncHuman(user.getId());
            }
        } catch (RuntimeException ignored) {
            // Registry sync is best-effort; user_profile remains the source of truth.
        }
    }

    private void retireHuman(Long humanId) {
        try {
            subjectRegistrySyncApi.retireHuman(humanId);
        } catch (RuntimeException ignored) {
            // Registry sync is best-effort; user_profile remains the source of truth.
        }
    }
}
