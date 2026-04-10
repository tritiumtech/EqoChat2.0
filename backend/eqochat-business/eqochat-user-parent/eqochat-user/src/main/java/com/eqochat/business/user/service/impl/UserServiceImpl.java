package com.eqochat.business.user.service.impl;

import com.eqochat.framework.common.BizException;
import com.eqochat.business.user.entity.UserFriend;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.user.api.dto.response.UserPublicProfileResponse;
import com.eqochat.business.user.api.dto.response.UserSearchResponse;
import com.eqochat.business.user.mapper.UserFriendMapper;
import com.eqochat.business.user.api.service.UserService;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.eqochat.business.user.api.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserProfileService userProfileService;
    private final UserFriendMapper userFriendMapper;
    private final WorldPostStatsApi worldPostStatsApi;

    @Override
    public UserSearchResponse searchUserByAccount(Long currentUserId, String type, String keyword) {
        UserProfile user = null;
        
        switch (type.toLowerCase()) {
            case "id":
                try {
                    Long userId = Long.parseLong(keyword);
                    user = userProfileService.getById(userId);
                } catch (NumberFormatException e) {
                    throw BizException.of("user.invalid_id");
                }
                break;
            case "phone":
                user = userProfileService.findByPhone(keyword).orElse(null);
                break;
            case "email":
                user = userProfileService.findByEmail(keyword).orElse(null);
                break;
            default:
                throw BizException.of("user.invalid_search_type");
        }
        
        if (user == null) {
            throw BizException.of("user.not_found");
        }
        
        // 检查是否已是好友
        boolean isFriend = userFriendMapper.areFriends(currentUserId, user.getId());
        
        // 获取世界动态数量
        int worldPostCount = Math.toIntExact(worldPostStatsApi.countByAuthorId(user.getId()));
        
        // 脱敏处理
        String maskedPhone = user.getPhone() != null ? maskPhone(user.getPhone()) : null;
        String maskedEmail = user.getEmail() != null ? maskEmail(user.getEmail()) : null;
        
        return UserSearchResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(maskedPhone)
                .email(maskedEmail)
                .worldPostCount(worldPostCount)
                .isFriend(isFriend)
                .friendType(UserFriend.FriendType.HUMAN.name())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .build();
    }

    @Override
    public UserPublicProfileResponse getUserPublicProfile(Long currentUserId, Long userId) {
        UserProfile user = userProfileService.getById(userId);
        if (user == null) {
            throw BizException.of("user.not_found");
        }
        
        // 检查是否已是好友
        boolean isFriend = userFriendMapper.areFriends(currentUserId, userId);
        
        // 获取世界动态数量
        int worldPostCount = Math.toIntExact(worldPostStatsApi.countByAuthorId(userId));
        
        // 如果不是好友，隐藏敏感信息
        if (!isFriend) {
            return UserPublicProfileResponse.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .status(user.getStatus() != null ? user.getStatus().name() : null)
                    .worldPostCount(worldPostCount)
                    .friendType(UserFriend.FriendType.HUMAN.name())
                    .isFriend(false)
                    .tags(List.of())
                    .capabilities(List.of())
                    .build();
        } else {
            // 是好友，返回完整信息
            return UserPublicProfileResponse.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .status(user.getStatus() != null ? user.getStatus().name() : null)
                    .worldPostCount(worldPostCount)
                    .friendType(UserFriend.FriendType.HUMAN.name())
                    .isFriend(true)
                    .tags(List.of())
                    .capabilities(List.of())
                    .build();
        }
    }
    
    /**
     * 脱敏手机号：13812345678 -> 138****5678
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /**
     * 脱敏邮箱：test@example.com -> t**t@example.com
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return email;
        }
        String localPart = parts[0];
        String masked = localPart.charAt(0) + "**" + localPart.charAt(localPart.length() - 1);
        return masked + "@" + parts[1];
    }
}
