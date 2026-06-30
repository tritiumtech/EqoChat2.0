package com.eqochat.business.world.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.business.user.mapper.UserFriendMapper;
import com.eqochat.business.user.mapper.UserProfileMapper;
import com.eqochat.business.world.api.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.business.world.api.dto.request.CreateWorldPostRequest;
import com.eqochat.business.world.api.dto.response.WorldPostResponse;
import com.eqochat.business.world.config.WorldModuleProperties;
import com.eqochat.business.world.entity.WorldPost;
import com.eqochat.business.world.entity.WorldPostMention;
import com.eqochat.business.world.entity.WorldPostReply;
import com.eqochat.business.world.entity.WorldPostReplyUpvote;
import com.eqochat.business.world.entity.WorldPostUpvote;
import com.eqochat.business.world.entity.WorldTopic;
import com.eqochat.business.world.entity.WorldTopicFollow;
import com.eqochat.business.world.mapper.WorldPostMapper;
import com.eqochat.business.world.mapper.WorldPostMentionMapper;
import com.eqochat.business.world.mapper.WorldPostReplyMapper;
import com.eqochat.business.world.mapper.WorldPostReplyUpvoteMapper;
import com.eqochat.business.world.mapper.WorldPostTopicMapper;
import com.eqochat.business.world.mapper.WorldPostUpvoteMapper;
import com.eqochat.business.world.mapper.WorldTopicFollowMapper;
import com.eqochat.business.world.mapper.WorldTopicMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldServiceImplActorAuthorTest {

    @Mock
    WorldPostMapper worldPostMapper;
    @Mock
    WorldPostMentionMapper worldPostMentionMapper;
    @Mock
    WorldTopicMapper worldTopicMapper;
    @Mock
    WorldPostTopicMapper worldPostTopicMapper;
    @Mock
    WorldPostUpvoteMapper worldPostUpvoteMapper;
    @Mock
    WorldTopicFollowMapper worldTopicFollowMapper;
    @Mock
    WorldPostReplyMapper worldPostReplyMapper;
    @Mock
    WorldPostReplyUpvoteMapper worldPostReplyUpvoteMapper;
    @Mock
    UserProfileMapper userProfileMapper;
    @Mock
    UserFriendMapper userFriendMapper;
    @Mock
    NotificationService notificationService;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;
    @Mock
    WalletPolicyApi walletPolicyApi;

    WorldServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WorldServiceImpl(
                worldPostMapper,
                worldPostMentionMapper,
                worldTopicMapper,
                worldPostTopicMapper,
                worldPostUpvoteMapper,
                worldTopicFollowMapper,
                worldPostReplyMapper,
                worldPostReplyUpvoteMapper,
                userProfileMapper,
                userFriendMapper,
                notificationService,
                new WorldModuleProperties(),
                subjectDirectoryApi,
                liabilityPolicyApi,
                walletPolicyApi
        );
    }

    @Test
    void humanPostCreationPersistsCanonicalAuthorType() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(2L);
        request.setActorSubjectType(SubjectType.HUMAN);
        request.setContent("hello world");
        when(subjectDirectoryApi.getSubject(SubjectRef.human(2L)))
                .thenReturn(SubjectSummaryResponse.builder()
                        .id(2L)
                        .type(SubjectType.HUMAN)
                        .displayName("John")
                        .build());
        doAnswer(invocation -> {
            WorldPost post = invocation.getArgument(0);
            post.setId(100L);
            post.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(worldPostMapper).insert(any(WorldPost.class));

        WorldPostResponse response = service.createPost(2L, request);

        ArgumentCaptor<WorldPost> captor = ArgumentCaptor.forClass(WorldPost.class);
        org.mockito.Mockito.verify(worldPostMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(2L);
        assertThat(captor.getValue().getAuthorType()).isEqualTo("HUMAN");
        assertThat(response.getAuthor().getType()).isEqualTo("human");
        assertThat(response.getAuthor().isAi()).isFalse();
    }

    @Test
    void replyCreationPersistsCanonicalAuthorType() {
        WorldPost existing = WorldPost.builder()
                .id(200L)
                .authorId(101L)
                .authorType("AGENT")
                .replyCount(0)
                .delToken(0L)
                .build();
        WorldPost updated = WorldPost.builder()
                .id(200L)
                .replyCount(1)
                .delToken(0L)
                .build();
        when(worldPostMapper.selectById(200L)).thenReturn(existing, updated);
        doAnswer(invocation -> {
            WorldPostReply reply = invocation.getArgument(0);
            reply.setId(300L);
            return 1;
        }).when(worldPostReplyMapper).insert(any(WorldPostReply.class));
        CreateWorldPostReplyRequest request = new CreateWorldPostReplyRequest();
        request.setActorSubjectId(2L);
        request.setActorSubjectType(SubjectType.HUMAN);
        request.setContent("reply");

        int count = service.createReply(2L, 200L, request);

        ArgumentCaptor<WorldPostReply> captor = ArgumentCaptor.forClass(WorldPostReply.class);
        org.mockito.Mockito.verify(worldPostReplyMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(2L);
        assertThat(captor.getValue().getAuthorType()).isEqualTo("HUMAN");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void authorizedAgentPostCreationPersistsAgentAuthorAndNotificationSender() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);
        request.setContent("@Ada hello from Nova");
        request.setMentionedSubjects(List.of(mention(3L, SubjectType.HUMAN)));

        SubjectRef actor = SubjectRef.agent(101L);
        SubjectRef mentioned = SubjectRef.human(3L);
        when(liabilityPolicyApi.resolveLiability(actor)).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        when(subjectDirectoryApi.batchGetSubjects(any()))
                .thenReturn(Map.of(mentioned, SubjectSummaryResponse.builder().id(3L).type(SubjectType.HUMAN).build()));
        when(subjectDirectoryApi.getSubject(actor))
                .thenReturn(SubjectSummaryResponse.builder()
                        .id(101L)
                        .type(SubjectType.AGENT)
                        .displayName("Nova")
                        .associatedHumanId(2L)
                        .associatedHumanName("John")
                        .build());
        doAnswer(invocation -> {
            WorldPost post = invocation.getArgument(0);
            post.setId(10100L);
            post.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(worldPostMapper).insert(any(WorldPost.class));

        WorldPostResponse response = service.createPost(2L, request);

        ArgumentCaptor<WorldPost> postCaptor = ArgumentCaptor.forClass(WorldPost.class);
        verify(worldPostMapper).insert(postCaptor.capture());
        assertThat(postCaptor.getValue().getAuthorId()).isEqualTo(101L);
        assertThat(postCaptor.getValue().getAuthorType()).isEqualTo("AGENT");
        assertThat(response.getAuthor().getId()).isEqualTo(101L);
        assertThat(response.getAuthor().getType()).isEqualTo("agent");
        assertThat(response.getAuthor().isAi()).isTrue();
        verify(notificationService).sendNotification(
                eq(mentioned),
                eq("MESSAGE_MENTION"),
                eq("You were mentioned in a post"),
                any(),
                eq("{\"postId\":10100}"),
                eq(actor)
        );
    }

    @Test
    void authorizedAgentReplyCreationPersistsAgentAuthor() {
        WorldPost existing = WorldPost.builder()
                .id(200L)
                .authorId(2L)
                .authorType("HUMAN")
                .replyCount(0)
                .delToken(0L)
                .build();
        WorldPost updated = WorldPost.builder()
                .id(200L)
                .replyCount(1)
                .delToken(0L)
                .build();
        when(worldPostMapper.selectById(200L)).thenReturn(existing, updated);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        doAnswer(invocation -> {
            WorldPostReply reply = invocation.getArgument(0);
            reply.setId(300L);
            return 1;
        }).when(worldPostReplyMapper).insert(any(WorldPostReply.class));
        CreateWorldPostReplyRequest request = new CreateWorldPostReplyRequest();
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);
        request.setContent("agent reply");

        int count = service.createReply(2L, 200L, request);

        ArgumentCaptor<WorldPostReply> captor = ArgumentCaptor.forClass(WorldPostReply.class);
        verify(worldPostReplyMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(101L);
        assertThat(captor.getValue().getAuthorType()).isEqualTo("AGENT");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void agentPostCreationRejectsLiabilityForAnotherHuman() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);
        request.setContent("not allowed");
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));

        assertThatThrownBy(() -> service.createPost(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.forbidden");
    }

    @Test
    void createPostRejectsSystemActor() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(1L);
        request.setActorSubjectType(SubjectType.SYSTEM);
        request.setContent("system");

        assertThatThrownBy(() -> service.createPost(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.invalid");
    }

    @Test
    void mentionSelfFilterUsesFullSubjectRefNotNumericIdOnly() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);
        request.setContent("@Human101 @Nova");
        request.setMentionedSubjects(List.of(
                mention(101L, SubjectType.HUMAN),
                mention(101L, SubjectType.AGENT)
        ));
        SubjectRef humanSameId = SubjectRef.human(101L);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        when(subjectDirectoryApi.batchGetSubjects(any()))
                .thenReturn(Map.of(humanSameId, SubjectSummaryResponse.builder().id(101L).type(SubjectType.HUMAN).build()));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L)))
                .thenReturn(SubjectSummaryResponse.builder().id(101L).type(SubjectType.AGENT).displayName("Nova").build());
        doAnswer(invocation -> {
            WorldPost post = invocation.getArgument(0);
            post.setId(10101L);
            post.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(worldPostMapper).insert(any(WorldPost.class));

        service.createPost(2L, request);

        ArgumentCaptor<WorldPostMention> captor = ArgumentCaptor.forClass(WorldPostMention.class);
        verify(worldPostMentionMapper).insert(captor.capture());
        assertThat(captor.getValue().getMentionedSubjectId()).isEqualTo(101L);
        assertThat(captor.getValue().getMentionedSubjectType()).isEqualTo("HUMAN");
    }

    @Test
    void mapperSqlUsesStoredAuthorTypeWithoutAgentProfileInference() throws Exception {
        String selectFeed = selectSql("selectFeed");
        String selectByAuthor = selectSql("selectPostsByAuthor");
        String selectMyPosts = selectSql("selectMyPosts");

        assertThat(selectFeed).doesNotContain("COALESCE(p.author_type");
        assertThat(selectFeed).doesNotContain("CASE WHEN ap.id IS NULL");
        assertThat(selectFeed).contains("p.author_type AS author_type");
        assertThat(selectFeed).contains("p.author_type = 'HUMAN'");
        assertThat(selectFeed).contains("p.author_type = 'AGENT'");
        assertThat(selectFeed).contains("uf.user_id = #{viewerId}");
        assertThat(selectFeed).contains("uf.user_type = 'HUMAN'");
        assertThat(selectFeed).contains("uf.friend_type = p.author_type");
        assertThat(selectFeed).contains("up.voter_id = #{viewerId}");
        assertThat(selectFeed).contains("up.voter_type = 'HUMAN'");
        assertThat(selectFeed).contains("tf.follower_id = #{viewerId}");
        assertThat(selectFeed).contains("tf.follower_type = 'HUMAN'");
        assertThat(selectByAuthor).contains("p.author_type = #{authorType}");
        assertThat(selectMyPosts).contains("p.author_type = 'HUMAN'");
    }

    @Test
    void engagementMapperSqlUsesSubjectColumns() throws Exception {
        String postUpvote = selectSql(WorldPostUpvoteMapper.class, "findActive");
        String replyUpvote = selectSql(WorldPostReplyUpvoteMapper.class, "findActive");
        String topicFollow = selectSql(WorldTopicFollowMapper.class, "findActive");
        String topicList = selectSql(WorldTopicMapper.class, "selectTopTopicsWithCursor");
        String mentionList = selectSql(WorldPostMentionMapper.class, "selectMentionedSubjectsByPostId");
        String mentionFeed = selectSql("selectMentionFeed");

        assertThat(postUpvote).contains("voter_id = #{voterId}", "voter_type = #{voterType}");
        assertThat(postUpvote).doesNotContain("user_id =");
        assertThat(replyUpvote).contains("voter_id = #{voterId}", "voter_type = #{voterType}");
        assertThat(replyUpvote).doesNotContain("user_id =");
        assertThat(topicFollow).contains("follower_id = #{followerId}", "follower_type = #{followerType}");
        assertThat(topicFollow).doesNotContain("user_id =");
        assertThat(topicList).contains("f.follower_id = #{viewerId}", "f.follower_type = 'HUMAN'");
        assertThat(mentionList).contains("mentioned_subject_id", "mentioned_subject_type");
        assertThat(mentionList).doesNotContain("mentioned_user_id");
        assertThat(mentionFeed).contains("m.mentioned_subject_id = #{viewerId}");
        assertThat(mentionFeed).contains("m.mentioned_subject_type = 'HUMAN'");
        assertThat(mentionFeed).doesNotContain("mentioned_user_id");
    }

    @Test
    void createPostPersistsCanonicalMentionSubjects() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(2L);
        request.setActorSubjectType(SubjectType.HUMAN);
        request.setContent("@Ada @Nova hello");
        request.setMentionedSubjects(List.of(
                mention(3L, SubjectType.HUMAN),
                mention(101L, SubjectType.AGENT)
        ));
        SubjectRef human = SubjectRef.human(3L);
        SubjectRef agent = SubjectRef.agent(101L);
        when(subjectDirectoryApi.batchGetSubjects(any()))
                .thenReturn(Map.of(
                        human, SubjectSummaryResponse.builder().id(3L).type(SubjectType.HUMAN).build(),
                        agent, SubjectSummaryResponse.builder().id(101L).type(SubjectType.AGENT).build()
                ));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(2L)))
                .thenReturn(SubjectSummaryResponse.builder()
                        .id(2L)
                        .type(SubjectType.HUMAN)
                        .displayName("John")
                        .build());
        doAnswer(invocation -> {
            WorldPost post = invocation.getArgument(0);
            post.setId(100L);
            post.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(worldPostMapper).insert(any(WorldPost.class));

        service.createPost(2L, request);

        ArgumentCaptor<WorldPostMention> captor = ArgumentCaptor.forClass(WorldPostMention.class);
        verify(worldPostMentionMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(WorldPostMention::getMentionedSubjectId, WorldPostMention::getMentionedSubjectType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(3L, "HUMAN"),
                        org.assertj.core.groups.Tuple.tuple(101L, "AGENT")
                );
        verify(notificationService).sendNotification(
                eq(human),
                eq("MESSAGE_MENTION"),
                eq("You were mentioned in a post"),
                any(),
                eq("{\"postId\":100}"),
                eq(SubjectRef.human(2L))
        );
        verify(notificationService).sendNotification(
                eq(agent),
                eq("MESSAGE_MENTION"),
                eq("You were mentioned in a post"),
                any(),
                eq("{\"postId\":100}"),
                eq(SubjectRef.human(2L))
        );
    }

    @Test
    void createPostRejectsSystemMentionSubject() {
        CreateWorldPostRequest request = new CreateWorldPostRequest();
        request.setActorSubjectId(2L);
        request.setActorSubjectType(SubjectType.HUMAN);
        request.setContent("hello @system");
        request.setMentionedSubjects(List.of(mention(0L, SubjectType.SYSTEM)));
        doAnswer(invocation -> {
            WorldPost post = invocation.getArgument(0);
            post.setId(100L);
            post.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(worldPostMapper).insert(any(WorldPost.class));

        assertThatThrownBy(() -> service.createPost(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("world.mention.subject_type.invalid");
    }

    @Test
    void engagementWritesUseHumanSubjectFields() {
        when(worldPostMapper.selectById(10L)).thenReturn(WorldPost.builder().id(10L).delToken(0L).build());
        when(worldPostUpvoteMapper.findActive(10L, 2L, "HUMAN")).thenReturn(null);
        when(worldPostUpvoteMapper.findAny(10L, 2L, "HUMAN")).thenReturn(null);

        assertThat(service.toggleUpvote(2L, 10L)).isTrue();

        ArgumentCaptor<WorldPostUpvote> postUpvote = ArgumentCaptor.forClass(WorldPostUpvote.class);
        verify(worldPostUpvoteMapper).insert(postUpvote.capture());
        assertThat(postUpvote.getValue().getVoterId()).isEqualTo(2L);
        assertThat(postUpvote.getValue().getVoterType()).isEqualTo("HUMAN");

        when(worldPostReplyMapper.selectById(11L)).thenReturn(WorldPostReply.builder().id(11L).delToken(0L).build());
        when(worldPostReplyUpvoteMapper.findActive(11L, 2L, "HUMAN")).thenReturn(null);
        when(worldPostReplyUpvoteMapper.findAny(11L, 2L, "HUMAN")).thenReturn(null);

        assertThat(service.toggleReplyUpvote(2L, 11L)).isTrue();

        ArgumentCaptor<WorldPostReplyUpvote> replyUpvote = ArgumentCaptor.forClass(WorldPostReplyUpvote.class);
        verify(worldPostReplyUpvoteMapper).insert(replyUpvote.capture());
        assertThat(replyUpvote.getValue().getVoterId()).isEqualTo(2L);
        assertThat(replyUpvote.getValue().getVoterType()).isEqualTo("HUMAN");

        when(worldTopicMapper.selectByName("java")).thenReturn(WorldTopic.builder().id(12L).name("java").build());
        when(worldTopicFollowMapper.findActive(12L, 2L, "HUMAN")).thenReturn(null);
        when(worldTopicFollowMapper.findAny(12L, 2L, "HUMAN")).thenReturn(null);

        assertThat(service.toggleTopicFollow(2L, "java")).isTrue();

        ArgumentCaptor<WorldTopicFollow> topicFollow = ArgumentCaptor.forClass(WorldTopicFollow.class);
        verify(worldTopicFollowMapper).insert(topicFollow.capture());
        assertThat(topicFollow.getValue().getFollowerId()).isEqualTo(2L);
        assertThat(topicFollow.getValue().getFollowerType()).isEqualTo("HUMAN");
    }

    @Test
    void agentPostRowRendersCanonicalAgentAuthor() {
        WorldPostMapper.WorldPostRow row = row(10L, 101L, "AGENT");
        when(worldPostMapper.selectFeed(2L, "friends", null, 21)).thenReturn(List.of(row));
        when(worldPostMapper.selectTopicNamesByPostId(10L)).thenReturn(List.of());
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L)))
                .thenReturn(SubjectSummaryResponse.builder()
                        .id(101L)
                        .type(SubjectType.AGENT)
                        .displayName("Nova")
                        .avatarUrl("#7C3AED")
                        .associatedHumanId(2L)
                        .associatedHumanName("John")
                        .build());
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(101L)))
                .thenReturn(WalletCapability.agentToOwner(101L, 2L, "owner wallet route"));

        WorldPostResponse item = service.listFeed(2L, "friends", null, null).getItems().get(0);

        assertThat(item.getAuthor().getId()).isEqualTo(101L);
        assertThat(item.getAuthor().getType()).isEqualTo("agent");
        assertThat(item.getAuthor().isAi()).isTrue();
        assertThat(item.getAuthor().getAssociatedHumanId()).isEqualTo(2L);
    }

    @Test
    void listPostsByAuthorScopesFriendCheckByHumanViewerSubjectType() {
        when(userFriendMapper.areFriends(2L, com.eqochat.business.user.entity.UserFriend.FriendType.HUMAN,
                101L, com.eqochat.business.user.entity.UserFriend.FriendType.AGENT))
                .thenReturn(true);

        service.listPostsByAuthor(2L, 101L, "AGENT", null, 20);

        verify(userFriendMapper).areFriends(2L, com.eqochat.business.user.entity.UserFriend.FriendType.HUMAN,
                101L, com.eqochat.business.user.entity.UserFriend.FriendType.AGENT);
    }

    @Test
    void legacyUserAuthorTypeIsRejected() {
        WorldPostMapper.WorldPostRow row = row(10L, 2L, "USER");
        when(worldPostMapper.selectFeed(2L, "friends", null, 21)).thenReturn(List.of(row));
        when(worldPostMapper.selectTopicNamesByPostId(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.listFeed(2L, "friends", null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("world.author.type.invalid");
    }

    private static WorldPostMapper.WorldPostRow row(Long id, Long authorId, String authorType) {
        WorldPostMapper.WorldPostRow row = new WorldPostMapper.WorldPostRow();
        row.setId(id);
        row.setAuthorId(authorId);
        row.setAuthorType(authorType);
        row.setAuthorName(authorType + " author");
        row.setContent("content");
        row.setMediaType("TEXT");
        row.setReplyCount(0);
        row.setUpvoteCount(0);
        row.setIsFriend(0);
        row.setIsUpvoted(0);
        row.setCreateTime(LocalDateTime.now());
        return row;
    }

    private static String selectSql(String methodName) throws Exception {
        return selectSql(WorldPostMapper.class, methodName);
    }

    private static String selectSql(Class<?> mapperType, String methodName) throws Exception {
        for (Method method : mapperType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Select select = method.getAnnotation(Select.class);
                return String.join("\n", select.value());
            }
        }
        throw new IllegalArgumentException("method not found: " + methodName);
    }

    private static CreateWorldPostRequest.MentionedSubject mention(Long id, SubjectType type) {
        CreateWorldPostRequest.MentionedSubject subject = new CreateWorldPostRequest.MentionedSubject();
        subject.setSubjectId(id);
        subject.setSubjectType(type);
        return subject;
    }
}
