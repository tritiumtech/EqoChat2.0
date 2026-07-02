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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubjectDirectoryServiceImpl implements SubjectDirectoryApi {

    private final ActorSourceRepository actorSourceRepository;
    private final ActorDataAccess dataAccess;
    private final CapabilityQueryApi capabilityQueryApi;
    private final LiabilityPolicyApi liabilityPolicyApi;
    private final SubjectRegistryRepository subjectRegistryRepository;

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
    public List<SubjectRef> listAssociatedSubjects(Long principalHumanId) {
        if (principalHumanId == null || principalHumanId <= 0) {
            return List.of();
        }
        Set<SubjectRef> subjects = new LinkedHashSet<>(subjectRegistryRepository.findAssociatedSubjects(principalHumanId));
        return new ArrayList<>(subjects);
    }

    @Override
    public Long requireLiableHumanId(SubjectRef ref) {
        LiabilityChain chain = liabilityPolicyApi.resolveLiability(ref);
        return chain != null ? chain.liableHumanId() : null;
    }

    private SubjectSummaryResponse getHuman(Long id) {
        SubjectRef ref = SubjectRef.human(id);
        return subjectRegistryRepository.find(ref)
                .map(this::fromRegistry)
                .orElse(null);
    }

    SubjectSummaryResponse refreshHuman(Long id) {
        SubjectRef ref = SubjectRef.human(id);
        ActorSourceRepository.Human profile = actorSourceRepository.findHuman(id).orElse(null);
        if (profile == null) {
            return null;
        }
        CreditProfileSummary credit = dataAccess.creditProfile(ref, profile.getCreditScore());
        CapabilitySet capabilities = capabilityQueryApi.getCapabilities(ref);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(ref);
        SubjectSummaryResponse subject = SubjectSummaryResponse.builder()
                .id(profile.getId())
                .type(SubjectType.HUMAN)
                .did(profile.did())
                .displayName(dataAccess.displayName(profile))
                .avatarUrl(profile.avatarUrl())
                .bio(profile.bio())
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
        subjectRegistryRepository.upsertHuman(profile, subject);
        return subject;
    }

    private SubjectSummaryResponse getAgent(Long id) {
        SubjectRef ref = SubjectRef.agent(id);
        return subjectRegistryRepository.find(ref)
                .map(this::fromRegistry)
                .orElse(null);
    }

    SubjectSummaryResponse refreshAgent(Long id) {
        SubjectRef ref = SubjectRef.agent(id);
        ActorSourceRepository.Agent profile = actorSourceRepository.findAgent(id).orElse(null);
        if (profile == null) {
            return null;
        }
        ActorSourceRepository.Human owner = profile.getOwnerId() != null
                ? actorSourceRepository.findHuman(profile.getOwnerId()).orElse(null)
                : null;
        CreditProfileSummary credit = dataAccess.creditProfile(ref, profile.getCreditScore());
        CapabilitySet capabilities = capabilityQueryApi.getCapabilities(ref);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(ref);
        SubjectSummaryResponse subject = SubjectSummaryResponse.builder()
                .id(profile.getId())
                .type(SubjectType.AGENT)
                .did(profile.did())
                .displayName(dataAccess.displayName(profile))
                .avatarUrl(profile.avatarUrl())
                .bio(profile.description())
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
        subjectRegistryRepository.upsertAgent(profile, subject);
        return subject;
    }

    private SubjectSummaryResponse fromRegistry(SubjectRegistryRepository.SubjectRegistryRecord row) {
        SubjectRef ref = row.ref();
        CreditProfileSummary credit = dataAccess.creditProfile(ref, row.creditScore());
        CapabilitySet capabilities = capabilityQueryApi.getCapabilities(ref);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(ref);
        Long associatedHumanId = row.associatedHumanId();
        String associatedHumanName = row.associatedHumanName();
        if (row.subjectType() == SubjectType.HUMAN) {
            associatedHumanId = row.subjectId();
            associatedHumanName = row.displayName();
        }
        return SubjectSummaryResponse.builder()
                .id(row.subjectId())
                .type(row.subjectType())
                .did(row.did())
                .displayName(row.displayName())
                .avatarUrl(row.avatarUrl())
                .bio(row.bio())
                .status(row.status())
                .points(dataAccess.currentPoints(ref, row.points()))
                .creditScore(credit.score())
                .creditProfile(credit)
                .liabilityChain(liability)
                .capabilities(capabilities)
                .capabilityTags(dataAccess.parseCapabilityTags(row.capabilityTags()))
                .associatedHumanId(associatedHumanId)
                .associatedHumanName(associatedHumanName)
                .build();
    }
}
