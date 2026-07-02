package com.eqochat.business.world.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.business.contact.api.service.SubjectRelationshipApi;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.business.world.api.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.business.world.api.dto.request.CreateWorldPostRequest;
import com.eqochat.business.world.api.dto.response.WorldPostResponse;
import com.eqochat.business.world.config.WorldModuleProperties;
import com.eqochat.business.world.WorldService;
import com.eqochat.business.world.WorldUploadService;
import com.eqochat.business.world.controller.world.WorldController;
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
import com.eqochat.framework.common.PageResponse;
import com.eqochat.framework.common.UserContext;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
    SubjectRelationshipApi subjectRelationshipApi;
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
                subjectRelationshipApi,
                notificationService,
                new WorldModuleProperties(),
                subjectDirectoryApi,
                liabilityPolicyApi,
                walletPolicyApi
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
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
    void mapperSqlUsesRegistryOnlyAuthorIdentityProjection() throws Exception {
        String selectFeed = selectSql("selectFeed");
        String selectByAuthor = selectSql("selectPostsByAuthor");
        String selectMyPosts = selectSql("selectMyPosts");

        assertThat(selectFeed).doesNotContain("COALESCE(p.author_type");
        assertThat(selectFeed).doesNotContain("CASE WHEN ap.id IS NULL");
        assertThat(selectFeed).contains("LEFT JOIN subject_registry sr");
        assertThat(selectFeed).contains("sr.subject_id = p.author_id");
        assertThat(selectFeed).contains("sr.subject_type = p.author_type");
        assertThat(selectFeed).contains("COALESCE(sr.display_name, CONCAT('AGENT:', p.author_id))");
        assertThat(selectFeed).contains("COALESCE(sr.display_name, CONCAT('HUMAN:', p.author_id))");
        assertThat(selectFeed).contains("sr.avatar_url AS author_avatar_url");
        assertThat(selectFeed).contains("sr.associated_human_id AS author_owner_id");
        assertThat(selectFeed).contains("LEFT JOIN subject_registry owner_sr");
        assertThat(selectFeed).contains("owner_sr.subject_id = sr.associated_human_id");
        assertThat(selectFeed).doesNotContain("LEFT JOIN user_profile");
        assertThat(selectFeed).doesNotContain("LEFT JOIN agent_profile");
        assertThat(selectFeed).doesNotContain("ap.owner_id");
        assertThat(selectFeed).doesNotContain("owner.nickname");
        assertThat(selectFeed).contains("p.author_type AS author_type");
        assertThat(selectFeed).contains("p.author_type = 'AGENT'");
        assertThat(selectFeed).doesNotContain("user_friend");
        assertThat(selectFeed).doesNotContain("uf.");
        assertThat(selectFeed).contains("friendHumanIds");
        assertThat(selectFeed).contains("friendAgentIds");
        assertThat(selectFeed).contains("up.voter_id = #{viewerId}");
        assertThat(selectFeed).contains("up.voter_type = #{viewerType}");
        assertThat(selectFeed).contains("tf.follower_id = #{viewerId}");
        assertThat(selectFeed).contains("tf.follower_type = #{viewerType}");
        assertThat(selectByAuthor).contains("p.author_type = #{authorType}");
        assertThat(selectMyPosts).contains("p.author_type = #{viewerType}");
    }

    @Test
    void engagementMapperSqlUsesSubjectColumns() throws Exception {
        String postUpvote = selectSql(WorldPostUpvoteMapper.class, "findActive");
        String replyUpvote = selectSql(WorldPostReplyUpvoteMapper.class, "findActive");
        String replyUpvotesByViewer = selectSql(WorldPostReplyUpvoteMapper.class, "selectActiveByVoterAndReplyIds");
        String topicFollow = selectSql(WorldTopicFollowMapper.class, "findActive");
        String topicList = selectSql(WorldTopicMapper.class, "selectTopTopicsWithCursor");
        String mentionList = selectSql(WorldPostMentionMapper.class, "selectMentionedSubjectsByPostId");
        String mentionFeed = selectSql("selectMentionFeed");

        assertThat(postUpvote).contains("voter_id = #{voterId}", "voter_type = #{voterType}");
        assertThat(postUpvote).doesNotContain("user_id =");
        assertThat(replyUpvote).contains("voter_id = #{voterId}", "voter_type = #{voterType}");
        assertThat(replyUpvote).doesNotContain("user_id =");
        assertThat(replyUpvotesByViewer).contains("voter_id = #{voterId}", "voter_type = #{voterType}");
        assertThat(replyUpvotesByViewer).doesNotContain("user_id =");
        assertThat(topicFollow).contains("follower_id = #{followerId}", "follower_type = #{followerType}");
        assertThat(topicFollow).doesNotContain("user_id =");
        assertThat(topicList).contains("f.follower_id = #{viewerId}", "f.follower_type = #{viewerType}");
        assertThat(mentionList).contains("mentioned_subject_id", "mentioned_subject_type");
        assertThat(mentionList).doesNotContain("mentioned_user_id");
        assertThat(mentionFeed).contains("m.mentioned_subject_id = #{viewerId}");
        assertThat(mentionFeed).contains("m.mentioned_subject_type = #{viewerType}");
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

        assertThat(service.toggleUpvote(2L, SubjectRef.human(2L), 10L)).isTrue();

        ArgumentCaptor<WorldPostUpvote> postUpvote = ArgumentCaptor.forClass(WorldPostUpvote.class);
        verify(worldPostUpvoteMapper).insert(postUpvote.capture());
        assertThat(postUpvote.getValue().getVoterId()).isEqualTo(2L);
        assertThat(postUpvote.getValue().getVoterType()).isEqualTo("HUMAN");

        when(worldPostReplyMapper.selectById(11L)).thenReturn(WorldPostReply.builder().id(11L).delToken(0L).build());
        when(worldPostReplyUpvoteMapper.findActive(11L, 2L, "HUMAN")).thenReturn(null);
        when(worldPostReplyUpvoteMapper.findAny(11L, 2L, "HUMAN")).thenReturn(null);

        assertThat(service.toggleReplyUpvote(2L, SubjectRef.human(2L), 11L)).isTrue();

        ArgumentCaptor<WorldPostReplyUpvote> replyUpvote = ArgumentCaptor.forClass(WorldPostReplyUpvote.class);
        verify(worldPostReplyUpvoteMapper).insert(replyUpvote.capture());
        assertThat(replyUpvote.getValue().getVoterId()).isEqualTo(2L);
        assertThat(replyUpvote.getValue().getVoterType()).isEqualTo("HUMAN");

        when(worldTopicMapper.selectByName("java")).thenReturn(WorldTopic.builder().id(12L).name("java").build());
        when(worldTopicFollowMapper.findActive(12L, 2L, "HUMAN")).thenReturn(null);
        when(worldTopicFollowMapper.findAny(12L, 2L, "HUMAN")).thenReturn(null);

        assertThat(service.toggleTopicFollow(2L, SubjectRef.human(2L), "java")).isTrue();

        ArgumentCaptor<WorldTopicFollow> topicFollow = ArgumentCaptor.forClass(WorldTopicFollow.class);
        verify(worldTopicFollowMapper).insert(topicFollow.capture());
        assertThat(topicFollow.getValue().getFollowerId()).isEqualTo(2L);
        assertThat(topicFollow.getValue().getFollowerType()).isEqualTo("HUMAN");
    }

    @Test
    void agentViewerReadPathsPassCanonicalSubjectTypeToMappers() {
        SubjectRef viewer = SubjectRef.agent(101L);
        when(liabilityPolicyApi.resolveLiability(viewer)).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        when(subjectRelationshipApi.listFriends(viewer)).thenReturn(List.of());
        when(worldPostMapper.selectFeed(101L, "AGENT", "friends", List.of(), List.of(), null, 21)).thenReturn(List.of());
        when(worldTopicMapper.selectTopTopicsWithCursor(101L, "AGENT", null, 21)).thenReturn(List.of());
        when(worldPostMapper.selectTopicPosts(101L, "AGENT", "java", null, 21)).thenReturn(List.of());
        when(worldPostMapper.selectMentionFeed(101L, "AGENT", null, 21)).thenReturn(List.of());
        when(worldPostMapper.selectMyPosts(101L, "AGENT", null, 21)).thenReturn(List.of());
        WorldPostReply reply = WorldPostReply.builder()
                .id(31L)
                .authorId(2L)
                .authorType("HUMAN")
                .content("reply")
                .upvoteCount(1)
                .delToken(0L)
                .build();
        when(worldPostReplyMapper.selectList(any())).thenReturn(List.of(reply));
        when(worldPostReplyUpvoteMapper.selectActiveByVoterAndReplyIds(101L, "AGENT", List.of(31L)))
                .thenReturn(List.of(WorldPostReplyUpvote.builder().replyId(31L).voterId(101L).voterType("AGENT").build()));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(2L)))
                .thenReturn(SubjectSummaryResponse.builder().id(2L).type(SubjectType.HUMAN).displayName("Ava").build());

        service.listFeed(2L, viewer, "friends", null, null);
        service.listTopics(2L, viewer, 20, null);
        service.listTopicPosts(2L, viewer, "java", null, null);
        service.listMentionedMe(2L, viewer, null, null);
        service.listMyPosts(2L, viewer, null, null);
        var replies = service.listReplies(2L, viewer, 30L, null, null);

        verify(worldPostMapper).selectFeed(101L, "AGENT", "friends", List.of(), List.of(), null, 21);
        verify(worldTopicMapper).selectTopTopicsWithCursor(101L, "AGENT", null, 21);
        verify(worldPostMapper).selectTopicPosts(101L, "AGENT", "java", null, 21);
        verify(worldPostMapper).selectMentionFeed(101L, "AGENT", null, 21);
        verify(worldPostMapper).selectMyPosts(101L, "AGENT", null, 21);
        verify(worldPostReplyUpvoteMapper).selectActiveByVoterAndReplyIds(101L, "AGENT", List.of(31L));
        assertThat(replies).hasSize(1);
        assertThat(replies.get(0).isUpvoted()).isTrue();
    }

    @Test
    void agentEngagementWritesUseAgentSubjectFields() {
        SubjectRef actor = SubjectRef.agent(101L);
        when(liabilityPolicyApi.resolveLiability(actor)).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        when(worldPostMapper.selectById(10L)).thenReturn(WorldPost.builder().id(10L).delToken(0L).build());
        when(worldPostUpvoteMapper.findActive(10L, 101L, "AGENT")).thenReturn(null);
        when(worldPostUpvoteMapper.findAny(10L, 101L, "AGENT")).thenReturn(null);

        assertThat(service.toggleUpvote(2L, actor, 10L)).isTrue();

        ArgumentCaptor<WorldPostUpvote> postUpvote = ArgumentCaptor.forClass(WorldPostUpvote.class);
        verify(worldPostUpvoteMapper).insert(postUpvote.capture());
        assertThat(postUpvote.getValue().getVoterId()).isEqualTo(101L);
        assertThat(postUpvote.getValue().getVoterType()).isEqualTo("AGENT");

        when(worldPostReplyMapper.selectById(11L)).thenReturn(WorldPostReply.builder().id(11L).delToken(0L).build());
        when(worldPostReplyUpvoteMapper.findActive(11L, 101L, "AGENT")).thenReturn(null);
        when(worldPostReplyUpvoteMapper.findAny(11L, 101L, "AGENT")).thenReturn(null);

        assertThat(service.toggleReplyUpvote(2L, actor, 11L)).isTrue();

        ArgumentCaptor<WorldPostReplyUpvote> replyUpvote = ArgumentCaptor.forClass(WorldPostReplyUpvote.class);
        verify(worldPostReplyUpvoteMapper).insert(replyUpvote.capture());
        assertThat(replyUpvote.getValue().getVoterId()).isEqualTo(101L);
        assertThat(replyUpvote.getValue().getVoterType()).isEqualTo("AGENT");

        when(worldTopicMapper.selectByName("java")).thenReturn(WorldTopic.builder().id(12L).name("java").build());
        when(worldTopicFollowMapper.findActive(12L, 101L, "AGENT")).thenReturn(null);
        when(worldTopicFollowMapper.findAny(12L, 101L, "AGENT")).thenReturn(null);

        assertThat(service.toggleTopicFollow(2L, actor, "java")).isTrue();

        ArgumentCaptor<WorldTopicFollow> topicFollow = ArgumentCaptor.forClass(WorldTopicFollow.class);
        verify(worldTopicFollowMapper).insert(topicFollow.capture());
        assertThat(topicFollow.getValue().getFollowerId()).isEqualTo(101L);
        assertThat(topicFollow.getValue().getFollowerType()).isEqualTo("AGENT");
    }

    @Test
    void unauthorizedAgentViewerAndActorAreRejected() {
        SubjectRef agent = SubjectRef.agent(101L);
        when(liabilityPolicyApi.resolveLiability(agent)).thenReturn(LiabilityChain.agentToHuman(101L, 9L));

        assertThatThrownBy(() -> service.listFeed(2L, agent, "friends", null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.forbidden");

        assertThatThrownBy(() -> service.toggleUpvote(2L, agent, 10L))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.forbidden");
    }

    @Test
    void systemViewerAndActorAreRejected() {
        SubjectRef system = SubjectRef.system(0L);

        assertThatThrownBy(() -> service.listMentionedMe(2L, system, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.invalid");

        assertThatThrownBy(() -> service.toggleReplyUpvote(2L, system, 11L))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.invalid");
    }

    @Test
    void sameNumericHumanAndAgentEngagementDoNotCollide() {
        when(worldPostMapper.selectById(20L)).thenReturn(WorldPost.builder().id(20L).delToken(0L).build());
        when(worldPostUpvoteMapper.findActive(20L, 101L, "HUMAN")).thenReturn(null);
        when(worldPostUpvoteMapper.findAny(20L, 101L, "HUMAN")).thenReturn(null);
        when(worldPostUpvoteMapper.findActive(20L, 101L, "AGENT")).thenReturn(null);
        when(worldPostUpvoteMapper.findAny(20L, 101L, "AGENT")).thenReturn(null);
        when(worldPostReplyMapper.selectById(21L)).thenReturn(WorldPostReply.builder().id(21L).delToken(0L).build());
        when(worldPostReplyUpvoteMapper.findActive(21L, 101L, "HUMAN")).thenReturn(null);
        when(worldPostReplyUpvoteMapper.findAny(21L, 101L, "HUMAN")).thenReturn(null);
        when(worldPostReplyUpvoteMapper.findActive(21L, 101L, "AGENT")).thenReturn(null);
        when(worldPostReplyUpvoteMapper.findAny(21L, 101L, "AGENT")).thenReturn(null);
        when(worldTopicMapper.selectByName("java")).thenReturn(WorldTopic.builder().id(22L).name("java").build());
        when(worldTopicFollowMapper.findActive(22L, 101L, "HUMAN")).thenReturn(null);
        when(worldTopicFollowMapper.findAny(22L, 101L, "HUMAN")).thenReturn(null);
        when(worldTopicFollowMapper.findActive(22L, 101L, "AGENT")).thenReturn(null);
        when(worldTopicFollowMapper.findAny(22L, 101L, "AGENT")).thenReturn(null);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 2L));

        assertThat(service.toggleUpvote(101L, SubjectRef.human(101L), 20L)).isTrue();
        assertThat(service.toggleUpvote(2L, SubjectRef.agent(101L), 20L)).isTrue();
        assertThat(service.toggleReplyUpvote(101L, SubjectRef.human(101L), 21L)).isTrue();
        assertThat(service.toggleReplyUpvote(2L, SubjectRef.agent(101L), 21L)).isTrue();
        assertThat(service.toggleTopicFollow(101L, SubjectRef.human(101L), "java")).isTrue();
        assertThat(service.toggleTopicFollow(2L, SubjectRef.agent(101L), "java")).isTrue();

        ArgumentCaptor<WorldPostUpvote> captor = ArgumentCaptor.forClass(WorldPostUpvote.class);
        verify(worldPostUpvoteMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(WorldPostUpvote::getVoterId, WorldPostUpvote::getVoterType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, "HUMAN"),
                        org.assertj.core.groups.Tuple.tuple(101L, "AGENT")
                );

        ArgumentCaptor<WorldPostReplyUpvote> replyCaptor = ArgumentCaptor.forClass(WorldPostReplyUpvote.class);
        verify(worldPostReplyUpvoteMapper, times(2)).insert(replyCaptor.capture());
        assertThat(replyCaptor.getAllValues())
                .extracting(WorldPostReplyUpvote::getVoterId, WorldPostReplyUpvote::getVoterType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, "HUMAN"),
                        org.assertj.core.groups.Tuple.tuple(101L, "AGENT")
                );

        ArgumentCaptor<WorldTopicFollow> followCaptor = ArgumentCaptor.forClass(WorldTopicFollow.class);
        verify(worldTopicFollowMapper, times(2)).insert(followCaptor.capture());
        assertThat(followCaptor.getAllValues())
                .extracting(WorldTopicFollow::getFollowerId, WorldTopicFollow::getFollowerType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, "HUMAN"),
                        org.assertj.core.groups.Tuple.tuple(101L, "AGENT")
                );
    }

    @Test
    void sameNumericHumanAndAgentReadPathsDoNotCollideForMentionsAndMyPosts() {
        when(worldPostMapper.selectMentionFeed(101L, "HUMAN", null, 21)).thenReturn(List.of());
        when(worldPostMapper.selectMyPosts(101L, "HUMAN", null, 21)).thenReturn(List.of());
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        when(worldPostMapper.selectMentionFeed(101L, "AGENT", null, 21)).thenReturn(List.of());
        when(worldPostMapper.selectMyPosts(101L, "AGENT", null, 21)).thenReturn(List.of());

        service.listMentionedMe(101L, SubjectRef.human(101L), null, null);
        service.listMyPosts(101L, SubjectRef.human(101L), null, null);
        service.listMentionedMe(2L, SubjectRef.agent(101L), null, null);
        service.listMyPosts(2L, SubjectRef.agent(101L), null, null);

        verify(worldPostMapper).selectMentionFeed(101L, "HUMAN", null, 21);
        verify(worldPostMapper).selectMyPosts(101L, "HUMAN", null, 21);
        verify(worldPostMapper).selectMentionFeed(101L, "AGENT", null, 21);
        verify(worldPostMapper).selectMyPosts(101L, "AGENT", null, 21);
    }

    @Test
    void agentPostRowRendersCanonicalAgentAuthor() {
        WorldPostMapper.WorldPostRow row = row(10L, 101L, "AGENT");
        when(subjectRelationshipApi.listFriends(SubjectRef.human(2L))).thenReturn(List.of());
        when(worldPostMapper.selectFeed(2L, "HUMAN", "friends", List.of(), List.of(), null, 21)).thenReturn(List.of(row));
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

        WorldPostResponse item = service.listFeed(2L, SubjectRef.human(2L), "friends", null, null).getItems().get(0);

        assertThat(item.getAuthor().getId()).isEqualTo(101L);
        assertThat(item.getAuthor().getType()).isEqualTo("agent");
        assertThat(item.getAuthor().isAi()).isTrue();
        assertThat(item.getAuthor().getAssociatedHumanId()).isEqualTo(2L);
    }

    @Test
    void feedFriendOrderingAndFlagsUseRelationshipApiBoundary() {
        WorldPostMapper.WorldPostRow agentFriend = row(10L, 101L, "AGENT");
        WorldPostMapper.WorldPostRow humanFriend = row(11L, 3L, "HUMAN");
        WorldPostMapper.WorldPostRow stranger = row(12L, 4L, "HUMAN");
        SubjectRef viewer = SubjectRef.human(2L);
        when(subjectRelationshipApi.listFriends(viewer))
                .thenReturn(List.of(SubjectRef.agent(101L), SubjectRef.human(3L)));
        when(worldPostMapper.selectFeed(2L, "HUMAN", "friends", List.of(3L), List.of(101L), null, 21))
                .thenReturn(List.of(agentFriend, humanFriend, stranger));
        when(worldPostMapper.selectTopicNamesByPostId(10L)).thenReturn(List.of());
        when(worldPostMapper.selectTopicNamesByPostId(11L)).thenReturn(List.of());
        when(worldPostMapper.selectTopicNamesByPostId(12L)).thenReturn(List.of());
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L)))
                .thenReturn(SubjectSummaryResponse.builder().id(101L).type(SubjectType.AGENT).displayName("Nova").build());
        when(subjectDirectoryApi.getSubject(SubjectRef.human(3L)))
                .thenReturn(SubjectSummaryResponse.builder().id(3L).type(SubjectType.HUMAN).displayName("Ada").build());
        when(subjectDirectoryApi.getSubject(SubjectRef.human(4L)))
                .thenReturn(SubjectSummaryResponse.builder().id(4L).type(SubjectType.HUMAN).displayName("Lin").build());
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(101L)))
                .thenReturn(WalletCapability.agentDirect(101L));

        List<WorldPostResponse> items = service.listFeed(2L, SubjectRef.human(2L), "friends", null, null).getItems();

        assertThat(items)
                .extracting(WorldPostResponse::isFriend)
                .containsExactly(true, true, false);
        verify(subjectRelationshipApi).listFriends(viewer);
        verify(worldPostMapper).selectFeed(2L, "HUMAN", "friends", List.of(3L), List.of(101L), null, 21);
    }

    @Test
    void listPostsByAuthorScopesFriendCheckByHumanViewerSubjectType() {
        when(subjectRelationshipApi.areFriends(SubjectRef.human(2L), SubjectRef.agent(101L))).thenReturn(true);
        when(worldPostMapper.selectPostsByAuthor(2L, "HUMAN", 101L, "AGENT", null, 21))
                .thenReturn(List.of());

        service.listPostsByAuthor(2L, SubjectRef.human(2L), SubjectRef.agent(101L), null, 20);

        verify(subjectRelationshipApi).areFriends(SubjectRef.human(2L), SubjectRef.agent(101L));
    }

    @Test
    void listPostsByAuthorScopesFriendCheckByAgentViewerSubjectType() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        when(subjectRelationshipApi.areFriends(SubjectRef.agent(101L), SubjectRef.human(2L))).thenReturn(true);
        when(worldPostMapper.selectPostsByAuthor(101L, "AGENT", 2L, "HUMAN", null, 21))
                .thenReturn(List.of());

        service.listPostsByAuthor(2L, SubjectRef.agent(101L), SubjectRef.human(2L), null, 20);

        verify(subjectRelationshipApi).areFriends(SubjectRef.agent(101L), SubjectRef.human(2L));
    }

    @Test
    void legacyUserAuthorTypeIsRejected() {
        WorldPostMapper.WorldPostRow row = row(10L, 2L, "USER");
        when(subjectRelationshipApi.listFriends(SubjectRef.human(2L))).thenReturn(List.of());
        when(worldPostMapper.selectFeed(2L, "HUMAN", "friends", List.of(), List.of(), null, 21)).thenReturn(List.of(row));

        assertThatThrownBy(() -> service.listFeed(2L, SubjectRef.human(2L), "friends", null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("world.author.type.invalid");
    }

    @Test
    void engagementControllerRequiresActorSubjectAndDoesNotUseViewerFallback() {
        WorldService worldService = org.mockito.Mockito.mock(WorldService.class);
        WorldUploadService uploadService = org.mockito.Mockito.mock(WorldUploadService.class);
        WorldController controller = new WorldController(worldService, uploadService);
        UserContext.setCurrentUser(2L);

        assertThatThrownBy(() -> controller.toggleUpvote(10L, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.invalid");

        verifyNoInteractions(worldService);
        when(worldService.toggleUpvote(2L, SubjectRef.agent(101L), 10L)).thenReturn(true);

        assertThat(controller.toggleUpvote(10L, 101L, "AGENT").getData().get("upvoted")).isEqualTo(true);

        verify(worldService).toggleUpvote(2L, SubjectRef.agent(101L), 10L);
    }

    @Test
    void readControllerRequiresViewerSubjectAndDoesNotUsePrincipalFallback() {
        WorldService worldService = org.mockito.Mockito.mock(WorldService.class);
        WorldUploadService uploadService = org.mockito.Mockito.mock(WorldUploadService.class);
        WorldController controller = new WorldController(worldService, uploadService);
        UserContext.setCurrentUser(2L);

        assertThatThrownBy(() -> controller.listPosts("friends", null, null, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("world.actor.invalid");

        verifyNoInteractions(worldService);
        when(worldService.listFeed(2L, SubjectRef.human(2L), "friends", null, null)).thenReturn(PageResponse.empty());

        controller.listPosts("friends", null, null, 2L, "HUMAN");

        verify(worldService).listFeed(2L, SubjectRef.human(2L), "friends", null, null);
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
