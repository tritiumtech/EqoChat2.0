package com.eqochat.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 用户搜索结果响应
 */
@Data
@Builder
public class UserSearchResponse {
    
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String phone;
    private String email;
    private Integer worldPostCount;
    private Boolean isFriend;
    private String friendType;
    private String status;
}
