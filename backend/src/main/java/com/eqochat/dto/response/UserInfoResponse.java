package com.eqochat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    
    private Long id;
    private String did;
    private String phone;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String status;
    private Integer creditScore;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createTime;
}
