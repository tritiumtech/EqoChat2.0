package com.eqochat.business.contact.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.contact.entity.ContactRelationship;
import com.eqochat.business.contact.entity.ContactTag;
import com.eqochat.business.contact.mapper.ContactRelationshipMapper;
import com.eqochat.business.contact.mapper.ContactTagMapper;
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
    ContactRelationshipMapper contactRelationshipMapper;
    @Mock
    ContactTagMapper contactTagMapper;
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
                contactRelationshipMapper,
                contactTagMapper,
                worldPostStatsApi,
                subjectDirectoryApi,
                liabilityPolicyApi
        );
    }

    @Test
    void contactRelationshipMapperSqlScopesOwnerAndTargetBySubjectType() throws Exception {
        assertThat(selectSql(ContactRelationshipMapper.class, "findByOwner"))
                .contains("FROM contact_relationship", "user_id = #{ownerId}", "user_type = #{ownerType}");
        assertThat(selectSql(ContactRelationshipMapper.class, "findByTarget"))
                .contains("friend_id = #{targetId}", "friend_type = #{targetType}");
        assertThat(selectSql(ContactRelationshipMapper.class, "findByOwnerAndTarget"))
                .contains(
                        "user_id = #{ownerId}",
                        "user_type = #{ownerType}",
                        "friend_id = #{targetId}",
                        "friend_type = #{targetType}"
                );
        assertThat(selectSql(ContactRelationshipMapper.class, "findActiveFriendsByOwner"))
                .contains("user_id = #{ownerId}", "user_type = #{ownerType}", "status = 'ACTIVE'");
        assertThat(selectSql(ContactRelationshipMapper.class, "areFriends"))
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
        assertThat(selectSql(ContactTagMapper.class, "selectActiveTagNames"))
                .contains(
                        "FROM contact_tag",
                        "user_id = #{userId}",
                        "user_type = #{userType}",
                        "friend_id = #{friendId}",
                        "friend_type = #{friendType}"
                );
        assertThat(deleteSql(ContactTagMapper.class, "hardDeleteAll"))
                .contains(
                        "FROM contact_tag",
                        "user_id = #{userId}",
                        "user_type = #{userType}",
                        "friend_id = #{friendId}",
                        "friend_type = #{friendType}"
                );
    }

    @Test
    void listContactsResolvesTargetsWithTypedSubjectRefs() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(contactRelationshipMapper.findActiveFriendsByOwner(2L, ContactRelationship.RelationshipSubjectType.HUMAN)).thenReturn(List.of(
                ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build(),
                ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build()
        ));
        when(subjectDirectoryApi.batchGetSubjects(Set.of(SubjectRef.agent(101L), SubjectRef.human(101L))))
                .thenReturn(Map.of(
                        SubjectRef.agent(101L), activeSubject(101L, SubjectType.AGENT, "Nova"),
                        SubjectRef.human(101L), activeSubject(101L, SubjectType.HUMAN, "Human 101")
                ));
        when(contactTagMapper.selectActiveTagNames(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT))
                .thenReturn(List.of("agent"));
        when(contactTagMapper.selectActiveTagNames(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.HUMAN))
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
    void listContactsSupportsAgentOwnerSubject() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(contactRelationshipMapper.findActiveFriendsByOwner(101L, ContactRelationship.RelationshipSubjectType.AGENT)).thenReturn(List.of(
                ContactRelationship.builder()
                        .userId(101L)
                        .userType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .friendId(2L)
                        .friendType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build()
        ));
        when(subjectDirectoryApi.batchGetSubjects(Set.of(SubjectRef.human(2L))))
                .thenReturn(Map.of(SubjectRef.human(2L), activeSubject(2L, SubjectType.HUMAN, "Ava")));
        when(contactTagMapper.selectActiveTagNames(101L, ContactRelationship.RelationshipSubjectType.AGENT, 2L, ContactRelationship.RelationshipSubjectType.HUMAN))
                .thenReturn(List.of("owner-agent"));

        var contacts = service.listContacts(9L, SubjectRef.agent(101L));

        verify(contactRelationshipMapper).findActiveFriendsByOwner(101L, ContactRelationship.RelationshipSubjectType.AGENT);
        assertThat(contacts)
                .extracting("ownerSubjectId", "ownerSubjectType", "targetSubjectId", "targetSubjectType", "tags")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        101L,
                        SubjectType.AGENT,
                        2L,
                        SubjectType.HUMAN,
                        List.of("owner-agent")
                ));
    }

    @Test
    void relationshipApiListsCanonicalFriendSubjectRefs() {
        when(contactRelationshipMapper.findActiveFriendsByOwner(2L, ContactRelationship.RelationshipSubjectType.HUMAN)).thenReturn(List.of(
                ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build(),
                ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build(),
                ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build()
        ));

        assertThat(service.listFriends(SubjectRef.human(2L)))
                .containsExactly(SubjectRef.agent(101L), SubjectRef.human(101L));
    }

    @Test
    void updateContactTagsDeletesAndInsertsOnlyTheTypedTarget() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(2L))).thenReturn(LiabilityChain.selfResponsible(2L));
        when(contactRelationshipMapper.findByOwnerAndTarget(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT))
                .thenReturn(Optional.of(ContactRelationship.builder()
                        .userId(2L)
                        .userType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .friendId(101L)
                        .friendType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build()));

        var tags = service.updateContactTags(2L, SubjectRef.human(2L), SubjectRef.agent(101L),
                List.of("AI", "ai", " Research "));

        assertThat(tags).containsExactly("AI", "Research");
        verify(contactTagMapper)
                .hardDeleteAll(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT);
        ArgumentCaptor<ContactTag> tagCaptor = ArgumentCaptor.forClass(ContactTag.class);
        verify(contactTagMapper, org.mockito.Mockito.times(2)).insert(tagCaptor.capture());
        assertThat(tagCaptor.getAllValues())
                .extracting(
                        ContactTag::getUserId,
                        ContactTag::getUserType,
                        ContactTag::getFriendId,
                        ContactTag::getFriendType,
                        ContactTag::getTagName
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT, "AI"),
                        org.assertj.core.groups.Tuple.tuple(2L, ContactRelationship.RelationshipSubjectType.HUMAN, 101L, ContactRelationship.RelationshipSubjectType.AGENT, "Research")
                );
    }

    @Test
    void updateContactTagsScopesAgentOwnerSeparatelyFromHumanOwner() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(contactRelationshipMapper.findByOwnerAndTarget(101L, ContactRelationship.RelationshipSubjectType.AGENT, 2L, ContactRelationship.RelationshipSubjectType.HUMAN))
                .thenReturn(Optional.of(ContactRelationship.builder()
                        .userId(101L)
                        .userType(ContactRelationship.RelationshipSubjectType.AGENT)
                        .friendId(2L)
                        .friendType(ContactRelationship.RelationshipSubjectType.HUMAN)
                        .status(ContactRelationship.RelationshipStatus.ACTIVE)
                        .build()));

        var tags = service.updateContactTags(9L, SubjectRef.agent(101L), SubjectRef.human(2L), List.of("Agent Owner"));

        assertThat(tags).containsExactly("Agent Owner");
        verify(contactTagMapper)
                .hardDeleteAll(101L, ContactRelationship.RelationshipSubjectType.AGENT, 2L, ContactRelationship.RelationshipSubjectType.HUMAN);
        ArgumentCaptor<ContactTag> tagCaptor = ArgumentCaptor.forClass(ContactTag.class);
        verify(contactTagMapper).insert(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getUserId()).isEqualTo(101L);
        assertThat(tagCaptor.getValue().getUserType()).isEqualTo(ContactRelationship.RelationshipSubjectType.AGENT);
        assertThat(tagCaptor.getValue().getFriendId()).isEqualTo(2L);
        assertThat(tagCaptor.getValue().getFriendType()).isEqualTo(ContactRelationship.RelationshipSubjectType.HUMAN);
    }

    @Test
    void contactOperationsRejectUnauthorizedAgentOwnerLiability() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 8L));

        assertThatThrownBy(() -> service.listContacts(9L, SubjectRef.agent(101L)))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.access.denied");
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
