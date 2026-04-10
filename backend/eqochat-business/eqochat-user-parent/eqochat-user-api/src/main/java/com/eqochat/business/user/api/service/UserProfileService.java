package com.eqochat.business.user.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.framework.common.AuditMetaObjectHandler;
import com.eqochat.business.user.entity.UserProfile;

import java.util.Optional;

public interface UserProfileService extends IService<UserProfile> {
    
    Optional<UserProfile> findByDid(String did);
    
    Optional<UserProfile> findByPhone(String phone);
    
    Optional<UserProfile> findByEmail(String email);
    
    boolean existsByDid(String did);
    
    boolean existsByPhone(String phone);
    
    UserProfile createUser(UserProfile user);
    
    /**
     * 逻辑删除用户
     */
    default boolean logicDelete(Long id) {
        UserProfile user = getById(id);
        if (user == null) {
            return false;
        }
        user.setDelToken(AuditMetaObjectHandler.generateDelToken());
        return updateById(user);
    }
    
    /**
     * 恢复已删除用户
     */
    default boolean restore(Long id) {
        UserProfile user = getById(id);
        if (user == null) {
            return false;
        }
        user.setDelToken(0L);
        return updateById(user);
    }
}
