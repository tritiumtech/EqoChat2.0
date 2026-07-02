package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.api.dto.request.SendFriendRequestRequest;
import com.eqochat.business.contact.api.dto.response.FriendRequestResponse;
import com.eqochat.business.contact.api.service.FriendRequestService;
import com.eqochat.business.contact.entity.ContactRelationship;
import com.eqochat.business.contact.entity.FriendRequest;
import com.eqochat.business.contact.mapper.ContactRelationshipMapper;
import com.eqochat.business.contact.mapper.FriendRequestMapper;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.framework.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendRequestMapper friendRequestMapper;
    private final ContactRelationshipMapper contactRelationshipMapper;
    private final SubjectDirectoryApi subjectDirectoryApi;
    private final LiabilityPolicyApi liabilityPolicyApi;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendRequestResponse sendRequest(Long principalHumanId, SendFriendRequestRequest request) {
        SubjectRef requester = requireRelationshipSubject(request.getActorSubjectId(), request.getActorSubjectType());
        SubjectRef recipient = requireRelationshipSubject(request.getRecipientSubjectId(), request.getRecipientSubjectType());
        if (requester.equals(recipient)) {
            throw BizException.of("friend_request.self");
        }
        SubjectSummaryResponse requesterSummary = requireActiveSubject(requester);
        SubjectSummaryResponse recipientSummary = requireActiveSubject(recipient);
        requireAuthorizedLiability(principalHumanId, requester);

        if (contactRelationshipMapper.areFriends(
                requester.id(), toFriendType(requester), recipient.id(), toFriendType(recipient))) {
            throw BizException.of("friend_request.already_friends");
        }
        var existing = friendRequestMapper.findPendingRequest(
                requester.id(), requester.type(), recipient.id(), recipient.type());
        if (existing.isPresent()) {
            throw BizException.of("friend_request.pending_exists");
        }

        FriendRequest fr = FriendRequest.builder()
                .requesterId(requester.id())
                .requesterType(requester.type())
                .recipientId(recipient.id())
                .recipientType(recipient.type())
                .requestType(FriendRequest.RequestType.FRIEND)
                .requestMessage(StringUtils.hasText(request.getRequestMessage()) ? request.getRequestMessage() : null)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();
        friendRequestMapper.insert(fr);

        // 发送好友请求通知
        try {
            String requesterName = resolveDisplayName(requesterSummary);
            String message = StringUtils.hasText(request.getRequestMessage())
                    ? request.getRequestMessage()
                    : requesterName + " 请求添加你为好友";
            notificationService.sendNotification(
                    recipient,
                    "FRIEND_REQUEST",
                    "新的好友请求",
                    message,
                    friendRequestPayload(fr),
                    requester
            );
        } catch (Exception e) {
            log.error("发送好友请求通知失败", e);
        }

        return toResponse(fr, requesterSummary, recipientSummary);
    }

    @Override
    @Transactional
    public void accept(Long principalHumanId, Long requestId) {
        FriendRequest fr = friendRequestMapper.selectById(requestId);
        if (fr == null) {
            throw BizException.of("friend_request.not_found");
        }
        SubjectRef requester = requesterRef(fr);
        SubjectRef recipient = recipientRef(fr);
        requireAuthorizedLiability(principalHumanId, recipient);
        if (fr.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw BizException.of("friend_request.already_handled");
        }
        SubjectSummaryResponse requesterSummary = requireActiveSubject(requester);
        SubjectSummaryResponse recipientSummary = requireActiveSubject(recipient);

        LocalDateTime now = LocalDateTime.now();
        fr.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        fr.setRespondedAt(now);
        friendRequestMapper.updateById(fr);

        boolean inserted = false;
        if (!contactRelationshipMapper.areFriends(requester.id(), toFriendType(requester), recipient.id(), toFriendType(recipient))) {
            contactRelationshipMapper.insert(relation(requester, recipient, now));
            inserted = true;
        }
        if (!contactRelationshipMapper.areFriends(recipient.id(), toFriendType(recipient), requester.id(), toFriendType(requester))) {
            contactRelationshipMapper.insert(relation(recipient, requester, now));
            inserted = true;
        }

        if (inserted) {
            // 发送好友请求被接受的通知
            try {
                String recipientName = resolveDisplayName(recipientSummary);
                notificationService.sendNotification(
                        requester,
                        "FRIEND_REQUEST",
                        "好友请求已接受",
                        recipientName + " 接受了你的好友请求",
                        friendRequestPayload(fr),
                        recipient
                );
            } catch (Exception e) {
                log.error("发送好友请求接受通知失败", e);
            }
        }
    }

    @Override
    @Transactional
    public void reject(Long principalHumanId, Long requestId) {
        FriendRequest fr = friendRequestMapper.selectById(requestId);
        if (fr == null) {
            throw BizException.of("friend_request.not_found");
        }
        requireAuthorizedLiability(principalHumanId, recipientRef(fr));
        if (fr.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw BizException.of("friend_request.already_handled");
        }
        fr.setStatus(FriendRequest.RequestStatus.REJECTED);
        fr.setRespondedAt(LocalDateTime.now());
        friendRequestMapper.updateById(fr);
    }

    @Override
    public List<FriendRequestResponse> listReceived(Long principalHumanId, SubjectRef recipient) {
        SubjectRef subject = requireAuthorizedInboxSubject(principalHumanId, recipient);
        List<FriendRequest> list = friendRequestMapper
                .findPendingByRecipientSubject(subject.id(), subject.type())
                .stream()
                .sorted(Comparator.comparing(FriendRequest::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return toResponseList(list, true);
    }

    @Override
    public List<FriendRequestResponse> listSent(Long principalHumanId, SubjectRef requester) {
        SubjectRef subject = requireAuthorizedInboxSubject(principalHumanId, requester);
        List<FriendRequest> list = friendRequestMapper
                .findByRequesterSubject(subject.id(), subject.type())
                .stream()
                .sorted(Comparator.comparing(FriendRequest::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return toResponseList(list, false);
    }

    private List<FriendRequestResponse> toResponseList(List<FriendRequest> list, boolean received) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<SubjectRef> subjects = list.stream()
                .flatMap(fr -> List.of(requesterRef(fr), recipientRef(fr)).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<SubjectRef, SubjectSummaryResponse> subjectMap = subjectDirectoryApi.batchGetSubjects(subjects);

        List<FriendRequestResponse> result = new ArrayList<>();
        for (FriendRequest fr : list) {
            SubjectSummaryResponse requester = subjectMap.get(requesterRef(fr));
            SubjectSummaryResponse recipient = subjectMap.get(recipientRef(fr));
            result.add(toResponse(fr, requester, recipient));
        }
        return result;
    }

    private FriendRequestResponse toResponse(
            FriendRequest fr,
            SubjectSummaryResponse requester,
            SubjectSummaryResponse recipient
    ) {
        String requesterNickname = requester != null ? resolveDisplayName(requester) : null;
        String requesterAvatar = requester != null ? requester.getAvatarUrl() : null;
        String recipientNickname = recipient != null ? resolveDisplayName(recipient) : null;
        String recipientAvatar = recipient != null ? recipient.getAvatarUrl() : null;
        return FriendRequestResponse.builder()
                .id(fr.getId())
                .requesterSubjectId(fr.getRequesterId())
                .requesterSubjectType(requireSubjectType(fr.getRequesterType()))
                .recipientSubjectId(fr.getRecipientId())
                .recipientSubjectType(requireSubjectType(fr.getRecipientType()))
                .requestMessage(fr.getRequestMessage())
                .status(fr.getStatus() != null ? fr.getStatus().name() : null)
                .createTime(fr.getCreateTime())
                .requesterNickname(requesterNickname)
                .requesterAvatarUrl(requesterAvatar)
                .recipientNickname(recipientNickname)
                .recipientAvatarUrl(recipientAvatar)
                .build();
    }

    private SubjectSummaryResponse requireActiveSubject(SubjectRef ref) {
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(ref);
        if (subject == null) {
            throw BizException.of("contact.user.not_found");
        }
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw BizException.of("friend_request.subject.invalid");
        }
        return subject;
    }

    private void requireAuthorizedLiability(Long principalHumanId, SubjectRef actor) {
        LiabilityChain chain = liabilityPolicyApi.resolveLiability(actor);
        Long liableHumanId = chain != null ? chain.liableHumanId() : null;
        if (liableHumanId == null || !Objects.equals(liableHumanId, principalHumanId)) {
            throw BizException.of("friend_request.not_recipient");
        }
    }

    private SubjectRef requireAuthorizedInboxSubject(Long principalHumanId, SubjectRef explicitSubject) {
        SubjectRef subject = requireRelationshipSubject(
                explicitSubject != null ? explicitSubject.id() : null,
                explicitSubject != null ? explicitSubject.type() : null);
        requireAuthorizedLiability(principalHumanId, subject);
        return subject;
    }

    private static ContactRelationship relation(SubjectRef owner, SubjectRef target, LocalDateTime now) {
        return ContactRelationship.builder()
                .userId(owner.id())
                .userType(toFriendType(owner))
                .friendId(target.id())
                .friendType(toFriendType(target))
                .status(ContactRelationship.RelationshipStatus.ACTIVE)
                .addSource("FRIEND_REQUEST")
                .createTime(now)
                .build();
    }

    private static SubjectRef requireRelationshipSubject(Long id, SubjectType type) {
        if (id == null || type == null || type == SubjectType.SYSTEM) {
            throw BizException.of("friend_request.subject.invalid");
        }
        return new SubjectRef(id, type);
    }

    private static SubjectRef requesterRef(FriendRequest fr) {
        return requireRelationshipSubject(fr.getRequesterId(), requireSubjectType(fr.getRequesterType()));
    }

    private static SubjectRef recipientRef(FriendRequest fr) {
        return requireRelationshipSubject(fr.getRecipientId(), requireSubjectType(fr.getRecipientType()));
    }

    private static SubjectType requireSubjectType(SubjectType type) {
        if (type == null) {
            throw BizException.of("friend_request.subject.invalid");
        }
        return type;
    }

    private static ContactRelationship.RelationshipSubjectType toFriendType(SubjectRef ref) {
        if (ref.type() == SubjectType.SYSTEM) {
            throw BizException.of("friend_request.subject.invalid");
        }
        return ref.type() == SubjectType.AGENT
                ? ContactRelationship.RelationshipSubjectType.AGENT
                : ContactRelationship.RelationshipSubjectType.HUMAN;
    }

    private static String resolveDisplayName(SubjectSummaryResponse subject) {
        if (subject == null) return "对方";
        if (StringUtils.hasText(subject.getDisplayName())) return subject.getDisplayName();
        return subject.getId() != null ? subject.getType().name() + ":" + subject.getId() : "对方";
    }

    private static String friendRequestPayload(FriendRequest fr) {
        return "{\"requestId\":" + fr.getId()
                + ",\"requesterSubjectId\":" + fr.getRequesterId()
                + ",\"requesterSubjectType\":\"" + requireSubjectType(fr.getRequesterType()).name()
                + "\",\"recipientSubjectId\":" + fr.getRecipientId()
                + ",\"recipientSubjectType\":\"" + requireSubjectType(fr.getRecipientType()).name()
                + "\"}";
    }
}
