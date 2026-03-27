package com.eqochat.world;

import com.eqochat.dto.request.CreateWorldPostRequest;
import com.eqochat.dto.response.WorldPostResponse;
import com.eqochat.dto.response.WorldShareLinkResponse;
import com.eqochat.dto.response.WorldTopicResponse;

import java.util.List;

public interface WorldService {

    List<WorldPostResponse> listFeed(Long viewerId, String sortBy, Long cursorId, Integer limit);

    WorldPostResponse createPost(Long authorId, CreateWorldPostRequest request);

    WorldShareLinkResponse shareLink(Long postId);

    List<WorldTopicResponse> listTopics(Long viewerId, Integer limit);

    List<WorldPostResponse> listTopicPosts(Long viewerId, String topicName, Long cursorId, Integer limit);

    boolean toggleUpvote(Long viewerId, Long postId);

    boolean toggleTopicFollow(Long viewerId, String topicName);
}

