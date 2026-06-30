package com.eqochat.business.world.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldPostReplyResponse {

    private String id;
    private Author author;
    private String content;
    private String timestamp;
    private int upvotes;
    private boolean upvoted;
    private String parentId;
    private java.util.List<WorldPostReplyResponse> replies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        private Long id;
        private String name;
        private String type;
        /**
         * 同 WorldPostResponse，前端用于渐变色/头像。
         */
        private String avatar;
        private boolean ai;
    }
}
