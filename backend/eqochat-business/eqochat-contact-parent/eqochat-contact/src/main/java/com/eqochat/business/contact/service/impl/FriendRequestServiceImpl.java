package com.eqochat.business.contact.service.impl;

import com.eqochat.framework.common.BizException;
import com.eqochat.business.contact.entity.FriendRequest;
import com.eqochat.business.user.entity.UserFriend;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.contact.api.dto.request.SendFriendRequestRequest;
import com.eqochat.business.contact.api.dto.response.FriendRequestResponse;
import com.eqochat.business.contact.mapper.FriendRequestMapper;
import com.eqochat.business.user.mapper.UserFriendMapper;
import com.eqochat.business.contact.api.service.FriendRequestService;
import com.eqochat.business.user.api.service.UserProfileService;
import com.eqochat.business.notification.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendRequestMapper friendRequestMapper;
    private final UserFriendMapper userFriendMapper;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendRequestResponse sendRequest(Long userId, SendFriendRequestRequest request) {
        Long friendId = request.getFriendId();
        if (userId.equals(friendId)) {
            throw BizException.of("friend_request.self");
        }
        UserProfile target = userProfileService.getById(friendId);
        if (target == null) {
            throw BizException.of("contact.user.not_found");
        }
        if (userFriendMapper.areFriends(userId, friendId)) {
            throw BizException.of("friend_request.already_friends");
        }
        var existing = friendRequestMapper.findPendingRequest(userId, friendId);
        if (existing.isPresent()) {
            throw BizException.of("friend_request.pending_exists");
        }

        UserProfile self = userProfileService.getById(userId);

        FriendRequest fr = FriendRequest.builder()
                .requesterId(userId)
                .recipientId(friendId)
                .requestType(FriendRequest.RequestType.FRIEND)
                .requestMessage(StringUtils.hasText(request.getRequestMessage()) ? request.getRequestMessage() : null)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();
        friendRequestMapper.insert(fr);

        // 发送好友请求通知
        try {
            String requesterName = resolveDisplayName(self);
            String message = StringUtils.hasText(request.getRequestMessage())
                    ? request.getRequestMessage()
                    : requesterName + " 请求添加你为好友";
            notificationService.sendNotification(
                    friendId,
                    "FRIEND_REQUEST",
                    "新的好友请求",
                    message,
                    "{\"requestId\":" + fr.getId() + ",\"requesterId\":" + userId + "}",
                    userId
            );
        } catch (Exception e) {
            log.error("发送好友请求通知失败", e);
        }

        return toResponse(fr, self, target);
    }

    @Override
    @Transactional
    public void accept(Long userId, Long requestId) {
        FriendRequest fr = friendRequestMapper.selectById(requestId);
        if (fr == null) {
            throw BizException.of("friend_request.not_found");
        }
        if (!fr.getRecipientId().equals(userId)) {
            throw BizException.of("friend_request.not_recipient");
        }
        if (fr.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw BizException.of("friend_request.already_handled");
        }

        LocalDateTime now = LocalDateTime.now();
        fr.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        fr.setRespondedAt(now);
        friendRequestMapper.updateById(fr);

        Long requesterId = fr.getRequesterId();
        Long recipientId = fr.getRecipientId();
        if (!userFriendMapper.areFriends(requesterId, recipientId)) {
            List<UserFriend> relations = List.of(
                    UserFriend.builder()
                            .userId(requesterId)
                            .friendId(recipientId)
                            .friendType(UserFriend.FriendType.HUMAN)
                            .status(UserFriend.FriendStatus.ACTIVE)
                            .addSource("FRIEND_REQUEST")
                            .createTime(now)
                            .build(),
                    UserFriend.builder()
                            .userId(recipientId)
                            .friendId(requesterId)
                            .friendType(UserFriend.FriendType.HUMAN)
                            .status(UserFriend.FriendStatus.ACTIVE)
                            .addSource("FRIEND_REQUEST")
                            .createTime(now)
                            .build()
            );
            relations.forEach(userFriendMapper::insert);

            // 发送好友请求被接受的通知
            try {
                UserProfile recipient = userProfileService.getById(recipientId);
                String recipientName = recipient != null ? resolveDisplayName(recipient) : "对方";
                notificationService.sendNotification(
                        requesterId,
                        "FRIEND_REQUEST",
                        "好友请求已接受",
                        recipientName + " 接受了你的好友请求",
                        "{\"requestId\":" + requestId + ",\"recipientId\":" + recipientId + "}",
                        recipientId
                );
            } catch (Exception e) {
                log.error("发送好友请求接受通知失败", e);
            }
        }
    }

    @Override
    @Transactional
    public void reject(Long userId, Long requestId) {
        FriendRequest fr = friendRequestMapper.selectById(requestId);
        if (fr == null) {
            throw BizException.of("friend_request.not_found");
        }
        if (!fr.getRecipientId().equals(userId)) {
            throw BizException.of("friend_request.not_recipient");
        }
        if (fr.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw BizException.of("friend_request.already_handled");
        }
        fr.setStatus(FriendRequest.RequestStatus.REJECTED);
        fr.setRespondedAt(LocalDateTime.now());
        friendRequestMapper.updateById(fr);
    }

    @Override
    public List<FriendRequestResponse> listReceived(Long userId) {
        List<FriendRequest> list = friendRequestMapper.findPendingByRecipientId(userId);
        return toResponseList(list, true);
    }

    @Override
    public List<FriendRequestResponse> listSent(Long userId) {
        List<FriendRequest> list = friendRequestMapper.findByRequesterId(userId);
        return toResponseList(list, false);
    }

    private List<FriendRequestResponse> toResponseList(List<FriendRequest> list, boolean received) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = list.stream()
                .flatMap(fr -> List.of(fr.getRequesterId(), fr.getRecipientId()).stream())
                .collect(Collectors.toSet());
        Map<Long, UserProfile> profileMap = userProfileService.listByIds(new ArrayList<>(userIds))
                .stream()
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        List<FriendRequestResponse> result = new ArrayList<>();
        for (FriendRequest fr : list) {
            UserProfile requester = profileMap.get(fr.getRequesterId());
            UserProfile recipient = profileMap.get(fr.getRecipientId());
            result.add(toResponse(fr, requester, recipient));
        }
        return result;
    }

    private FriendRequestResponse toResponse(FriendRequest fr, UserProfile requester, UserProfile recipient) {
        String requesterNickname = requester != null ? resolveDisplayName(requester) : null;
        String requesterAvatar = requester != null ? requester.getAvatarUrl() : null;
        String recipientNickname = recipient != null ? resolveDisplayName(recipient) : null;
        String recipientAvatar = recipient != null ? recipient.getAvatarUrl() : null;
        return FriendRequestResponse.builder()
                .id(fr.getId())
                .requesterId(fr.getRequesterId())
                .recipientId(fr.getRecipientId())
                .requestMessage(fr.getRequestMessage())
                .status(fr.getStatus() != null ? fr.getStatus().name() : null)
                .createTime(fr.getCreateTime())
                .requesterNickname(requesterNickname)
                .requesterAvatarUrl(requesterAvatar)
                .recipientNickname(recipientNickname)
                .recipientAvatarUrl(recipientAvatar)
                .build();
    }

    private String resolveDisplayName(UserProfile u) {
        if (u == null) return null;
        if (StringUtils.hasText(u.getNickname())) return u.getNickname();
        if (StringUtils.hasText(u.getPhone())) return u.getPhone();
        if (StringUtils.hasText(u.getEmail())) return u.getEmail();
        return u.getId() != null ? "用户" + u.getId() : null;
    }
}
