package com.eqochat.service.impl;

import com.eqochat.common.BizException;
import com.eqochat.domain.entity.UserFriend;
import com.eqochat.domain.entity.UserProfile;
import com.eqochat.dto.response.ContactResponse;
import com.eqochat.mapper.UserFriendMapper;
import com.eqochat.service.ContactService;
import com.eqochat.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {
    
    private final UserFriendMapper userFriendMapper;
    private final UserProfileService userProfileService;
    
    @Override
    public List<ContactResponse> listContacts(Long userId) {
        List<UserFriend> friends = userFriendMapper.findActiveFriendsByUserId(userId);
        if (friends.isEmpty()) {
            return List.of();
        }
        
        List<Long> friendIds = friends.stream()
                .map(UserFriend::getFriendId)
                .collect(Collectors.toList());
        
        List<UserProfile> profiles = userProfileService.listByIds(friendIds);
        
        return profiles.stream()
                .map(profile -> ContactResponse.builder()
                        .id(profile.getId())
                        .nickname(profile.getNickname())
                        .avatarUrl(profile.getAvatarUrl())
                        .status(profile.getStatus() != null ? profile.getStatus().name() : null)
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public ContactResponse addContact(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw BizException.of("contact.self");
        }
        
        UserProfile friendProfile = userProfileService.getById(friendId);
        if (friendProfile == null) {
            throw BizException.of("contact.user.not_found");
        }
        
        boolean areFriends = userFriendMapper.areFriends(userId, friendId);
        if (!areFriends) {
            LocalDateTime now = LocalDateTime.now();
            List<UserFriend> relations = new ArrayList<>();
            
            relations.add(UserFriend.builder()
                    .userId(userId)
                    .friendId(friendId)
                    .friendType(UserFriend.FriendType.HUMAN)
                    .status(UserFriend.FriendStatus.ACTIVE)
                    .addSource("MANUAL")
                    .createTime(now)
                    .build());
            
            relations.add(UserFriend.builder()
                    .userId(friendId)
                    .friendId(userId)
                    .friendType(UserFriend.FriendType.HUMAN)
                    .status(UserFriend.FriendStatus.ACTIVE)
                    .addSource("MANUAL")
                    .createTime(now)
                    .build());
            
            relations.forEach(userFriendMapper::insert);
        }
        
        return ContactResponse.builder()
                .id(friendProfile.getId())
                .nickname(friendProfile.getNickname())
                .avatarUrl(friendProfile.getAvatarUrl())
                .status(friendProfile.getStatus() != null ? friendProfile.getStatus().name() : null)
                .build();
    }
}
