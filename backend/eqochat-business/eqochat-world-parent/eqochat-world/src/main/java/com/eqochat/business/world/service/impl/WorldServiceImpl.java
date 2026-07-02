package com.eqochat.business.world.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.PageResponse;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.business.contact.api.service.SubjectRelationshipApi;
import com.eqochat.business.world.config.WorldModuleProperties;
import com.eqochat.business.world.entity.WorldPost;
import com.eqochat.business.world.entity.WorldPostReply;
import com.eqochat.business.world.entity.WorldPostMention;
import com.eqochat.business.world.entity.WorldPostReplyUpvote;
import com.eqochat.business.world.entity.WorldPostTopic;
import com.eqochat.business.world.entity.WorldPostUpvote;
import com.eqochat.business.world.entity.WorldTopic;
import com.eqochat.business.world.entity.WorldTopicFollow;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.business.world.api.dto.request.CreateWorldPostRequest;
import com.eqochat.business.world.api.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.business.world.api.dto.response.WorldPostResponse;
import com.eqochat.business.world.api.dto.response.WorldPostReplyResponse;
import com.eqochat.business.world.api.dto.response.WorldShareLinkResponse;
import com.eqochat.business.world.api.dto.response.WorldTopicResponse;
import com.eqochat.business.world.mapper.WorldPostMentionMapper;
import com.eqochat.business.world.mapper.WorldPostMapper;
import com.eqochat.business.world.mapper.WorldPostReplyMapper;
import com.eqochat.business.world.mapper.WorldPostTopicMapper;
import com.eqochat.business.world.mapper.WorldPostUpvoteMapper;
import com.eqochat.business.world.mapper.WorldPostReplyUpvoteMapper;
import com.eqochat.business.world.mapper.WorldTopicFollowMapper;
import com.eqochat.business.world.mapper.WorldTopicMapper;
import com.eqochat.business.world.WorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorldServiceImpl implements WorldService {

    private static final int DEFAULT_LIMIT = 20;
    private static final String MESSAGE_MENTION_NOTIFICATION_TYPE = "MESSAGE_MENTION";
    /**
     * 支持中文/英文/数字/下划线/短横线话题：#话题 #topic #topic_1 #主题-a
     */
    private static final Pattern TOPIC_PATTERN = Pattern.compile("#([\\p{L}\\p{N}_-]+)");

    private final WorldPostMapper worldPostMapper;
    private final WorldPostMentionMapper worldPostMentionMapper;
    private final WorldTopicMapper worldTopicMapper;
    private final WorldPostTopicMapper worldPostTopicMapper;
    private final WorldPostUpvoteMapper worldPostUpvoteMapper;
    private final WorldTopicFollowMapper worldTopicFollowMapper;
    private final WorldPostReplyMapper worldPostReplyMapper;
    private final WorldPostReplyUpvoteMapper worldPostReplyUpvoteMapper;
    private final SubjectRelationshipApi subjectRelationshipApi;
    private final NotificationService notificationService;
    private final WorldModuleProperties worldModuleProperties;
    private final SubjectDirectoryApi subjectDirectoryApi;
    private final LiabilityPolicyApi liabilityPolicyApi;
    private final WalletPolicyApi walletPolicyApi;

    @Override
    public PageResponse<WorldPostResponse> listFeed(Long principalHumanId, SubjectRef viewer, String sortBy, Long cursorId, Integer limit) {
        SubjectRef resolvedViewer = requireAuthorizedSubject(principalHumanId, viewer);
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        String sort = normalizeSort(sortBy);
        FriendLookup friendLookup = "friends".equals(sort) ? loadFriendLookup(resolvedViewer) : null;

        // 查询多一条用于判断是否有更多
        List<WorldPostMapper.WorldPostRow> rows = worldPostMapper.selectFeed(
                resolvedViewer.id(),
                resolvedViewer.type().name(),
                sort,
                friendLookup != null ? friendLookup.humanIds() : List.of(),
                friendLookup != null ? friendLookup.agentIds() : List.of(),
                cursorId,
                size + 1);
        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }

        if (rows.isEmpty()) {
            return PageResponse.empty();
        }
        markFriendRows(resolvedViewer, rows, friendLookup);

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        List<WorldPostResponse> items = rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();

        Long nextCursorId = hasMore ? rows.get(rows.size() - 1).getId() : null;

        return PageResponse.of(items, hasMore, nextCursorId);
    }

    @Override
    public PageResponse<WorldPostResponse> listPostsByAuthor(
            Long principalHumanId,
            SubjectRef viewer,
            SubjectRef author,
            Long cursorId,
            Integer limit
    ) {
        if (author == null || author.id() == null) {
            throw BizException.of("world.author.required");
        }
        if (author.type() == null || author.type() == SubjectType.SYSTEM) {
            throw BizException.of("world.author.type.invalid");
        }
        SubjectRef resolvedViewer = requireAuthorizedSubject(principalHumanId, viewer);
        if (!areFriends(resolvedViewer, author)) {
            throw BizException.of("contact.not_friend");
        }
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        List<WorldPostMapper.WorldPostRow> rows =
                worldPostMapper.selectPostsByAuthor(
                        resolvedViewer.id(), resolvedViewer.type().name(), author.id(), author.type().name(), cursorId, size + 1);
        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }
        if (rows.isEmpty()) {
            return PageResponse.empty();
        }
        markFriendRows(resolvedViewer, rows, null);
        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));
        List<WorldPostResponse> items = rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
        Long nextCursorId = hasMore ? rows.get(rows.size() - 1).getId() : null;
        return PageResponse.of(items, hasMore, nextCursorId);
    }

    @Override
    @Transactional
    public WorldPostResponse createPost(Long principalHumanId, CreateWorldPostRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        SubjectRef actor = resolvePostingActor(
                principalHumanId,
                request != null ? request.getActorSubjectId() : null,
                request != null ? request.getActorSubjectType() : null
        );
        String content = request.getContent() != null ? request.getContent().trim() : "";
        String imageUrl = request.getImageUrl() != null ? request.getImageUrl().trim() : "";
        String videoUrl = request.getVideoUrl() != null ? request.getVideoUrl().trim() : "";
        String mediaType = normalizeMediaType(request.getMediaType(), imageUrl, videoUrl);
        if (!StringUtils.hasText(content) && !StringUtils.hasText(imageUrl) && !StringUtils.hasText(videoUrl)) {
            throw BizException.of("world.post.body_required");
        }
        WorldPost post = WorldPost.builder()
                .authorId(actor.id())
                .authorType(actor.type().name())
                .content(content)
                .mediaType(mediaType)
                .imageUrl(StringUtils.hasText(imageUrl) ? imageUrl : null)
                .videoUrl(StringUtils.hasText(videoUrl) ? videoUrl : null)
                .replyCount(0)
                .upvoteCount(0)
                .status("ACTIVE")
                .build();
        worldPostMapper.insert(post);
        if (post.getId() == null) {
            throw BizException.of("error.system");
        }
        Set<SubjectRef> mentionSubjects = sanitizeMentionSubjects(actor, request.getMentionedSubjects());
        for (SubjectRef mentionedSubject : mentionSubjects) {
            worldPostMentionMapper.insert(WorldPostMention.builder()
                    .postId(post.getId())
                    .mentionedSubjectId(mentionedSubject.id())
                    .mentionedSubjectType(mentionedSubject.type().name())
                    .build());
        }
        Set<String> topics = extractTopics(content);
        for (String topicName : topics) {
            WorldTopic topic = worldTopicMapper.selectByName(topicName);
            if (topic == null) {
                topic = WorldTopic.builder()
                        .name(topicName)
                        .postCount(0)
                        .followerCount(0)
                        .build();
                worldTopicMapper.insert(topic);
            }
            worldPostTopicMapper.insert(WorldPostTopic.builder()
                    .postId(post.getId())
                    .topicId(topic.getId())
                    .build());
            worldTopicMapper.update(null, new LambdaUpdateWrapper<WorldTopic>()
                    .setSql("post_count = post_count + 1")
                    .eq(WorldTopic::getId, topic.getId()));
        }
        if (!mentionSubjects.isEmpty()) {
            String title = "You were mentioned in a post";
            String shortContent = content;
            if (shortContent.length() > 120) {
                shortContent = shortContent.substring(0, 120) + "...";
            }
            for (SubjectRef mentionedSubject : mentionSubjects) {
                notificationService.sendNotification(
                        mentionedSubject,
                        MESSAGE_MENTION_NOTIFICATION_TYPE,
                        title,
                        shortContent,
                        "{\"postId\":" + post.getId() + "}",
                        actor
                );
            }
        }
        return toNewPostResponse(post, List.copyOf(topics));
    }

    @Override
    @Transactional
    public int createReply(Long principalHumanId, Long postId, CreateWorldPostReplyRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        SubjectRef actor = resolvePostingActor(
                principalHumanId,
                request != null ? request.getActorSubjectId() : null,
                request != null ? request.getActorSubjectType() : null
        );
        if (postId == null) {
            throw BizException.of("world.post.required");
        }

        WorldPost post = worldPostMapper.selectById(postId);
        if (post == null || (post.getDelToken() != null && post.getDelToken() != 0L)) {
            throw BizException.of("world.post.not_found");
        }

        String content = request != null && request.getContent() != null ? request.getContent().trim() : "";
        if (!StringUtils.hasText(content)) {
            throw BizException.of("world.post.body_required");
        }

        WorldPostReply reply = WorldPostReply.builder()
                .postId(postId)
                .parentId(request.getParentId())
                .authorId(actor.id())
                .authorType(actor.type().name())
                .content(content)
                .upvoteCount(0)
                .build();
        worldPostReplyMapper.insert(reply);

        // reply_count 冗余在 world_post 上，保证前端 feed 展示即时一致
        worldPostMapper.update(null, new LambdaUpdateWrapper<WorldPost>()
                .setSql("reply_count = reply_count + 1")
                .eq(WorldPost::getId, postId));

        WorldPost updated = worldPostMapper.selectById(postId);
        return updated != null && updated.getReplyCount() != null ? updated.getReplyCount() : 0;
    }

    @Override
    public WorldShareLinkResponse shareLink(Long postId) {
        if (postId == null) {
            throw BizException.of("world.post.required");
        }
        WorldPost post = worldPostMapper.selectById(postId);
        if (post == null) {
            throw BizException.of("world.post.not_found");
        }
        String tpl = worldModuleProperties.getShareUrlTemplate();
        if (!StringUtils.hasText(tpl) || !tpl.contains("{postId}")) {
            throw BizException.of("world.share_url_template.invalid");
        }
        String url = tpl.replace("{postId}", String.valueOf(postId));
        return WorldShareLinkResponse.builder().url(url).build();
    }

    @Override
    public PageResponse<WorldTopicResponse> listTopics(Long principalHumanId, SubjectRef viewer, Integer limit, Long cursorId) {
        SubjectRef resolvedViewer = requireAuthorizedSubject(principalHumanId, viewer);
        int size = sanitizeLimit(limit, 50);
        List<WorldTopicMapper.WorldTopicRow> rows =
                worldTopicMapper.selectTopTopicsWithCursor(resolvedViewer.id(), resolvedViewer.type().name(), cursorId, size + 1);
        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }
        List<WorldTopicResponse> items = rows.stream()
                .map(r -> {
                    boolean following = r.getIsFollowing() != null && r.getIsFollowing() == 1;
                    return WorldTopicResponse.builder()
                            .id(String.valueOf(r.getId()))
                            .name(r.getName())
                            .posts(nvl(r.getPostCount()))
                            .followers(nvl(r.getFollowerCount()))
                            .favorite(following)
                            .followed(following)
                            .build();
                })
                .toList();
        return PageResponse.of(items, hasMore);
    }

    @Override
    public PageResponse<WorldPostResponse> listTopicPosts(Long principalHumanId, SubjectRef viewer, String topicName, Long cursorId, Integer limit) {
        SubjectRef resolvedViewer = requireAuthorizedSubject(principalHumanId, viewer);
        if (topicName == null || topicName.isBlank()) {
            throw BizException.of("world.topic.required");
        }
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        List<WorldPostMapper.WorldPostRow> rows =
                worldPostMapper.selectTopicPosts(
                        resolvedViewer.id(), resolvedViewer.type().name(), topicName.trim(), cursorId, size + 1);
        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }
        if (rows.isEmpty()) {
            return PageResponse.empty();
        }
        markFriendRows(resolvedViewer, rows, null);

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        List<WorldPostResponse> items = rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
        Long nextCursorId = hasMore ? rows.get(rows.size() - 1).getId() : null;
        return PageResponse.of(items, hasMore, nextCursorId);
    }

    @Override
    public PageResponse<WorldPostResponse> listMentionedMe(Long principalHumanId, SubjectRef viewer, Long cursorId, Integer limit) {
        SubjectRef resolvedViewer = requireAuthorizedSubject(principalHumanId, viewer);
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        List<WorldPostMapper.WorldPostRow> rows =
                worldPostMapper.selectMentionFeed(resolvedViewer.id(), resolvedViewer.type().name(), cursorId, size + 1);
        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }
        if (rows.isEmpty()) {
            return PageResponse.empty();
        }
        markFriendRows(resolvedViewer, rows, null);

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        List<WorldPostResponse> items = rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
        Long nextCursorId = hasMore ? rows.get(rows.size() - 1).getId() : null;
        return PageResponse.of(items, hasMore, nextCursorId);
    }

    @Override
    @Transactional
    public boolean toggleUpvote(Long principalHumanId, SubjectRef actor, Long postId) {
        SubjectRef voter = requireAuthorizedSubject(principalHumanId, actor);
        if (postId == null) throw BizException.of("world.post.required");
        WorldPost post = worldPostMapper.selectById(postId);
        if (post == null || (post.getDelToken() != null && post.getDelToken() != 0L)) {
            throw BizException.of("world.post.not_found");
        }
        String viewerType = voter.type().name();
        WorldPostUpvote existing = worldPostUpvoteMapper.findActive(postId, voter.id(), viewerType);
        if (existing != null) {
            // 软删除 upvote 记录 + 回写计数
            worldPostUpvoteMapper.update(null, new LambdaUpdateWrapper<WorldPostUpvote>()
                    .set(WorldPostUpvote::getDelToken, System.currentTimeMillis())
                    .eq(WorldPostUpvote::getId, existing.getId()));
            worldPostMapper.update(null, new LambdaUpdateWrapper<WorldPost>()
                    .setSql("upvote_count = GREATEST(upvote_count - 1, 0)")
                    .eq(WorldPost::getId, postId));
            return false;
        }
        WorldPostUpvote any = worldPostUpvoteMapper.findAny(postId, voter.id(), viewerType);
        if (any != null) {
            // 复用历史记录，避免同一投票主体的唯一键冲突
            worldPostUpvoteMapper.update(null, new LambdaUpdateWrapper<WorldPostUpvote>()
                    .set(WorldPostUpvote::getDelToken, 0L)
                    .eq(WorldPostUpvote::getId, any.getId()));
        } else {
            worldPostUpvoteMapper.insert(WorldPostUpvote.builder()
                    .postId(postId)
                    .voterId(voter.id())
                    .voterType(viewerType)
                    .build());
        }
        worldPostMapper.update(null, new LambdaUpdateWrapper<WorldPost>()
                .setSql("upvote_count = upvote_count + 1")
                .eq(WorldPost::getId, postId));
        return true;
    }

    @Override
    @Transactional
    public boolean toggleTopicFollow(Long principalHumanId, SubjectRef actor, String topicName) {
        SubjectRef follower = requireAuthorizedSubject(principalHumanId, actor);
        if (topicName == null || topicName.isBlank()) throw BizException.of("world.topic.required");
        WorldTopic topic = worldTopicMapper.selectByName(topicName.trim());
        if (topic == null) throw BizException.of("world.topic.not_found");

        String viewerType = follower.type().name();
        WorldTopicFollow existing = worldTopicFollowMapper.findActive(topic.getId(), follower.id(), viewerType);
        if (existing != null) {
            worldTopicFollowMapper.update(null, new LambdaUpdateWrapper<WorldTopicFollow>()
                    .set(WorldTopicFollow::getDelToken, System.currentTimeMillis())
                    .eq(WorldTopicFollow::getId, existing.getId()));
            worldTopicMapper.update(null, new LambdaUpdateWrapper<WorldTopic>()
                    .setSql("follower_count = GREATEST(follower_count - 1, 0)")
                    .eq(WorldTopic::getId, topic.getId()));
            return false;
        }
        WorldTopicFollow any = worldTopicFollowMapper.findAny(topic.getId(), follower.id(), viewerType);
        if (any != null) {
            // 复用历史记录，避免同一关注主体的唯一键冲突
            worldTopicFollowMapper.update(null, new LambdaUpdateWrapper<WorldTopicFollow>()
                    .set(WorldTopicFollow::getDelToken, 0L)
                    .eq(WorldTopicFollow::getId, any.getId()));
        } else {
            worldTopicFollowMapper.insert(WorldTopicFollow.builder()
                    .topicId(topic.getId())
                    .followerId(follower.id())
                    .followerType(viewerType)
                    .build());
        }
        worldTopicMapper.update(null, new LambdaUpdateWrapper<WorldTopic>()
                .setSql("follower_count = follower_count + 1")
                .eq(WorldTopic::getId, topic.getId()));
        return true;
    }

    @Override
    public List<WorldPostReplyResponse> listReplies(Long principalHumanId, SubjectRef viewer, Long postId, Long cursorId, Integer limit) {
        SubjectRef resolvedViewer = requireAuthorizedSubject(principalHumanId, viewer);
        if (postId == null) {
            throw BizException.of("world.post.required");
        }
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);

        List<WorldPostReply> rows = worldPostReplyMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorldPostReply>()
                        .eq(WorldPostReply::getPostId, postId)
                        .orderByAsc(WorldPostReply::getCreateTime)
                        .last("LIMIT " + size)
        );
        if (rows.isEmpty()) {
            return List.of();
        }

        // 当前主体点赞的回复
        List<Long> replyIds = rows.stream().map(WorldPostReply::getId).toList();
        List<WorldPostReplyUpvote> activeUpvotes = worldPostReplyUpvoteMapper
                .selectActiveByVoterAndReplyIds(resolvedViewer.id(), resolvedViewer.type().name(), replyIds);
        Set<Long> likedIds = (activeUpvotes == null ? List.<WorldPostReplyUpvote>of() : activeUpvotes).stream()
                .map(WorldPostReplyUpvote::getReplyId)
                .collect(Collectors.toSet());

        // 先扁平化，再组装树
        Map<Long, WorldPostReplyResponse> nodeMap = rows.stream()
                .collect(Collectors.toMap(
                        WorldPostReply::getId,
                        r -> toReplyResponse(r, likedIds.contains(r.getId()))
                ));

        List<WorldPostReplyResponse> roots = new java.util.ArrayList<>();
        for (WorldPostReply r : rows) {
            WorldPostReplyResponse node = nodeMap.get(r.getId());
            Long pid = r.getParentId();
            if (pid == null) {
                roots.add(node);
            } else {
                WorldPostReplyResponse parent = nodeMap.get(pid);
                if (parent != null) {
                    if (parent.getReplies() == null) {
                        parent.setReplies(new java.util.ArrayList<>());
                    }
                    parent.getReplies().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public boolean toggleReplyUpvote(Long principalHumanId, SubjectRef actor, Long replyId) {
        SubjectRef voter = requireAuthorizedSubject(principalHumanId, actor);
        if (replyId == null) {
            throw BizException.of("world.reply.required");
        }
        WorldPostReply reply = worldPostReplyMapper.selectById(replyId);
        if (reply == null || (reply.getDelToken() != null && reply.getDelToken() != 0L)) {
            throw BizException.of("world.reply.not_found");
        }

        String viewerType = voter.type().name();
        WorldPostReplyUpvote existing =
                worldPostReplyUpvoteMapper.findActive(replyId, voter.id(), viewerType);
        if (existing != null) {
            worldPostReplyUpvoteMapper.update(null, new LambdaUpdateWrapper<WorldPostReplyUpvote>()
                    .set(WorldPostReplyUpvote::getDelToken, System.currentTimeMillis())
                    .eq(WorldPostReplyUpvote::getId, existing.getId()));
            worldPostReplyMapper.update(null, new LambdaUpdateWrapper<WorldPostReply>()
                    .setSql("upvote_count = GREATEST(upvote_count - 1, 0)")
                    .eq(WorldPostReply::getId, replyId));
            return false;
        }

        WorldPostReplyUpvote any =
                worldPostReplyUpvoteMapper.findAny(replyId, voter.id(), viewerType);
        if (any != null) {
            worldPostReplyUpvoteMapper.update(null, new LambdaUpdateWrapper<WorldPostReplyUpvote>()
                    .set(WorldPostReplyUpvote::getDelToken, 0L)
                    .eq(WorldPostReplyUpvote::getId, any.getId()));
        } else {
            worldPostReplyUpvoteMapper.insert(WorldPostReplyUpvote.builder()
                    .replyId(replyId)
                    .voterId(voter.id())
                    .voterType(viewerType)
                    .build());
        }
        worldPostReplyMapper.update(null, new LambdaUpdateWrapper<WorldPostReply>()
                .setSql("upvote_count = upvote_count + 1")
                .eq(WorldPostReply::getId, replyId));
        return true;
    }

    @Override
    public PageResponse<WorldPostResponse> listMyPosts(Long principalHumanId, SubjectRef author, Long cursorId, Integer limit) {
        SubjectRef resolvedAuthor = requireAuthorizedSubject(principalHumanId, author);
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        List<WorldPostMapper.WorldPostRow> rows =
                worldPostMapper.selectMyPosts(resolvedAuthor.id(), resolvedAuthor.type().name(), cursorId, size + 1);
        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }
        if (rows.isEmpty()) {
            return PageResponse.empty();
        }
        markFriendRows(resolvedAuthor, rows, null);

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        List<WorldPostResponse> items = rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
        Long nextCursorId = hasMore ? rows.get(rows.size() - 1).getId() : null;
        return PageResponse.of(items, hasMore, nextCursorId);
    }

    private static int sanitizeLimit(Integer limit, int fallback) {
        if (limit == null) return fallback;
        if (limit <= 0) return fallback;
        return Math.min(limit, 50);
    }

    private static String normalizeSort(String sortBy) {
        if (sortBy == null) return "friends";
        String s = sortBy.trim().toLowerCase();
        return Set.of("friends", "upvotes", "topics").contains(s) ? s : "friends";
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private WorldPostResponse toPostResponse(WorldPostMapper.WorldPostRow r, List<String> topics) {
        SubjectType type = parseAuthorType(r.getAuthorType());
        SubjectRef authorRef = new SubjectRef(r.getAuthorId(), type);
        SubjectSummaryResponse author = subjectDirectoryApi.getSubject(authorRef);
        String name = author != null && StringUtils.hasText(author.getDisplayName())
                ? author.getDisplayName()
                : (r.getAuthorName() != null ? r.getAuthorName() : "User");
        String avatar = author != null && StringUtils.hasText(author.getAvatarUrl())
                ? author.getAvatarUrl()
                : (StringUtils.hasText(r.getAuthorAvatarUrl()) ? r.getAuthorAvatarUrl() : ColorUtil.pick(name + "#" + r.getAuthorId()));
        boolean authorAi = type == SubjectType.AGENT;
        WalletCapability wallet = authorAi ? walletPolicyApi.resolveWallet(authorRef) : null;
        return WorldPostResponse.builder()
                .id(String.valueOf(r.getId()))
                .author(WorldPostResponse.Author.builder()
                        .id(r.getAuthorId())
                        .name(name)
                        .type(type.jsonValue())
                        .avatar(avatar)
                        .ai(authorAi)
                        .associatedHumanId(author != null ? author.getAssociatedHumanId() : r.getAuthorOwnerId())
                        .associatedHumanName(author != null ? author.getAssociatedHumanName() : r.getAuthorOwnerName())
                        .walletRouting(wallet != null ? wallet.routing() : null)
                        .build())
                .content(r.getContent())
                .mediaType(StringUtils.hasText(r.getMediaType()) ? r.getMediaType() : "TEXT")
                .imageUrl(r.getImageUrl())
                .videoUrl(r.getVideoUrl())
                .sharedProject(toSharedProject(r))
                .timestamp(WorldPostResponse.formatTime(r.getCreateTime()))
                .createdAt(r.getCreateTime() != null ? r.getCreateTime().toString() : null)
                .upvotes(nvl(r.getUpvoteCount()))
                .replies(nvl(r.getReplyCount()))
                .topics(topics)
                .upvoted(r.getIsUpvoted() != null && r.getIsUpvoted() == 1)
                .friend(r.getIsFriend() != null && r.getIsFriend() == 1)
                .build();
    }

    private WorldPostResponse toNewPostResponse(WorldPost post, List<String> topics) {
        Long aid = post.getAuthorId();
        SubjectType type = parseAuthorType(post.getAuthorType());
        SubjectSummaryResponse author = subjectDirectoryApi.getSubject(new SubjectRef(aid, type));
        String name = author != null && StringUtils.hasText(author.getDisplayName())
                ? author.getDisplayName()
                : (type == SubjectType.AGENT ? "Agent" : "User");
        String color = author != null && StringUtils.hasText(author.getAvatarUrl())
                ? author.getAvatarUrl()
                : ColorUtil.pick(name + "#" + aid);
        return WorldPostResponse.builder()
                .id(String.valueOf(post.getId()))
                .author(WorldPostResponse.Author.builder()
                        .id(aid)
                        .name(name)
                        .type(type.jsonValue())
                        .avatar(color)
                        .ai(type == SubjectType.AGENT)
                        .build())
                .content(post.getContent())
                .mediaType(StringUtils.hasText(post.getMediaType()) ? post.getMediaType() : "TEXT")
                .imageUrl(post.getImageUrl())
                .videoUrl(post.getVideoUrl())
                .timestamp(WorldPostResponse.formatTime(post.getCreateTime()))
                .createdAt(post.getCreateTime() != null ? post.getCreateTime().toString() : null)
                .upvotes(0)
                .replies(0)
                .topics(topics)
                .upvoted(false)
                .friend(false)
                .build();
    }

    private WorldPostReplyResponse toReplyResponse(WorldPostReply reply, boolean upvoted) {
        SubjectType type = parseAuthorType(reply.getAuthorType());
        SubjectRef authorRef = new SubjectRef(reply.getAuthorId(), type);
        SubjectSummaryResponse author = subjectDirectoryApi.getSubject(authorRef);
        String fallbackName = type == SubjectType.AGENT ? "Agent" : "User";
        String name = author != null && StringUtils.hasText(author.getDisplayName())
                ? author.getDisplayName()
                : fallbackName;
        String color = author != null && StringUtils.hasText(author.getAvatarUrl())
                ? author.getAvatarUrl()
                : ColorUtil.pick(name + "#" + reply.getAuthorId());
        return WorldPostReplyResponse.builder()
                .id(String.valueOf(reply.getId()))
                .author(WorldPostReplyResponse.Author.builder()
                        .id(reply.getAuthorId())
                        .name(name)
                        .type(type.jsonValue())
                        .avatar(color)
                        .ai(type == SubjectType.AGENT)
                        .build())
                .content(reply.getContent())
                .timestamp(WorldPostResponse.formatTime(reply.getCreateTime()))
                .upvotes(reply.getUpvoteCount() == null ? 0 : reply.getUpvoteCount())
                .upvoted(upvoted)
                .parentId(reply.getParentId() == null ? null : String.valueOf(reply.getParentId()))
                .replies(java.util.List.of())
                .build();
    }

    private static WorldPostResponse.SharedProject toSharedProject(WorldPostMapper.WorldPostRow r) {
        if (r == null || r.getSharedProjectId() == null) {
            return null;
        }
        return WorldPostResponse.SharedProject.builder()
                .id(String.valueOf(r.getSharedProjectId()))
                .name(r.getSharedProjectName())
                .ownerName(r.getSharedProjectOwnerName())
                .ownerAi(Boolean.TRUE.equals(r.getSharedProjectOwnerAi()))
                .associatedHumanName(r.getSharedProjectAssociatedHumanName())
                .budget(r.getSharedProjectBudget())
                .teamMix(r.getSharedProjectTeamMix())
                .deadline(r.getSharedProjectDeadline())
                .status(r.getSharedProjectStatus())
                .build();
    }

    private static Set<String> extractTopics(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (!StringUtils.hasText(content)) {
            return out;
        }
        Matcher m = TOPIC_PATTERN.matcher(content);
        while (m.find()) {
            out.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private Set<SubjectRef> sanitizeMentionSubjects(
            SubjectRef author,
            List<CreateWorldPostRequest.MentionedSubject> mentionedSubjects
    ) {
        if (mentionedSubjects == null || mentionedSubjects.isEmpty()) {
            return Set.of();
        }
        for (CreateWorldPostRequest.MentionedSubject item : mentionedSubjects) {
            if (item != null && item.getSubjectType() == SubjectType.SYSTEM) {
                throw BizException.of("world.mention.subject_type.invalid");
            }
        }
        Set<SubjectRef> dedup = mentionedSubjects.stream()
                .filter(item -> item != null
                        && item.getSubjectId() != null
                        && item.getSubjectId() > 0
                        && item.getSubjectType() != null)
                .map(item -> new SubjectRef(item.getSubjectId(), item.getSubjectType()))
                .filter(ref -> !ref.equals(author))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dedup.isEmpty()) {
            return Set.of();
        }
        Map<SubjectRef, SubjectSummaryResponse> subjects = subjectDirectoryApi.batchGetSubjects(dedup);
        if (subjects == null || subjects.isEmpty()) {
            return Set.of();
        }
        return dedup.stream()
                .filter(subjects::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private SubjectRef resolvePostingActor(Long principalHumanId, Long actorSubjectId, SubjectType actorSubjectType) {
        if (actorSubjectId == null || actorSubjectId <= 0 || actorSubjectType == null || actorSubjectType == SubjectType.SYSTEM) {
            throw BizException.of("world.actor.invalid");
        }
        SubjectRef actor = new SubjectRef(actorSubjectId, actorSubjectType);
        return requireAuthorizedSubject(principalHumanId, actor);
    }

    private SubjectRef requireAuthorizedSubject(Long principalHumanId, SubjectRef subject) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (subject == null || subject.id() == null || subject.id() <= 0 || subject.type() == null || subject.type() == SubjectType.SYSTEM) {
            throw BizException.of("world.actor.invalid");
        }
        if (subject.type() == SubjectType.HUMAN) {
            if (!subject.id().equals(principalHumanId)) {
                throw BizException.of("world.actor.forbidden");
            }
            return subject;
        }
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(subject);
        if (liability == null || liability.liableHumanId() == null || !liability.liableHumanId().equals(principalHumanId)) {
            throw BizException.of("world.actor.forbidden");
        }
        return subject;
    }

    private static String normalizeMediaType(String raw, String imageUrl, String videoUrl) {
        String t = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
        if ("IMAGE".equals(t)) {
            if (!StringUtils.hasText(imageUrl)) {
                throw BizException.of("world.post.image_required");
            }
            return "IMAGE";
        }
        if ("VIDEO".equals(t)) {
            if (!StringUtils.hasText(videoUrl)) {
                throw BizException.of("world.post.video_required");
            }
            return "VIDEO";
        }
        if (StringUtils.hasText(videoUrl)) {
            return "VIDEO";
        }
        if (StringUtils.hasText(imageUrl)) {
            return "IMAGE";
        }
        return "TEXT";
    }

    private static SubjectType parseAuthorType(String authorType) {
        if (!StringUtils.hasText(authorType)) {
            throw BizException.of("world.author.type.required");
        }
        try {
            return SubjectType.from(authorType);
        } catch (IllegalArgumentException ex) {
            throw BizException.of("world.author.type.invalid");
        }
    }

    private boolean areFriends(SubjectRef viewer, SubjectRef author) {
        if (viewer == null || viewer.id() == null || viewer.type() == null || viewer.type() == SubjectType.SYSTEM
                || author == null || author.id() == null || author.type() == null || author.type() == SubjectType.SYSTEM) {
            return false;
        }
        return subjectRelationshipApi.areFriends(viewer, author);
    }

    private FriendLookup loadFriendLookup(SubjectRef owner) {
        List<SubjectRef> friends = subjectRelationshipApi.listFriends(owner);
        if (friends == null || friends.isEmpty()) {
            return FriendLookup.empty();
        }
        Set<SubjectRef> refs = friends.stream()
                .filter(ref -> ref != null && ref.id() != null && ref.type() != null && ref.type() != SubjectType.SYSTEM)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (refs.isEmpty()) {
            return FriendLookup.empty();
        }
        return new FriendLookup(
                refs,
                refs.stream().filter(ref -> ref.type() == SubjectType.HUMAN).map(SubjectRef::id).toList(),
                refs.stream().filter(ref -> ref.type() == SubjectType.AGENT).map(SubjectRef::id).toList()
        );
    }

    private void markFriendRows(SubjectRef viewer, List<WorldPostMapper.WorldPostRow> rows, FriendLookup existingLookup) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        FriendLookup lookup = existingLookup != null ? existingLookup : loadFriendLookup(viewer);
        for (WorldPostMapper.WorldPostRow row : rows) {
            if (row == null || row.getAuthorId() == null || !StringUtils.hasText(row.getAuthorType())) {
                continue;
            }
            SubjectType authorType = parseAuthorType(row.getAuthorType());
            row.setIsFriend(lookup.refs().contains(new SubjectRef(row.getAuthorId(), authorType)) ? 1 : 0);
        }
    }

    private record FriendLookup(Set<SubjectRef> refs, List<Long> humanIds, List<Long> agentIds) {
        private static FriendLookup empty() {
            return new FriendLookup(Set.of(), List.of(), List.of());
        }
    }

    private static final class ColorUtil {
        private static final String[] COLORS = {
                "#7C3AED", "#2563EB", "#DC2626", "#059669",
                "#D97706", "#0EA5E9", "#EC4899", "#14B8A6"
        };

        static String pick(String key) {
            if (key == null) return COLORS[0];
            int h = 0;
            for (int i = 0; i < key.length(); i++) {
                h = key.charAt(i) + ((h << 5) - h);
            }
            int idx = Math.abs(h) % COLORS.length;
            return COLORS[idx];
        }
    }
}
