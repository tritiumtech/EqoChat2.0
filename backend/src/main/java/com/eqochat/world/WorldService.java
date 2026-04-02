package com.eqochat.world;

import com.eqochat.dto.request.CreateWorldPostRequest;
import com.eqochat.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.dto.response.WorldPostResponse;
import com.eqochat.dto.response.WorldPostReplyResponse;
import com.eqochat.dto.response.WorldShareLinkResponse;
import com.eqochat.dto.response.WorldTopicResponse;

import java.util.List;

public interface WorldService {

    List<WorldPostResponse> listFeed(Long viewerId, String sortBy, Long cursorId, Integer limit);

    /**
     * 查看某用户发布的动态（需与作者为好友，用于联系人详情等）。
     */
    List<WorldPostResponse> listPostsByAuthor(Long viewerId, Long authorId, Long cursorId, Integer limit);

    WorldPostResponse createPost(Long authorId, CreateWorldPostRequest request);

    int createReply(Long authorId, Long postId, CreateWorldPostReplyRequest request);

    WorldShareLinkResponse shareLink(Long postId);

    List<WorldTopicResponse> listTopics(Long viewerId, Integer limit);

    List<WorldPostResponse> listTopicPosts(Long viewerId, String topicName, Long cursorId, Integer limit);

    List<WorldPostResponse> listMentionedMe(Long viewerId, Long cursorId, Integer limit);

    boolean toggleUpvote(Long viewerId, Long postId);

    boolean toggleTopicFollow(Long viewerId, String topicName);

    /**
     * 列出单条动态下的回复树（父子结构）。
     */
    List<WorldPostReplyResponse> listReplies(Long viewerId, Long postId, Long cursorId, Integer limit);

    /**
     * 点赞 / 取消点赞 回复。
     */
    boolean toggleReplyUpvote(Long viewerId, Long replyId);

    /**
     * 当前用户自己发布的动态（用于 My Tab 时间线展示）。
     */
    List<WorldPostResponse> listMyPosts(Long viewerId, Long cursorId, Integer limit);
}

