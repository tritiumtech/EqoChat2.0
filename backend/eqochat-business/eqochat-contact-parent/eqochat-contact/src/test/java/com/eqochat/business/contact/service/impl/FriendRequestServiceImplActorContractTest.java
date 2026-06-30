package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.contact.api.dto.request.SendFriendRequestRequest;
import com.eqochat.business.contact.api.dto.response.FriendRequestResponse;
import com.eqochat.business.contact.entity.FriendRequest;
import com.eqochat.business.contact.mapper.FriendRequestMapper;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.business.user.entity.UserFriend;
import com.eqochat.business.user.mapper.UserFriendMapper;
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
    UserFriendMapper userFriendMapper;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;
    @Mock
    NotificationService notificationService;
    @Mock
    AgentProfileMapper agentProfileMapper;

    FriendRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FriendRequestServiceImpl(
                friendRequestMapper,
                userFriendMapper,
                subjectDirectoryApi,
                liabilityPolicyApi,
                notificationService,
                agentProfileMapper
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
        when(userFriendMapper.areFriends(101L, UserFriend.FriendType.AGENT, 2L, UserFriend.FriendType.HUMAN))
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
        when(userFriendMapper.areFriends(101L, UserFriend.FriendType.AGENT, 2L, UserFriend.FriendType.HUMAN))
                .thenReturn(false);
        when(userFriendMapper.areFriends(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT))
                .thenReturn(false);

        service.accept(2L, 88L);

        ArgumentCaptor<UserFriend> relationCaptor = ArgumentCaptor.forClass(UserFriend.class);
        verify(userFriendMapper, org.mockito.Mockito.times(2)).insert(relationCaptor.capture());
        assertThat(relationCaptor.getAllValues())
                .extracting(
                        UserFriend::getUserId,
                        UserFriend::getUserType,
                        UserFriend::getFriendId,
                        UserFriend::getFriendType,
                        UserFriend::getStatus,
                        UserFriend::getAddSource
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(101L, UserFriend.FriendType.AGENT, 2L, UserFriend.FriendType.HUMAN, UserFriend.FriendStatus.ACTIVE, "FRIEND_REQUEST"),
                        org.assertj.core.groups.Tuple.tuple(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT, UserFriend.FriendStatus.ACTIVE, "FRIEND_REQUEST")
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
