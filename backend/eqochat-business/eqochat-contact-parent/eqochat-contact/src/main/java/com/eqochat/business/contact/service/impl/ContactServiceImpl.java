package com.eqochat.business.contact.service.impl;

import com.eqochat.framework.common.BizException;
import com.eqochat.business.agent.entity.AgentProfile;
import com.eqochat.business.contact.entity.UserContactTag;
import com.eqochat.business.user.entity.UserFriend;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.api.dto.response.ContactResponse;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.contact.mapper.UserContactTagMapper;
import com.eqochat.business.user.mapper.UserFriendMapper;
import com.eqochat.business.contact.api.service.ContactService;
import com.eqochat.business.user.api.service.UserProfileService;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {
    
    private final UserFriendMapper userFriendMapper;
    private final UserContactTagMapper userContactTagMapper;
    private final UserProfileService userProfileService;
    private final WorldPostStatsApi worldPostStatsApi;
    private final AgentProfileMapper agentProfileMapper;
    private final ObjectMapper objectMapper;
    
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
        var tagsByFriendId = friendIds.stream()
                .collect(Collectors.toMap(id -> id, id -> userContactTagMapper.selectActiveTagNames(userId, id)));
        
        return profiles.stream()
                .map(profile -> ContactResponse.builder()
                        .id(profile.getId())
                        .nickname(profile.getNickname())
                        .avatarUrl(profile.getAvatarUrl())
                        .status(profile.getStatus() != null ? profile.getStatus().name() : null)
                        .tags(tagsByFriendId.getOrDefault(profile.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ContactDetailResponse getContactDetail(Long userId, Long contactId) {
        if (contactId == null) {
            throw BizException.of("contact.user.not_found");
        }
        if (!userFriendMapper.areFriends(userId, contactId)) {
            throw BizException.of("contact.not_friend");
        }
        UserFriend uf = userFriendMapper.findByUserAndFriend(userId, contactId)
                .orElseThrow(() -> BizException.of("contact.not_friend"));

        UserProfile up = userProfileService.getById(contactId);
        AgentProfile ap = agentProfileMapper.selectById(contactId);

        String nickname;
        String avatarUrl;
        String bio;
        String statusStr;

        if (up != null) {
            nickname = up.getNickname();
            avatarUrl = up.getAvatarUrl();
            bio = up.getBio();
            statusStr = up.getStatus() != null ? up.getStatus().name() : null;
        } else if (ap != null) {
            nickname = ap.getName();
            avatarUrl = ap.getAvatarUrl();
            bio = ap.getDescription();
            statusStr = ap.getStatus() != null ? ap.getStatus().name() : null;
        } else {
            throw BizException.of("contact.user.not_found");
        }

        List<String> tags = userContactTagMapper.selectActiveTagNames(userId, contactId);
        int worldPostCount = (int) Math.min(Integer.MAX_VALUE, worldPostStatsApi.countByAuthorId(contactId));

        List<String> capabilities = List.of();
        if (uf.getFriendType() == UserFriend.FriendType.AGENT && ap != null) {
            capabilities = parseCapabilityTags(ap.getCapabilityTags());
        }

        return ContactDetailResponse.builder()
                .id(contactId)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .status(statusStr)
                .tags(tags != null ? tags : List.of())
                .bio(bio)
                .worldPostCount(worldPostCount)
                .friendType(uf.getFriendType().name())
                .capabilities(capabilities)
                .build();
    }

    private List<String> parseCapabilityTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String t = raw.trim();
        try {
            List<String> list = objectMapper.readValue(t, new TypeReference<List<String>>() {
            });
            return list.stream().filter(StringUtils::hasText).map(String::trim).toList();
        } catch (Exception ignored) {
            return Arrays.stream(t.split("[,，;；]"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
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
                .tags(List.of())
                .build();
    }

    @Override
    public List<String> updateContactTags(Long userId, Long friendId, List<String> tags) {
        if (friendId == null) {
            throw BizException.of("contact.user.not_found");
        }
        boolean areFriends = userFriendMapper.areFriends(userId, friendId);
        if (!areFriends) {
            throw BizException.of("contact.not_friend");
        }
        List<String> normalized = normalizeTags(tags);
        userContactTagMapper.hardDeleteAll(userId, friendId);
        for (String tag : normalized) {
            userContactTagMapper.insert(UserContactTag.builder()
                    .userId(userId)
                    .friendId(friendId)
                    .tagName(tag)
                    .delToken("0")
                    .build());
        }
        return normalized;
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (String raw : tags) {
            String n = normalizeTag(raw);
            if (n == null) continue;
            String key = n.toLowerCase();
            if (seen.contains(key)) continue;
            seen.add(key);
            out.add(n);
            if (out.size() >= 20) break;
        }
        return out;
    }

    private static String normalizeTag(String raw) {
        if (raw == null) return null;
        String t = raw.trim().replaceAll("\\s+", " ");
        if (t.isBlank()) return null;
        if (t.length() > 24) return t.substring(0, 24);
        return t;
    }
}
