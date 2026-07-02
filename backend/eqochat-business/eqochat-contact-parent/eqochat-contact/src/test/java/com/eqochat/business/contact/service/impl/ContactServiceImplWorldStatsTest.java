package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.entity.ContactRelationship;
import com.eqochat.business.contact.mapper.ContactRelationshipMapper;
import com.eqochat.business.contact.mapper.ContactTagMapper;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplWorldStatsTest {

    @Mock
    ContactRelationshipMapper contactRelationshipMapper;
    @Mock
    ContactTagMapper contactTagMapper;
    @Mock
    WorldPostStatsApi worldPostStatsApi;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;

    @Test
    void agentContactDetailUsesSubjectAwareWorldPostStats() {
        ContactServiceImpl service = new ContactServiceImpl(
                contactRelationshipMapper,
                contactTagMapper,
                worldPostStatsApi,
                subjectDirectoryApi,
                liabilityPolicyApi
        );
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(contactRelationshipMapper.findByOwnerAndTarget(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT))
                .thenReturn(Optional.of(ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .delToken(0L)
                        .build()));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L)))
                .thenReturn(SubjectSummaryResponse.builder()
                        .id(101L)
                        .type(SubjectType.AGENT)
                        .displayName("Nova")
                        .status(SubjectStatus.ACTIVE)
                        .capabilityTags(List.of("research"))
                        .build());
        when(contactTagMapper.selectActiveTagNames(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT))
                .thenReturn(List.of("ai"));
        when(worldPostStatsApi.countByAuthor(101L, "AGENT")).thenReturn(7L);

        ContactDetailResponse detail = service.getContactDetail(2L, SubjectRef.human(2L), SubjectRef.agent(101L));

        assertThat(detail.getWorldPostCount()).isEqualTo(7);
        assertThat(detail.getTargetSubjectType()).isEqualTo(SubjectType.AGENT);
        verify(worldPostStatsApi).countByAuthor(101L, "AGENT");
    }
}
