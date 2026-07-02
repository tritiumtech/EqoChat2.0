package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityCode;
import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.api.service.SubjectRelationshipApi;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.eqochat.framework.common.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectProfileServiceImplActorContractTest {

    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    SubjectRelationshipApi subjectRelationshipApi;
    @Mock
    WorldPostStatsApi worldPostStatsApi;
    @Mock
    SubjectRegistryRepository subjectRegistryRepository;

    SubjectProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubjectProfileServiceImpl(
                subjectDirectoryApi,
                subjectRelationshipApi,
                worldPostStatsApi,
                subjectRegistryRepository
        );
    }

    @Test
    void searchReturnsRegistrySubjectsWithoutUserAlias() {
        when(subjectRegistryRepository.search("101", 10)).thenReturn(List.of(
                SubjectRef.human(101L),
                SubjectRef.agent(101L)
        ));
        when(subjectDirectoryApi.listAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.human(9L), SubjectRef.agent(77L)));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(101L))).thenReturn(subject(101L, SubjectType.HUMAN, "Ava", 575));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(subject(101L, SubjectType.AGENT, "Nova", 612));
        when(subjectRelationshipApi.areFriends(SubjectRef.agent(77L), SubjectRef.human(101L))).thenReturn(true);
        when(subjectRelationshipApi.areFriends(SubjectRef.agent(77L), SubjectRef.agent(101L))).thenReturn(false);
        when(worldPostStatsApi.countByAuthor(101L, "HUMAN")).thenReturn(3L);
        when(worldPostStatsApi.countByAuthor(101L, "AGENT")).thenReturn(5L);

        var results = service.search(9L, "101", 77L, SubjectType.AGENT);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(item -> item.getSubjectType().name())
                .containsExactly("HUMAN", "AGENT")
                .doesNotContain("USER");
        assertThat(results).extracting("subjectId").containsExactly(101L, 101L);
        assertThat(results).extracting("worldPostCount").containsExactly(3, 5);
        assertThat(results.get(0).getCreditScore()).isEqualTo(575);
        assertThat(results.get(1).getCreditScore()).isEqualTo(612);
        assertThat(results.get(0).getIsFriend()).isTrue();
        assertThat(results.get(1).getIsFriend()).isFalse();
    }

    @Test
    void searchUsesRegistryRefsAndDeduplicatesResults() {
        when(subjectRegistryRepository.search("nova", 10)).thenReturn(List.of(
                SubjectRef.human(2L),
                SubjectRef.agent(101L),
                SubjectRef.agent(101L)
        ));
        when(subjectDirectoryApi.listAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.human(9L), SubjectRef.agent(77L)));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(2L))).thenReturn(subject(2L, SubjectType.HUMAN, "Ava", 700));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(subject(101L, SubjectType.AGENT, "Nova", 612));
        when(subjectRelationshipApi.areFriends(SubjectRef.agent(77L), SubjectRef.human(2L))).thenReturn(false);
        when(subjectRelationshipApi.areFriends(SubjectRef.agent(77L), SubjectRef.agent(101L))).thenReturn(true);

        var results = service.search(9L, "nova", 77L, SubjectType.AGENT);

        assertThat(results).hasSize(2);
        assertThat(results).extracting("displayName").containsExactly("Ava", "Nova");
        assertThat(results).extracting(item -> item.getSubjectType().name()).containsExactly("HUMAN", "AGENT");
    }

    @Test
    void searchDoesNotFallbackToSourceProfilesWhenRegistryMisses() {
        when(subjectDirectoryApi.listAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.human(9L)));
        when(subjectRegistryRepository.search("nova", 10)).thenReturn(List.of());

        var results = service.search(9L, "nova", 9L, SubjectType.HUMAN);

        assertThat(results).isEmpty();
    }

    @Test
    void publicProfileUsesSubjectAwareWorldStatsAndEnabledCapabilities() {
        SubjectSummaryResponse agent = SubjectSummaryResponse.builder()
                .id(101L)
                .type(SubjectType.AGENT)
                .displayName("Nova")
                .bio("Builder agent")
                .status(SubjectStatus.ACTIVE)
                .creditScore(612)
                .points(540)
                .associatedHumanId(9L)
                .associatedHumanName("Ava")
                .capabilities(new CapabilitySet(List.of(
                        Capability.enabled(CapabilityCode.POST_WORLD),
                        Capability.disabled(CapabilityCode.LOGIN, "agent has no login")
                )))
                .build();
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(agent);
        when(subjectDirectoryApi.listAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.human(9L), SubjectRef.agent(77L)));
        when(subjectRelationshipApi.areFriends(SubjectRef.agent(77L), SubjectRef.agent(101L))).thenReturn(true);
        when(worldPostStatsApi.countByAuthor(101L, "AGENT")).thenReturn(7L);

        var profile = service.getPublicProfile(9L, SubjectType.AGENT, 101L, 77L, SubjectType.AGENT);

        assertThat(profile.getSubjectId()).isEqualTo(101L);
        assertThat(profile.getSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(profile.getWorldPostCount()).isEqualTo(7);
        assertThat(profile.getCreditScore()).isEqualTo(612);
        assertThat(profile.getPoints()).isEqualTo(540);
        assertThat(profile.getAssociatedHumanId()).isEqualTo(9L);
        assertThat(profile.getIsFriend()).isTrue();
        assertThat(profile.getCapabilities()).containsExactly("POST_WORLD");
        verify(worldPostStatsApi).countByAuthor(101L, "AGENT");
    }

    @Test
    void publicProfileRejectsSystemAndMissingSubjects() {
        when(subjectDirectoryApi.listAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.human(9L)));

        assertThatThrownBy(() -> service.getPublicProfile(9L, SubjectType.SYSTEM, 0L, 9L, SubjectType.HUMAN))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.type.invalid");

        when(subjectDirectoryApi.getSubject(SubjectRef.human(404L))).thenReturn(null);
        assertThatThrownBy(() -> service.getPublicProfile(9L, SubjectType.HUMAN, 404L, 9L, SubjectType.HUMAN))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.not_found");
    }

    @Test
    void profileRejectsMissingAndUnauthorizedViewerSubject() {
        assertThatThrownBy(() -> service.getPublicProfile(9L, SubjectType.HUMAN, 101L, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.viewer.invalid");

        when(subjectDirectoryApi.listAssociatedSubjects(9L)).thenReturn(List.of(SubjectRef.human(9L)));
        assertThatThrownBy(() -> service.search(9L, "nova", 77L, SubjectType.AGENT))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.viewer.forbidden");
    }

    private static SubjectSummaryResponse subject(Long id, SubjectType type, String displayName, Integer creditScore) {
        return SubjectSummaryResponse.builder()
                .id(id)
                .type(type)
                .displayName(displayName)
                .status(SubjectStatus.ACTIVE)
                .creditScore(creditScore)
                .points(0)
                .capabilities(new CapabilitySet(List.of()))
                .build();
    }
}
