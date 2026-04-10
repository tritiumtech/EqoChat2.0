package com.eqochat.business.user.api.service;

import com.eqochat.business.user.api.dto.response.UserPublicProfileResponse;
import com.eqochat.business.user.api.dto.response.UserSearchResponse;

/**
 * 用户服务
 */
public interface UserService {
    
    /**
     * 通过账号搜索用户（精准匹配）
     * @param currentUserId 当前用户 ID
     * @param type 搜索类型：id, phone, email
     * @param keyword 搜索关键词
     * @return 用户搜索结果
     */
    UserSearchResponse searchUserByAccount(Long currentUserId, String type, String keyword);
    
    /**
     * 获取用户公开资料（非好友视角）
     * @param currentUserId 当前用户 ID
     * @param userId 目标用户 ID
     * @return 用户公开资料
     */
    UserPublicProfileResponse getUserPublicProfile(Long currentUserId, Long userId);
}
