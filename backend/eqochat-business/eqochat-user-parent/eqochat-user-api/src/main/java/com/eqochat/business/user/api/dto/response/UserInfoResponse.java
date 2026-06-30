package com.eqochat.business.user.api.dto.response;

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
    private String locale;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String status;
    private Integer creditScore;
    /**
     * 展示积分。默认可回退为信用分，Sprint demo 可通过后端配置覆盖，不污染 0-100 信用分语义。
     */
    private Integer points;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createTime;
}
