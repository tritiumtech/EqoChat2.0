package com.eqochat.business.user.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户公开资料响应（非好友视角）
 */
@Data
@Builder
public class UserPublicProfileResponse {
    
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String status;
    private Integer worldPostCount;
    private String friendType;
    private List<String> capabilities;
    private List<String> tags;
    private Boolean isFriend;
}
