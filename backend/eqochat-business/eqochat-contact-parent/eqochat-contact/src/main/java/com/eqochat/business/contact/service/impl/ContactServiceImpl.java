package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.api.dto.response.ContactResponse;
import com.eqochat.business.contact.api.service.ContactService;
import com.eqochat.business.contact.api.service.SubjectRelationshipApi;
import com.eqochat.business.contact.entity.ContactRelationship;
import com.eqochat.business.contact.entity.ContactTag;
import com.eqochat.business.contact.mapper.ContactRelationshipMapper;
import com.eqochat.business.contact.mapper.ContactTagMapper;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.eqochat.framework.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService, SubjectRelationshipApi {

    private final ContactRelationshipMapper contactRelationshipMapper;
    private final ContactTagMapper contactTagMapper;
    private final WorldPostStatsApi worldPostStatsApi;
    private final SubjectDirectoryApi subjectDirectoryApi;
    private final LiabilityPolicyApi liabilityPolicyApi;

    @Override
    public List<ContactResponse> listContacts(Long principalHumanId, SubjectRef owner) {
        SubjectRef ownerRef = requireRelationshipSubject(owner);
        requireAuthorizedLiability(principalHumanId, ownerRef, "contact.access.denied");
        ContactRelationship.RelationshipSubjectType ownerType = toFriendType(ownerRef);

        List<ContactRelationship> friends = contactRelationshipMapper.findActiveFriendsByOwner(ownerRef.id(), ownerType);
        if (friends.isEmpty()) {
            return List.of();
        }

        Set<SubjectRef> targets = friends.stream()
                .filter(friend -> friend.getFriendId() != null)
                .map(friend -> toSubjectRef(friend.getFriendId(), requireFriendType(friend.getFriendType())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<SubjectRef, SubjectSummaryResponse> subjects = subjectDirectoryApi.batchGetSubjects(targets);

        return friends.stream()
                .map(friend -> toContactResponse(ownerRef, friend, subjects))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public ContactDetailResponse getContactDetail(Long principalHumanId, SubjectRef owner, SubjectRef target) {
        SubjectRef ownerRef = requireRelationshipSubject(owner);
        SubjectRef targetRef = requireRelationshipSubject(target);
        requireAuthorizedLiability(principalHumanId, ownerRef, "contact.access.denied");
        ContactRelationship relation = requireRelation(ownerRef, targetRef);
        SubjectSummaryResponse subject = requireSubject(targetRef);
        List<String> tags = selectTags(ownerRef, targetRef);
        int worldPostCount = (int) Math.min(Integer.MAX_VALUE,
                worldPostStatsApi.countByAuthor(targetRef.id(), targetRef.type().name()));

        return ContactDetailResponse.builder()
                .ownerSubjectId(ownerRef.id())
                .ownerSubjectType(ownerRef.type())
                .targetSubjectId(targetRef.id())
                .targetSubjectType(targetRef.type())
                .nickname(displayName(subject))
                .avatarUrl(subject.getAvatarUrl())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .tags(tags)
                .bio(subject.getBio())
                .worldPostCount(worldPostCount)
                .capabilities(targetRef.type() == SubjectType.AGENT && subject.getCapabilityTags() != null
                        ? subject.getCapabilityTags()
                        : List.of())
                .build();
    }

    @Override
    public ContactResponse addContact(Long principalHumanId, SubjectRef owner, SubjectRef target) {
        SubjectRef ownerRef = requireRelationshipSubject(owner);
        SubjectRef targetRef = requireRelationshipSubject(target);
        requireAuthorizedLiability(principalHumanId, ownerRef, "contact.access.denied");
        if (ownerRef.equals(targetRef)) {
            throw BizException.of("contact.self");
        }
        SubjectSummaryResponse targetSummary = requireSubject(targetRef);

        if (!areFriends(ownerRef, targetRef)) {
            LocalDateTime now = LocalDateTime.now();
            contactRelationshipMapper.insert(relation(ownerRef, targetRef, "MANUAL", now));
            contactRelationshipMapper.insert(relation(targetRef, ownerRef, "MANUAL", now));
        }

        return toContactResponse(ownerRef, relation(ownerRef, targetRef, "MANUAL", LocalDateTime.now()),
                Map.of(targetRef, targetSummary));
    }

    @Override
    public List<String> updateContactTags(Long principalHumanId, SubjectRef owner, SubjectRef target, List<String> tags) {
        SubjectRef ownerRef = requireRelationshipSubject(owner);
        SubjectRef targetRef = requireRelationshipSubject(target);
        requireAuthorizedLiability(principalHumanId, ownerRef, "contact.access.denied");
        requireRelation(ownerRef, targetRef);

        List<String> normalized = normalizeTags(tags);
        ContactRelationship.RelationshipSubjectType ownerType = toFriendType(ownerRef);
        ContactRelationship.RelationshipSubjectType targetType = toFriendType(targetRef);
        contactTagMapper.hardDeleteAll(ownerRef.id(), ownerType, targetRef.id(), targetType);
        for (String tag : normalized) {
            contactTagMapper.insert(ContactTag.builder()
                    .userId(ownerRef.id())
                    .userType(ownerType)
                    .friendId(targetRef.id())
                    .friendType(targetType)
                    .tagName(tag)
                    .delToken("0")
                    .build());
        }
        return normalized;
    }

    private ContactResponse toContactResponse(
            SubjectRef owner,
            ContactRelationship friend,
            Map<SubjectRef, SubjectSummaryResponse> subjects
    ) {
        if (friend == null || friend.getFriendId() == null) {
            return null;
        }
        SubjectRef target = toSubjectRef(friend.getFriendId(), requireFriendType(friend.getFriendType()));
        SubjectSummaryResponse subject = subjects.get(target);
        if (subject == null) {
            return null;
        }
        return ContactResponse.builder()
                .ownerSubjectId(owner.id())
                .ownerSubjectType(owner.type())
                .targetSubjectId(target.id())
                .targetSubjectType(target.type())
                .nickname(displayName(subject))
                .avatarUrl(subject.getAvatarUrl())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .tags(selectTags(owner, target))
                .build();
    }

    private ContactRelationship requireRelation(SubjectRef owner, SubjectRef target) {
        return contactRelationshipMapper.findByOwnerAndTarget(owner.id(), toFriendType(owner), target.id(), toFriendType(target))
                .filter(relation -> relation.getStatus() == ContactRelationship.RelationshipStatus.ACTIVE)
                .orElseThrow(() -> BizException.of("contact.not_friend"));
    }

    @Override
    public boolean areFriends(SubjectRef owner, SubjectRef target) {
        SubjectRef ownerRef = requireRelationshipSubject(owner);
        SubjectRef targetRef = requireRelationshipSubject(target);
        return contactRelationshipMapper.areFriends(ownerRef.id(), toFriendType(ownerRef), targetRef.id(), toFriendType(targetRef));
    }

    @Override
    public List<SubjectRef> listFriends(SubjectRef owner) {
        SubjectRef ownerRef = requireRelationshipSubject(owner);
        List<ContactRelationship> friends = contactRelationshipMapper.findActiveFriendsByOwner(ownerRef.id(), toFriendType(ownerRef));
        if (friends == null || friends.isEmpty()) {
            return List.of();
        }
        return friends.stream()
                .filter(friend -> friend != null && friend.getFriendId() != null)
                .map(friend -> toSubjectRef(friend.getFriendId(), requireFriendType(friend.getFriendType())))
                .distinct()
                .toList();
    }

    private List<String> selectTags(SubjectRef owner, SubjectRef target) {
        List<String> tags = contactTagMapper.selectActiveTagNames(
                owner.id(),
                toFriendType(owner),
                target.id(),
                toFriendType(target));
        return tags != null ? tags : List.of();
    }

    private SubjectSummaryResponse requireSubject(SubjectRef ref) {
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(ref);
        if (subject == null) {
            throw BizException.of("contact.user.not_found");
        }
        if (subject.getStatus() != null && subject.getStatus() != SubjectStatus.ACTIVE) {
            throw BizException.of("contact.user.not_found");
        }
        return subject;
    }

    private void requireAuthorizedLiability(Long principalHumanId, SubjectRef actor, String errorCode) {
        LiabilityChain chain = liabilityPolicyApi.resolveLiability(actor);
        Long liableHumanId = chain != null ? chain.liableHumanId() : null;
        if (liableHumanId == null || !Objects.equals(liableHumanId, principalHumanId)) {
            throw BizException.of(errorCode);
        }
    }

    private static ContactRelationship relation(SubjectRef owner, SubjectRef target, String source, LocalDateTime now) {
        return ContactRelationship.builder()
                .userId(owner.id())
                .userType(toFriendType(owner))
                .friendId(target.id())
                .friendType(toFriendType(target))
                .status(ContactRelationship.RelationshipStatus.ACTIVE)
                .addSource(source)
                .createTime(now)
                .build();
    }

    private static SubjectRef requireRelationshipSubject(SubjectRef subject) {
        if (subject == null || subject.id() == null || subject.type() == null || subject.type() == SubjectType.SYSTEM) {
            throw BizException.of("contact.subject.invalid");
        }
        return subject;
    }

    private static SubjectRef toSubjectRef(Long id, ContactRelationship.RelationshipSubjectType type) {
        return type == ContactRelationship.RelationshipSubjectType.AGENT ? SubjectRef.agent(id) : SubjectRef.human(id);
    }

    private static ContactRelationship.RelationshipSubjectType toFriendType(SubjectRef ref) {
        if (ref.type() == SubjectType.SYSTEM) {
            throw BizException.of("contact.subject.invalid");
        }
        return ref.type() == SubjectType.AGENT
                ? ContactRelationship.RelationshipSubjectType.AGENT
                : ContactRelationship.RelationshipSubjectType.HUMAN;
    }

    private static ContactRelationship.RelationshipSubjectType requireFriendType(ContactRelationship.RelationshipSubjectType type) {
        if (type == null) {
            throw BizException.of("contact.subject.invalid");
        }
        return type;
    }

    private static String displayName(SubjectSummaryResponse subject) {
        if (subject.getDisplayName() != null && !subject.getDisplayName().isBlank()) {
            return subject.getDisplayName();
        }
        return subject.getType() + ":" + subject.getId();
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
