package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.CreditProfileSummary;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.CapabilityQueryApi;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectDirectoryServiceImplRegistryContractTest {

    @Mock
    ActorSourceRepository actorSourceRepository;
    @Mock
    ActorDataAccess dataAccess;
    @Mock
    CapabilityQueryApi capabilityQueryApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;
    @Mock
    SubjectRegistryRepository subjectRegistryRepository;

    SubjectDirectoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubjectDirectoryServiceImpl(
                actorSourceRepository,
                dataAccess,
                capabilityQueryApi,
                liabilityPolicyApi,
                subjectRegistryRepository
        );
    }

    @Test
    void registryHitBuildsAgentSummaryWithoutSourceProfileFallback() {
        SubjectRef agent = SubjectRef.agent(101L);
        when(subjectRegistryRepository.find(agent)).thenReturn(Optional.of(new SubjectRegistryRepository.SubjectRegistryRecord(
                101L,
                SubjectType.AGENT,
                "did:eqochat:agent:nova",
                "Nova",
                "/nova.png",
                "AI Marketing Analyst",
                SubjectStatus.ACTIVE,
                650,
                608,
                2L,
                "John Doe",
                "[\"marketing\"]"
        )));
        when(dataAccess.creditProfile(agent, 608)).thenReturn(new CreditProfileSummary(608, "FAIR", 0, 8, 93));
        when(dataAccess.currentPoints(agent, 650)).thenReturn(650);
        when(dataAccess.parseCapabilityTags("[\"marketing\"]")).thenReturn(List.of("marketing"));
        when(capabilityQueryApi.getCapabilities(agent)).thenReturn(new CapabilitySet(List.of()));
        when(liabilityPolicyApi.resolveLiability(agent)).thenReturn(LiabilityChain.agentToHuman(101L, 2L));

        var subject = service.getSubject(agent);

        assertThat(subject.getId()).isEqualTo(101L);
        assertThat(subject.getType()).isEqualTo(SubjectType.AGENT);
        assertThat(subject.getDisplayName()).isEqualTo("Nova");
        assertThat(subject.getAssociatedHumanId()).isEqualTo(2L);
        assertThat(subject.getAssociatedHumanName()).isEqualTo("John Doe");
        assertThat(subject.getPoints()).isEqualTo(650);
        assertThat(subject.getCreditScore()).isEqualTo(608);
        assertThat(subject.getCapabilityTags()).containsExactly("marketing");
        verify(actorSourceRepository, never()).findAgent(101L);
        verify(actorSourceRepository, never()).findHuman(2L);
    }

    @Test
    void registryMissDoesNotReadSourceProfilesAtRuntime() {
        SubjectRef human = SubjectRef.human(9L);
        when(subjectRegistryRepository.find(human)).thenReturn(Optional.empty());

        var subject = service.getSubject(human);

        assertThat(subject).isNull();
        verify(actorSourceRepository, never()).findHuman(9L);
        verify(subjectRegistryRepository, never()).upsertHuman(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void explicitRefreshUsesSourceProfileAndUpdatesRegistry() {
        SubjectRef human = SubjectRef.human(9L);
        ActorSourceRepository.Human profile = human(9L, "did:eqochat:user:ava", "Ava", 72);
        when(actorSourceRepository.findHuman(9L)).thenReturn(Optional.of(profile));
        when(dataAccess.creditProfile(human, 72)).thenReturn(new CreditProfileSummary(696, "GOOD", 0, 3, 94));
        when(dataAccess.currentPoints(human, 0)).thenReturn(185);
        when(dataAccess.displayName(profile)).thenReturn("Ava");
        when(dataAccess.userStatus(profile)).thenReturn(SubjectStatus.ACTIVE);
        when(capabilityQueryApi.getCapabilities(human)).thenReturn(new CapabilitySet(List.of()));
        when(liabilityPolicyApi.resolveLiability(human)).thenReturn(LiabilityChain.selfResponsible(9L));

        var subject = service.refreshHuman(9L);

        assertThat(subject.getDisplayName()).isEqualTo("Ava");
        assertThat(subject.getCreditScore()).isEqualTo(696);
        assertThat(subject.getPoints()).isEqualTo(185);
        assertThat(subject.getAssociatedHumanId()).isEqualTo(9L);
        verify(subjectRegistryRepository).upsertHuman(profile, subject);
    }

    @Test
    void sameNumericIdRemainsIsolatedBySubjectType() {
        SubjectRef humanRef = SubjectRef.human(101L);
        SubjectRef agentRef = SubjectRef.agent(101L);
        when(subjectRegistryRepository.find(humanRef)).thenReturn(Optional.of(registryRecord(
                101L,
                SubjectType.HUMAN,
                "Ava",
                101L,
                "Ava"
        )));
        when(subjectRegistryRepository.find(agentRef)).thenReturn(Optional.of(registryRecord(
                101L,
                SubjectType.AGENT,
                "Nova",
                9L,
                "Owner"
        )));
        when(dataAccess.creditProfile(humanRef, 700)).thenReturn(new CreditProfileSummary(700, "GOOD", 0, 0, 0));
        when(dataAccess.creditProfile(agentRef, 700)).thenReturn(new CreditProfileSummary(612, "FAIR", 0, 0, 0));
        when(dataAccess.currentPoints(humanRef, 0)).thenReturn(10);
        when(dataAccess.currentPoints(agentRef, 0)).thenReturn(20);
        when(dataAccess.parseCapabilityTags(null)).thenReturn(List.of());
        when(capabilityQueryApi.getCapabilities(humanRef)).thenReturn(new CapabilitySet(List.of()));
        when(capabilityQueryApi.getCapabilities(agentRef)).thenReturn(new CapabilitySet(List.of()));
        when(liabilityPolicyApi.resolveLiability(humanRef)).thenReturn(LiabilityChain.selfResponsible(101L));
        when(liabilityPolicyApi.resolveLiability(agentRef)).thenReturn(LiabilityChain.agentToHuman(101L, 9L));

        var subjects = service.batchGetSubjects(List.of(humanRef, agentRef));

        assertThat(subjects).hasSize(2);
        assertThat(subjects.get(humanRef).getDisplayName()).isEqualTo("Ava");
        assertThat(subjects.get(agentRef).getDisplayName()).isEqualTo("Nova");
        assertThat(subjects.get(humanRef).getAssociatedHumanId()).isEqualTo(101L);
        assertThat(subjects.get(agentRef).getAssociatedHumanId()).isEqualTo(9L);
    }

    @Test
    void associatedSubjectsUseRegistryAndIncludesRegisteredPrincipalHuman() {
        when(subjectRegistryRepository.findAssociatedSubjects(9L)).thenReturn(List.of(
                SubjectRef.human(9L),
                SubjectRef.agent(101L),
                SubjectRef.agent(102L),
                SubjectRef.agent(102L)
        ));

        var subjects = service.listAssociatedSubjects(9L);

        assertThat(subjects).containsExactly(
                SubjectRef.human(9L),
                SubjectRef.agent(101L),
                SubjectRef.agent(102L)
        );
    }

    @Test
    void associatedSubjectsDoNotSynthesizePrincipalHumanWhenRegistryOmitsIt() {
        when(subjectRegistryRepository.findAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.agent(101L)));

        var subjects = service.listAssociatedSubjects(9L);

        assertThat(subjects).containsExactly(SubjectRef.agent(101L));
    }

    @Test
    void invalidPrincipalHasNoAssociatedSubjectsAndDoesNotReadRegistry() {
        assertThat(service.listAssociatedSubjects(null)).isEmpty();
        assertThat(service.listAssociatedSubjects(0L)).isEmpty();
        assertThat(service.listAssociatedSubjects(-1L)).isEmpty();

        verify(subjectRegistryRepository, never()).findAssociatedSubjects(any());
    }

    private static SubjectRegistryRepository.SubjectRegistryRecord registryRecord(
            Long id,
            SubjectType type,
            String displayName,
            Long associatedHumanId,
            String associatedHumanName
    ) {
        return new SubjectRegistryRepository.SubjectRegistryRecord(
                id,
                type,
                "did:eqochat:" + type.name().toLowerCase() + ":" + id,
                displayName,
                null,
                null,
                SubjectStatus.ACTIVE,
                0,
                700,
                associatedHumanId,
                associatedHumanName,
                null
        );
    }

    private static ActorSourceRepository.Human human(Long id, String did, String nickname, Integer creditScore) {
        return new ActorSourceRepository.Human(
                id,
                did,
                null,
                null,
                nickname,
                null,
                null,
                ActorSourceRepository.HumanStatus.ACTIVE,
                creditScore
        );
    }
}
