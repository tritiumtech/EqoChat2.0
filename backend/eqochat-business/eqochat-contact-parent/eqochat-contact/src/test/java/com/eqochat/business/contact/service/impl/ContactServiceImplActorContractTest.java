package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.entity.UserContactTag;
import com.eqochat.business.contact.mapper.UserContactTagMapper;
import com.eqochat.business.user.entity.UserFriend;
import com.eqochat.business.user.mapper.UserFriendMapper;
import com.eqochat.business.world.api.service.WorldPostStatsApi;
import com.eqochat.framework.common.BizException;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplActorContractTest {

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

    ContactServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContactServiceImpl(
                userFriendMapper,
                userContactTagMapper,
                worldPostStatsApi,
                subjectDirectoryApi,
                liabilityPolicyApi
        );
    }

    @Test
    void userFriendMapperSqlScopesOwnerAndTargetBySubjectType() throws Exception {
        assertThat(selectSql(UserFriendMapper.class, "findByOwner"))
                .contains("user_id = #{ownerId}", "user_type = #{ownerType}");
        assertThat(selectSql(UserFriendMapper.class, "findByTarget"))
                .contains("friend_id = #{targetId}", "friend_type = #{targetType}");
        assertThat(selectSql(UserFriendMapper.class, "findByOwnerAndTarget"))
                .contains(
                        "user_id = #{ownerId}",
                        "user_type = #{ownerType}",
                        "friend_id = #{targetId}",
                        "friend_type = #{targetType}"
                );
        assertThat(selectSql(UserFriendMapper.class, "findActiveFriendsByOwner"))
                .contains("user_id = #{ownerId}", "user_type = #{ownerType}", "status = 'ACTIVE'");
        assertThat(selectSql(UserFriendMapper.class, "areFriends"))
                .contains(
                        "user_id = #{ownerId}",
                        "user_type = #{ownerType}",
                        "friend_id = #{targetId}",
                        "friend_type = #{targetType}",
                        "status = 'ACTIVE'"
                );
    }

    @Test
    void tagMapperSqlScopesTagsByOwnerAndTargetSubjectType() throws Exception {
        assertThat(selectSql(UserContactTagMapper.class, "selectActiveTagNames"))
                .contains(
                        "user_id = #{userId}",
                        "user_type = #{userType}",
                        "friend_id = #{friendId}",
                        "friend_type = #{friendType}"
                );
        assertThat(deleteSql(UserContactTagMapper.class, "hardDeleteAll"))
                .contains(
                        "user_id = #{userId}",
                        "user_type = #{userType}",
                        "friend_id = #{friendId}",
                        "friend_type = #{friendType}"
                );
    }

    @Test
    void listContactsResolvesTargetsWithTypedSubjectRefs() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(userFriendMapper.findActiveFriendsByOwner(2L, UserFriend.FriendType.HUMAN)).thenReturn(List.of(
                UserFriend.builder()
                        .userId(2L)
                        .userType(UserFriend.FriendType.HUMAN)
                        .friendId(101L)
                        .friendType(UserFriend.FriendType.AGENT)
                        .status(UserFriend.FriendStatus.ACTIVE)
                        .build(),
                UserFriend.builder()
                        .userId(2L)
                        .userType(UserFriend.FriendType.HUMAN)
                        .friendId(101L)
                        .friendType(UserFriend.FriendType.HUMAN)
                        .status(UserFriend.FriendStatus.ACTIVE)
                        .build()
        ));
        when(subjectDirectoryApi.batchGetSubjects(Set.of(SubjectRef.agent(101L), SubjectRef.human(101L))))
                .thenReturn(Map.of(
                        SubjectRef.agent(101L), activeSubject(101L, SubjectType.AGENT, "Nova"),
                        SubjectRef.human(101L), activeSubject(101L, SubjectType.HUMAN, "Human 101")
                ));
        when(userContactTagMapper.selectActiveTagNames(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT))
                .thenReturn(List.of("agent"));
        when(userContactTagMapper.selectActiveTagNames(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.HUMAN))
                .thenReturn(List.of("human"));

        var contacts = service.listContacts(2L, SubjectRef.human(2L));

        assertThat(contacts)
                .extracting("targetSubjectId", "targetSubjectType", "nickname", "tags")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, SubjectType.AGENT, "Nova", List.of("agent")),
                        org.assertj.core.groups.Tuple.tuple(101L, SubjectType.HUMAN, "Human 101", List.of("human"))
                );
    }

    @Test
    void updateContactTagsDeletesAndInsertsOnlyTheTypedTarget() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(userFriendMapper.findByOwnerAndTarget(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT))
                .thenReturn(Optional.of(UserFriend.builder()
                        .userId(2L)
                        .userType(UserFriend.FriendType.HUMAN)
                        .friendId(101L)
                        .friendType(UserFriend.FriendType.AGENT)
                        .status(UserFriend.FriendStatus.ACTIVE)
                        .build()));

        var tags = service.updateContactTags(2L, SubjectRef.human(2L), SubjectRef.agent(101L),
                List.of("AI", "ai", " Research "));

        assertThat(tags).containsExactly("AI", "Research");
        verify(userContactTagMapper)
                .hardDeleteAll(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT);
        ArgumentCaptor<UserContactTag> tagCaptor = ArgumentCaptor.forClass(UserContactTag.class);
        verify(userContactTagMapper, org.mockito.Mockito.times(2)).insert(tagCaptor.capture());
        assertThat(tagCaptor.getAllValues())
                .extracting(
                        UserContactTag::getUserId,
                        UserContactTag::getUserType,
                        UserContactTag::getFriendId,
                        UserContactTag::getFriendType,
                        UserContactTag::getTagName
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT, "AI"),
                        org.assertj.core.groups.Tuple.tuple(2L, UserFriend.FriendType.HUMAN, 101L, UserFriend.FriendType.AGENT, "Research")
                );
    }

    @Test
    void contactOperationsRejectSystemSubjects() {
        assertThatThrownBy(() -> service.getContactDetail(2L, SubjectRef.human(2L), SubjectRef.system(0L)))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.subject.invalid");
    }

    private static SubjectSummaryResponse activeSubject(Long id, SubjectType type, String name) {
        return SubjectSummaryResponse.builder()
                .id(id)
                .type(type)
                .displayName(name)
                .status(SubjectStatus.ACTIVE)
                .build();
    }

    private static String selectSql(Class<?> mapperType, String methodName) throws Exception {
        for (Method method : mapperType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Select select = method.getAnnotation(Select.class);
                return String.join("\n", select.value());
            }
        }
        throw new IllegalArgumentException("method not found: " + mapperType.getSimpleName() + "." + methodName);
    }

    private static String deleteSql(Class<?> mapperType, String methodName) throws Exception {
        for (Method method : mapperType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Delete delete = method.getAnnotation(Delete.class);
                return String.join("\n", delete.value());
            }
        }
        throw new IllegalArgumentException("method not found: " + mapperType.getSimpleName() + "." + methodName);
    }
}
