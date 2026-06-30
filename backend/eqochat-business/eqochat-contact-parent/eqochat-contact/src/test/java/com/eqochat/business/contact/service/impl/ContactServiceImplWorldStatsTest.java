package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.entity.UserContactTag;
import com.eqochat.business.contact.mapper.UserContactTagMapper;
import com.eqochat.business.user.entity.UserFriend;
import com.eqochat.business.user.mapper.UserFriendMapper;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplWorldStatsTest {

    @Mock
    UserFriendMapper userFriendMapper;
    @Mock
    UserContactTagMapper userContactTagMapper;
    @Mock
    WorldPostStatsApi worldPostStatsApi;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;

    @Test
    void agentContactDetailUsesSubjectAwareWorldPostStats() {
        ContactServiceImpl service = new ContactServiceImpl(
                userFriendMapper,
                userContactTagMapper,
                worldPostStatsApi,
                subjectDirectoryApi,
                liabilityPolicyApi
        );
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(userFriendMapper.findByOwnerAndTarget(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT))
                .thenReturn(Optional.of(UserFriend.builder()
                        .userId(2L)
                        .userType(UserFriend.FriendType.HUMAN)
                        .friendId(101L)
                        .friendType(UserFriend.FriendType.AGENT)
                        .status(UserFriend.FriendStatus.ACTIVE)
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
        when(userContactTagMapper.selectActiveTagNames(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT))
                .thenReturn(List.of("ai"));
        when(worldPostStatsApi.countByAuthor(101L, "AGENT")).thenReturn(7L);

        ContactDetailResponse detail = service.getContactDetail(2L, SubjectRef.human(2L), SubjectRef.agent(101L));

        assertThat(detail.getWorldPostCount()).isEqualTo(7);
        assertThat(detail.getTargetSubjectType()).isEqualTo(SubjectType.AGENT);
        verify(worldPostStatsApi).countByAuthor(101L, "AGENT");
        verify(worldPostStatsApi, never()).countByAuthorId(101L);
    }
}
