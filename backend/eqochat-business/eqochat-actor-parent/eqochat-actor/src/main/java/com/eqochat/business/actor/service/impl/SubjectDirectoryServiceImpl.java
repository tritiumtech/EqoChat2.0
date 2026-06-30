package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.CreditProfileSummary;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.CapabilityQueryApi;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.agent.entity.AgentProfile;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubjectDirectoryServiceImpl implements SubjectDirectoryApi {

    private final UserProfileMapper userProfileMapper;
    private final AgentProfileMapper agentProfileMapper;
    private final ActorDataAccess dataAccess;
    private final CapabilityQueryApi capabilityQueryApi;
    private final LiabilityPolicyApi liabilityPolicyApi;

    @Override
    public SubjectSummaryResponse getSubject(SubjectRef ref) {
        if (ref == null || ref.id() == null) {
            return null;
        }
        if (ref.type() == SubjectType.AGENT) {
            return getAgent(ref.id());
        }
        if (ref.type() == SubjectType.SYSTEM) {
            return SubjectSummaryResponse.builder()
                    .id(ref.id())
                    .type(SubjectType.SYSTEM)
                    .displayName("System")
                    .status(com.eqochat.business.actor.api.model.SubjectStatus.ACTIVE)
                    .points(0)
                    .creditScore(300)
                    .creditProfile(new CreditProfileSummary(300, "SYSTEM", 0, 0, 0))
                    .liabilityChain(LiabilityChain.unresolved(ref))
                    .capabilities(new CapabilitySet(java.util.List.of()))
                    .capabilityTags(java.util.List.of())
                    .build();
        }
        return getHuman(ref.id());
    }

    @Override
    public Map<SubjectRef, SubjectSummaryResponse> batchGetSubjects(Collection<SubjectRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Map.of();
        }
        Map<SubjectRef, SubjectSummaryResponse> out = new LinkedHashMap<>();
        for (SubjectRef ref : refs) {
            if (ref == null || out.containsKey(ref)) {
                continue;
            }
            SubjectSummaryResponse subject = getSubject(ref);
            if (subject != null) {
                out.put(ref, subject);
            }
        }
        return out;
    }

    @Override
    public Long requireLiableHumanId(SubjectRef ref) {
        LiabilityChain chain = liabilityPolicyApi.resolveLiability(ref);
        return chain != null ? chain.liableHumanId() : null;
    }

    private SubjectSummaryResponse getHuman(Long id) {
        UserProfile profile = userProfileMapper.selectById(id);
        if (profile == null) {
            return null;
        }
        SubjectRef ref = SubjectRef.human(profile.getId());
        CreditProfileSummary credit = dataAccess.creditProfile(ref, profile.getCreditScore());
        CapabilitySet capabilities = capabilityQueryApi.getCapabilities(ref);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(ref);
        return SubjectSummaryResponse.builder()
                .id(profile.getId())
                .type(SubjectType.HUMAN)
                .did(profile.getDid())
                .displayName(dataAccess.displayName(profile))
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .status(dataAccess.userStatus(profile))
                .points(dataAccess.currentPoints(ref, 0))
                .creditScore(credit.score())
                .creditProfile(credit)
                .liabilityChain(liability)
                .capabilities(capabilities)
                .capabilityTags(java.util.List.of())
                .associatedHumanId(profile.getId())
                .associatedHumanName(dataAccess.displayName(profile))
                .build();
    }

    private SubjectSummaryResponse getAgent(Long id) {
        AgentProfile profile = agentProfileMapper.selectById(id);
        if (profile == null) {
            return null;
        }
        UserProfile owner = profile.getOwnerId() != null ? userProfileMapper.selectById(profile.getOwnerId()) : null;
        SubjectRef ref = SubjectRef.agent(profile.getId());
        CreditProfileSummary credit = dataAccess.creditProfile(ref, profile.getCreditScore());
        CapabilitySet capabilities = capabilityQueryApi.getCapabilities(ref);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(ref);
        return SubjectSummaryResponse.builder()
                .id(profile.getId())
                .type(SubjectType.AGENT)
                .did(profile.getDid())
                .displayName(dataAccess.displayName(profile))
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getDescription())
                .status(dataAccess.agentStatus(profile))
                .points(dataAccess.currentPoints(ref, 0))
                .creditScore(credit.score())
                .creditProfile(credit)
                .liabilityChain(liability)
                .capabilities(capabilities)
                .capabilityTags(dataAccess.parseCapabilityTags(profile.getCapabilityTags()))
                .associatedHumanId(profile.getOwnerId())
                .associatedHumanName(owner != null ? dataAccess.displayName(owner) : null)
                .build();
    }
}
