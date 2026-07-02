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
import com.eqochat.business.contact.entity.ContactRelationship;
import com.eqochat.business.contact.entity.FriendRequest;
import com.eqochat.business.contact.mapper.ContactRelationshipMapper;
import com.eqochat.business.contact.mapper.FriendRequestMapper;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.framework.common.BizException;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceImplActorContractTest {

    @Mock
    FriendRequestMapper friendRequestMapper;
    @Mock
    ContactRelationshipMapper contactRelationshipMapper;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;
    @Mock
    NotificationService notificationService;

    FriendRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FriendRequestServiceImpl(
                friendRequestMapper,
                contactRelationshipMapper,
                subjectDirectoryApi,
                liabilityPolicyApi,
                notificationService
        );
    }

    @Test
    void mapperSqlScopesRequestsByBothSubjectIdAndType() throws Exception {
        assertThat(selectSql("findByRequesterSubject"))
                .contains("requester_id = #{requesterId}", "requester_type = #{requesterType}");
        assertThat(selectSql("findByRecipientSubject"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}");
        assertThat(selectSql("findPendingRequest"))
                .contains(
                        "requester_id = #{requesterId}",
                        "requester_type = #{requesterType}",
                        "recipient_id = #{recipientId}",
                        "recipient_type = #{recipientType}",
                        "status = 'PENDING'"
                );
        assertThat(selectSql("findPendingByRecipientSubject"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}", "status = 'PENDING'");
        assertThat(selectSql("countPendingByRecipientSubject"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}", "status = 'PENDING'");
    }

    @Test
    void sendRequestPersistsCanonicalSubjectsAndNotifiesRecipientWithSubjectPayload() {
        SendFriendRequestRequest request = new SendFriendRequestRequest();
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);
        request.setRecipientSubjectId(2L);
        request.setRecipientSubjectType(SubjectType.HUMAN);
        request.setRequestMessage("hello");

        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(activeSubject(101L, SubjectType.AGENT, "Nova"));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(2L))).thenReturn(activeSubject(2L, SubjectType.HUMAN, "Ava"));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(contactRelationshipMapper.areFriends(101L, ContactRelationship.RelationshipSubjectType.AGENT, 2L, ContactRelationship.RelationshipSubjectType.HUMAN))
                .thenReturn(false);
        when(friendRequestMapper.findPendingRequest(101L, SubjectType.AGENT, 2L, SubjectType.HUMAN))
                .thenReturn(Optional.empty());
        doAnswer(invocation -> {
            FriendRequest entity = invocation.getArgument(0);
            entity.setId(77L);
            entity.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(friendRequestMapper).insert(any(FriendRequest.class));

        FriendRequestResponse response = service.sendRequest(9L, request);

        ArgumentCaptor<FriendRequest> requestCaptor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestMapper).insert(requestCaptor.capture());
        FriendRequest saved = requestCaptor.getValue();
        assertThat(saved.getRequesterId()).isEqualTo(101L);
        assertThat(saved.getRequesterType()).isEqualTo(SubjectType.AGENT);
        assertThat(saved.getRecipientId()).isEqualTo(2L);
        assertThat(saved.getRecipientType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getRequesterSubjectId()).isEqualTo(101L);
        assertThat(response.getRequesterSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getRecipientSubjectId()).isEqualTo(2L);
        assertThat(response.getRecipientSubjectType()).isEqualTo(SubjectType.HUMAN);

        verify(notificationService).sendNotification(
                eq(SubjectRef.human(2L)),
                eq("FRIEND_REQUEST"),
                eq("新的好友请求"),
                eq("hello"),
                org.mockito.ArgumentMatchers.contains("\"requesterSubjectType\":\"AGENT\""),
                eq(SubjectRef.agent(101L))
        );
    }

    @Test
    void acceptCreatesReciprocalTypedFriendRowsWithoutNumericIdCollision() {
        FriendRequest request = FriendRequest.builder()
                .id(88L)
                .requesterId(101L)
                .requesterType(SubjectType.AGENT)
                .recipientId(2L)
                .recipientType(SubjectType.HUMAN)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();
        when(friendRequestMapper.selectById(88L)).thenReturn(request);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(activeSubject(101L, SubjectType.AGENT, "Nova"));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(2L))).thenReturn(activeSubject(2L, SubjectType.HUMAN, "Ava"));
        when(contactRelationshipMapper.areFriends(101L, ContactRelationship.RelationshipSubjectType.AGENT, 2L, ContactRelationship.RelationshipSubjectType.HUMAN))
                .thenReturn(false);
        when(contactRelationshipMapper.areFriends(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT))
                .thenReturn(false);

        service.accept(2L, 88L);

        ArgumentCaptor<ContactRelationship> relationCaptor = ArgumentCaptor.forClass(ContactRelationship.class);
        verify(contactRelationshipMapper, org.mockito.Mockito.times(2)).insert(relationCaptor.capture());
        assertThat(relationCaptor.getAllValues())
                .extracting(
                        ContactRelationship::getUserId,
                        ContactRelationship::getUserType,
                        ContactRelationship::getFriendId,
                        ContactRelationship::getFriendType,
                        ContactRelationship::getStatus,
                        ContactRelationship::getAddSource
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(101L, ContactRelationship.RelationshipSubjectType.AGENT, 2L, ContactRelationship.RelationshipSubjectType.HUMAN, ContactRelationship.RelationshipStatus.ACTIVE, "FRIEND_REQUEST"),
                        org.assertj.core.groups.Tuple.tuple(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT, ContactRelationship.RelationshipStatus.ACTIVE, "FRIEND_REQUEST")
                );
        verify(notificationService).sendNotification(
                eq(SubjectRef.agent(101L)),
                eq("FRIEND_REQUEST"),
                eq("好友请求已接受"),
                org.mockito.ArgumentMatchers.contains("Ava"),
                org.mockito.ArgumentMatchers.contains("\"recipientSubjectType\":\"HUMAN\""),
                eq(SubjectRef.human(2L))
        );
    }

    @Test
    void relationshipSubjectsRejectSystem() {
        SendFriendRequestRequest request = new SendFriendRequestRequest();
        request.setActorSubjectId(0L);
        request.setActorSubjectType(SubjectType.SYSTEM);
        request.setRecipientSubjectId(2L);
        request.setRecipientSubjectType(SubjectType.HUMAN);

        assertThatThrownBy(() -> service.sendRequest(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("friend_request.subject.invalid");
        verify(friendRequestMapper, never()).insert(any(FriendRequest.class));
    }

    @Test
    void explicitReceivedInboxIsAuthorizedAndScopedToThatSubjectOnly() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(friendRequestMapper.findPendingByRecipientSubject(101L, SubjectType.AGENT)).thenReturn(List.of());

        service.listReceived(9L, SubjectRef.agent(101L));

        verify(friendRequestMapper).findPendingByRecipientSubject(101L, SubjectType.AGENT);
    }

    @Test
    void explicitAgentSentInboxIsAuthorizedAndScopedToThatAgentOnly() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(friendRequestMapper.findByRequesterSubject(101L, SubjectType.AGENT)).thenReturn(List.of());

        service.listSent(9L, SubjectRef.agent(101L));

        verify(friendRequestMapper).findByRequesterSubject(101L, SubjectType.AGENT);
        verify(subjectDirectoryApi, never()).batchGetSubjects(any());
    }

    @Test
    void explicitUnauthorizedAgentInboxIsRejectedBeforeMapperQuery() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 8L));

        assertThatThrownBy(() -> service.listReceived(9L, SubjectRef.agent(101L)))
                .isInstanceOf(BizException.class)
                .hasMessage("friend_request.not_recipient");

        verify(friendRequestMapper, never()).findPendingByRecipientSubject(any(), any());
        verify(friendRequestMapper, never()).findByRequesterSubject(any(), any());
    }

    private static SubjectSummaryResponse activeSubject(Long id, SubjectType type, String name) {
        return SubjectSummaryResponse.builder()
                .id(id)
                .type(type)
                .displayName(name)
                .status(SubjectStatus.ACTIVE)
                .build();
    }

    private static String selectSql(String methodName) throws Exception {
        for (Method method : FriendRequestMapper.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Select select = method.getAnnotation(Select.class);
                return String.join("\n", select.value());
            }
        }
        throw new IllegalArgumentException("method not found: " + methodName);
    }
}
