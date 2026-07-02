package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectPublicProfileResponse;
import com.eqochat.business.actor.api.dto.response.SubjectSearchResponse;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityState;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.SubjectProfileApi;
import com.eqochat.business.contact.api.service.SubjectRelationshipApi;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.eqochat.framework.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubjectProfileServiceImpl implements SubjectProfileApi {

    private static final int MAX_SEARCH_RESULTS = 10;

    private final SubjectDirectoryApi subjectDirectoryApi;
    private final SubjectRelationshipApi subjectRelationshipApi;
    private final WorldPostStatsApi worldPostStatsApi;
    private final SubjectRegistryRepository subjectRegistryRepository;

    @Override
    public List<SubjectSearchResponse> search(
            Long principalHumanId,
            String keyword,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        if (!StringUtils.hasText(keyword)) {
            throw BizException.of("subject.search.keyword.required");
        }
        SubjectRef viewer = requireAuthorizedViewer(principalHumanId, viewerSubjectId, viewerSubjectType);
        String value = keyword.trim();
        Map<SubjectRef, SubjectSearchResponse> results = new LinkedHashMap<>();

        for (SubjectRef ref : safeRefs(subjectRegistryRepository.search(value, MAX_SEARCH_RESULTS))) {
            if (results.size() >= MAX_SEARCH_RESULTS) {
                break;
            }
            addSearchResult(results, viewer, ref);
        }

        return new ArrayList<>(results.values());
    }

    @Override
    public SubjectPublicProfileResponse getPublicProfile(
            Long principalHumanId,
            SubjectType subjectType,
            Long subjectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        SubjectRef viewer = requireAuthorizedViewer(principalHumanId, viewerSubjectId, viewerSubjectType);
        SubjectRef target = toTarget(subjectType, subjectId);
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(target);
        if (subject == null) {
            throw BizException.of("subject.not_found");
        }

        boolean isFriend = isFriend(viewer, target);
        List<String> capabilities = enabledCapabilities(subject);
        return SubjectPublicProfileResponse.builder()
                .subjectId(subject.getId())
                .subjectType(subject.getType())
                .did(subject.getDid())
                .displayName(subject.getDisplayName())
                .avatarUrl(subject.getAvatarUrl())
                .bio(subject.getBio())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .worldPostCount(worldPostCount(target))
                .creditScore(subject.getCreditScore())
                .points(subject.getPoints())
                .friendType(subject.getType().name())
                .isFriend(isFriend)
                .capabilities(capabilities)
                .tags(List.of())
                .associatedHumanId(subject.getAssociatedHumanId())
                .associatedHumanName(subject.getAssociatedHumanName())
                .id(subject.getId())
                .nickname(subject.getDisplayName())
                .build();
    }

    private void addSearchResult(Map<SubjectRef, SubjectSearchResponse> results, SubjectRef viewer, SubjectRef ref) {
        if (ref == null || results.containsKey(ref) || results.size() >= MAX_SEARCH_RESULTS) {
            return;
        }
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(ref);
        if (subject == null || subject.getType() == SubjectType.SYSTEM) {
            return;
        }
        results.put(ref, SubjectSearchResponse.builder()
                .subjectId(subject.getId())
                .subjectType(subject.getType())
                .did(subject.getDid())
                .displayName(subject.getDisplayName())
                .avatarUrl(subject.getAvatarUrl())
                .bio(subject.getBio())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .worldPostCount(worldPostCount(ref))
                .creditScore(subject.getCreditScore())
                .isFriend(isFriend(viewer, ref))
                .associatedHumanId(subject.getAssociatedHumanId())
                .associatedHumanName(subject.getAssociatedHumanName())
                .id(subject.getId())
                .nickname(subject.getDisplayName())
                .build());
    }

    private SubjectRef toTarget(SubjectType subjectType, Long subjectId) {
        if (subjectId == null || subjectId <= 0 || subjectType == null || subjectType == SubjectType.SYSTEM) {
            throw BizException.of("subject.type.invalid");
        }
        return subjectType == SubjectType.AGENT ? SubjectRef.agent(subjectId) : SubjectRef.human(subjectId);
    }

    private SubjectRef requireAuthorizedViewer(Long principalHumanId, Long viewerSubjectId, SubjectType viewerSubjectType) {
        SubjectRef viewer = toViewer(viewerSubjectType, viewerSubjectId);
        if (!safeRefs(subjectDirectoryApi.listAssociatedSubjects(principalHumanId)).contains(viewer)) {
            throw BizException.of("subject.viewer.forbidden");
        }
        return viewer;
    }

    private SubjectRef toViewer(SubjectType subjectType, Long subjectId) {
        if (subjectId == null || subjectId <= 0 || subjectType == null || subjectType == SubjectType.SYSTEM) {
            throw BizException.of("subject.viewer.invalid");
        }
        return subjectType == SubjectType.AGENT ? SubjectRef.agent(subjectId) : SubjectRef.human(subjectId);
    }

    private boolean isFriend(SubjectRef viewer, SubjectRef target) {
        if (viewer == null || viewer.type() == SubjectType.SYSTEM || target == null || target.type() == SubjectType.SYSTEM) {
            return false;
        }
        return subjectRelationshipApi.areFriends(viewer, target);
    }

    private int worldPostCount(SubjectRef ref) {
        if (ref == null || ref.id() == null || ref.type() == SubjectType.SYSTEM) {
            return 0;
        }
        return Math.toIntExact(worldPostStatsApi.countByAuthor(ref.id(), ref.type().name()));
    }

    private List<String> enabledCapabilities(SubjectSummaryResponse subject) {
        if (subject == null || subject.getCapabilities() == null) {
            return List.of();
        }
        return subject.getCapabilities().capabilities().stream()
                .filter(item -> item != null && item.state() == CapabilityState.ENABLED && item.code() != null)
                .map(Capability::code)
                .map(Enum::name)
                .toList();
    }

    private List<SubjectRef> safeRefs(List<SubjectRef> refs) {
        return refs == null ? List.of() : refs;
    }
}
