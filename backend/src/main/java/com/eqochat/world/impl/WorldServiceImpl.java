package com.eqochat.world.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eqochat.common.BizException;
import com.eqochat.config.WorldModuleProperties;
import com.eqochat.domain.entity.Notification;
import com.eqochat.domain.entity.UserProfile;
import com.eqochat.domain.entity.WorldPost;
import com.eqochat.domain.entity.WorldPostReply;
import com.eqochat.domain.entity.WorldPostMention;
import com.eqochat.domain.entity.WorldPostTopic;
import com.eqochat.domain.entity.WorldPostUpvote;
import com.eqochat.domain.entity.WorldTopic;
import com.eqochat.domain.entity.WorldTopicFollow;
import com.eqochat.dto.request.CreateWorldPostRequest;
import com.eqochat.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.dto.response.WorldPostResponse;
import com.eqochat.dto.response.WorldShareLinkResponse;
import com.eqochat.dto.response.WorldTopicResponse;
import com.eqochat.mapper.NotificationMapper;
import com.eqochat.mapper.UserProfileMapper;
import com.eqochat.mapper.WorldPostMentionMapper;
import com.eqochat.mapper.WorldPostMapper;
import com.eqochat.mapper.WorldPostReplyMapper;
import com.eqochat.mapper.WorldPostTopicMapper;
import com.eqochat.mapper.WorldPostUpvoteMapper;
import com.eqochat.mapper.WorldTopicFollowMapper;
import com.eqochat.mapper.WorldTopicMapper;
import com.eqochat.world.WorldService;
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
    private final UserProfileMapper userProfileMapper;
    private final NotificationMapper notificationMapper;
    private final WorldModuleProperties worldModuleProperties;

    @Override
    public List<WorldPostResponse> listFeed(Long viewerId, String sortBy, Long cursorId, Integer limit) {
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        String sort = normalizeSort(sortBy);

        List<WorldPostMapper.WorldPostRow> rows = worldPostMapper.selectFeed(viewerId, sort, cursorId, size);
        if (rows.isEmpty()) return List.of();

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        return rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public WorldPostResponse createPost(Long authorId, CreateWorldPostRequest request) {
        if (authorId == null) {
            throw BizException.of("auth.user.not_found");
        }
        String content = request.getContent() != null ? request.getContent().trim() : "";
        String imageUrl = request.getImageUrl() != null ? request.getImageUrl().trim() : "";
        String videoUrl = request.getVideoUrl() != null ? request.getVideoUrl().trim() : "";
        String mediaType = normalizeMediaType(request.getMediaType(), imageUrl, videoUrl);
        if (!StringUtils.hasText(content) && !StringUtils.hasText(imageUrl) && !StringUtils.hasText(videoUrl)) {
            throw BizException.of("world.post.body_required");
        }
        WorldPost post = WorldPost.builder()
                .authorId(authorId)
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
        Set<Long> mentionUserIds = sanitizeMentionUserIds(authorId, request.getMentionedUserIds());
        for (Long mentionedUserId : mentionUserIds) {
            worldPostMentionMapper.insert(WorldPostMention.builder()
                    .postId(post.getId())
                    .mentionedUserId(mentionedUserId)
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
        if (!mentionUserIds.isEmpty()) {
            String title = "You were mentioned in a post";
            String shortContent = content;
            if (shortContent.length() > 120) {
                shortContent = shortContent.substring(0, 120) + "...";
            }
            for (Long mentionedUserId : mentionUserIds) {
                notificationMapper.insert(Notification.builder()
                        .recipientId(mentionedUserId)
                        .recipientType(Notification.RecipientType.USER)
                        .notificationType(Notification.NotificationType.MESSAGE_MENTION)
                        .title(title)
                        .content(shortContent)
                        .data("{\"postId\":" + post.getId() + "}")
                        .senderId(authorId)
                        .senderType(Notification.SenderType.USER)
                        .isRead(false)
                        .priority(Notification.Priority.NORMAL)
                        .build());
            }
        }
        UserProfile profile = userProfileMapper.selectById(authorId);
        return toNewPostResponse(post, profile, List.copyOf(topics));
    }

    @Override
    @Transactional
    public int createReply(Long authorId, Long postId, CreateWorldPostReplyRequest request) {
        if (authorId == null) {
            throw BizException.of("auth.user.not_found");
        }
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
                .authorId(authorId)
                .content(content)
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
            tpl = "http://127.0.0.1:5173/#/pages/world/world?postId={postId}";
        }
        String url = tpl.replace("{postId}", String.valueOf(postId));
        return WorldShareLinkResponse.builder().url(url).build();
    }

    @Override
    public List<WorldTopicResponse> listTopics(Long viewerId, Integer limit) {
        int size = sanitizeLimit(limit, 50);
        return worldTopicMapper.selectTopTopics(viewerId, size).stream()
                .map(r -> WorldTopicResponse.builder()
                        .id(String.valueOf(r.getId()))
                        .name(r.getName())
                        .posts(nvl(r.getPostCount()))
                        .followers(nvl(r.getFollowerCount()))
                        .favorite(r.getIsFollowing() != null && r.getIsFollowing() == 1)
                        .build())
                .toList();
    }

    @Override
    public List<WorldPostResponse> listTopicPosts(Long viewerId, String topicName, Long cursorId, Integer limit) {
        if (topicName == null || topicName.isBlank()) {
            throw BizException.of("world.topic.required");
        }
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        List<WorldPostMapper.WorldPostRow> rows =
                worldPostMapper.selectTopicPosts(viewerId, topicName.trim(), cursorId, size);
        if (rows.isEmpty()) return List.of();

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        return rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    public List<WorldPostResponse> listMentionedMe(Long viewerId, Long cursorId, Integer limit) {
        int size = sanitizeLimit(limit, DEFAULT_LIMIT);
        List<WorldPostMapper.WorldPostRow> rows = worldPostMapper.selectMentionFeed(viewerId, cursorId, size);
        if (rows.isEmpty()) return List.of();

        Map<Long, List<String>> topicsByPostId = rows.stream()
                .map(WorldPostMapper.WorldPostRow::getId)
                .collect(Collectors.toMap(id -> id, worldPostMapper::selectTopicNamesByPostId));

        return rows.stream()
                .map(r -> toPostResponse(r, topicsByPostId.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public boolean toggleUpvote(Long viewerId, Long postId) {
        if (postId == null) throw BizException.of("world.post.required");
        WorldPost post = worldPostMapper.selectById(postId);
        if (post == null || (post.getDelToken() != null && post.getDelToken() != 0L)) {
            throw BizException.of("world.post.not_found");
        }
        WorldPostUpvote existing = worldPostUpvoteMapper.findActive(postId, viewerId);
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
        WorldPostUpvote any = worldPostUpvoteMapper.findAny(postId, viewerId);
        if (any != null) {
            // 复用历史记录，避免唯一键(post_id,user_id)冲突
            worldPostUpvoteMapper.update(null, new LambdaUpdateWrapper<WorldPostUpvote>()
                    .set(WorldPostUpvote::getDelToken, "0")
                    .eq(WorldPostUpvote::getId, any.getId()));
        } else {
            worldPostUpvoteMapper.insert(WorldPostUpvote.builder()
                    .postId(postId)
                    .userId(viewerId)
                    .build());
        }
        worldPostMapper.update(null, new LambdaUpdateWrapper<WorldPost>()
                .setSql("upvote_count = upvote_count + 1")
                .eq(WorldPost::getId, postId));
        return true;
    }

    @Override
    @Transactional
    public boolean toggleTopicFollow(Long viewerId, String topicName) {
        if (topicName == null || topicName.isBlank()) throw BizException.of("world.topic.required");
        WorldTopic topic = worldTopicMapper.selectByName(topicName.trim());
        if (topic == null) throw BizException.of("world.topic.not_found");

        WorldTopicFollow existing = worldTopicFollowMapper.findActive(topic.getId(), viewerId);
        if (existing != null) {
            worldTopicFollowMapper.update(null, new LambdaUpdateWrapper<WorldTopicFollow>()
                    .set(WorldTopicFollow::getDelToken, System.currentTimeMillis())
                    .eq(WorldTopicFollow::getId, existing.getId()));
            worldTopicMapper.update(null, new LambdaUpdateWrapper<WorldTopic>()
                    .setSql("follower_count = GREATEST(follower_count - 1, 0)")
                    .eq(WorldTopic::getId, topic.getId()));
            return false;
        }
        WorldTopicFollow any = worldTopicFollowMapper.findAny(topic.getId(), viewerId);
        if (any != null) {
            // 复用历史记录，避免唯一键(topic_id,user_id)冲突
            worldTopicFollowMapper.update(null, new LambdaUpdateWrapper<WorldTopicFollow>()
                    .set(WorldTopicFollow::getDelToken, "0")
                    .eq(WorldTopicFollow::getId, any.getId()));
        } else {
            worldTopicFollowMapper.insert(WorldTopicFollow.builder()
                    .topicId(topic.getId())
                    .userId(viewerId)
                    .build());
        }
        worldTopicMapper.update(null, new LambdaUpdateWrapper<WorldTopic>()
                .setSql("follower_count = follower_count + 1")
                .eq(WorldTopic::getId, topic.getId()));
        return true;
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

    private static WorldPostResponse toPostResponse(WorldPostMapper.WorldPostRow r, List<String> topics) {
        String name = r.getAuthorName() != null ? r.getAuthorName() : "User";
        String color = ColorUtil.pick(name + "#" + r.getAuthorId());
        return WorldPostResponse.builder()
                .id(String.valueOf(r.getId()))
                .author(WorldPostResponse.Author.builder()
                        .name(name)
                        .avatar(color)
                        .ai(false)
                        .build())
                .content(r.getContent())
                .mediaType(StringUtils.hasText(r.getMediaType()) ? r.getMediaType() : "TEXT")
                .imageUrl(r.getImageUrl())
                .videoUrl(r.getVideoUrl())
                .timestamp(WorldPostResponse.formatTime(r.getCreateTime()))
                .upvotes(nvl(r.getUpvoteCount()))
                .replies(nvl(r.getReplyCount()))
                .topics(topics)
                .upvoted(r.getIsUpvoted() != null && r.getIsUpvoted() == 1)
                .friend(r.getIsFriend() != null && r.getIsFriend() == 1)
                .build();
    }

    private static WorldPostResponse toNewPostResponse(WorldPost post, UserProfile profile, List<String> topics) {
        Long aid = post.getAuthorId();
        String name = profile != null && StringUtils.hasText(profile.getNickname()) ? profile.getNickname() : "User";
        String color = profile != null && StringUtils.hasText(profile.getAvatarUrl())
                ? profile.getAvatarUrl()
                : ColorUtil.pick(name + "#" + aid);
        return WorldPostResponse.builder()
                .id(String.valueOf(post.getId()))
                .author(WorldPostResponse.Author.builder()
                        .name(name)
                        .avatar(color)
                        .ai(false)
                        .build())
                .content(post.getContent())
                .mediaType(StringUtils.hasText(post.getMediaType()) ? post.getMediaType() : "TEXT")
                .imageUrl(post.getImageUrl())
                .videoUrl(post.getVideoUrl())
                .timestamp(WorldPostResponse.formatTime(post.getCreateTime()))
                .upvotes(0)
                .replies(0)
                .topics(topics)
                .upvoted(false)
                .friend(false)
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

    private Set<Long> sanitizeMentionUserIds(Long authorId, List<Long> mentionedUserIds) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> dedup = mentionedUserIds.stream()
                .filter(id -> id != null && id > 0 && !id.equals(authorId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dedup.isEmpty()) {
            return Set.of();
        }
        Set<Long> exists = userProfileMapper.selectBatchIds(dedup).stream()
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
        if (exists.isEmpty()) {
            return Set.of();
        }
        return dedup.stream().filter(exists::contains).collect(Collectors.toCollection(LinkedHashSet::new));
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

