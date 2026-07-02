package com.eqochat.business.world;

import com.eqochat.business.world.api.dto.request.CreateWorldPostRequest;
import com.eqochat.business.world.api.dto.request.CreateWorldPostReplyRequest;
import com.eqochat.business.world.api.dto.response.WorldPostResponse;
import com.eqochat.business.world.api.dto.response.WorldPostReplyResponse;
import com.eqochat.business.world.api.dto.response.WorldShareLinkResponse;
import com.eqochat.business.world.api.dto.response.WorldTopicResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.framework.common.PageResponse;

import java.util.List;

public interface WorldService {

    PageResponse<WorldPostResponse> listFeed(Long principalHumanId, SubjectRef viewer, String sortBy, Long cursorId, Integer limit);

    /**
     * 查看某用户发布的动态（需与作者为好友，用于联系人详情等）。
     */
    PageResponse<WorldPostResponse> listPostsByAuthor(
            Long principalHumanId,
            SubjectRef viewer,
            SubjectRef author,
            Long cursorId,
            Integer limit
    );

    WorldPostResponse createPost(Long principalHumanId, CreateWorldPostRequest request);

    int createReply(Long principalHumanId, Long postId, CreateWorldPostReplyRequest request);

    WorldShareLinkResponse shareLink(Long postId);

    PageResponse<WorldTopicResponse> listTopics(Long principalHumanId, SubjectRef viewer, Integer limit, Long cursorId);

    PageResponse<WorldPostResponse> listTopicPosts(Long principalHumanId, SubjectRef viewer, String topicName, Long cursorId, Integer limit);

    PageResponse<WorldPostResponse> listMentionedMe(Long principalHumanId, SubjectRef viewer, Long cursorId, Integer limit);

    boolean toggleUpvote(Long principalHumanId, SubjectRef actor, Long postId);

    boolean toggleTopicFollow(Long principalHumanId, SubjectRef actor, String topicName);

    /**
     * 列出单条动态下的回复树（父子结构）。
     */
    List<WorldPostReplyResponse> listReplies(Long principalHumanId, SubjectRef viewer, Long postId, Long cursorId, Integer limit);

    /**
     * 点赞 / 取消点赞 回复。
     */
    boolean toggleReplyUpvote(Long principalHumanId, SubjectRef actor, Long replyId);

    /**
     * 当前用户自己发布的动态（用于 My Tab 时间线展示）。
     */
    PageResponse<WorldPostResponse> listMyPosts(Long principalHumanId, SubjectRef author, Long cursorId, Integer limit);
}
