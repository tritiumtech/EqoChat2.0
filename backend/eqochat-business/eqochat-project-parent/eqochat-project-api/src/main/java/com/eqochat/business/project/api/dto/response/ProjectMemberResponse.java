package com.eqochat.business.project.api.dto.response;

import lombok.*;

/**
 * 项目成员返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private Long id;
    private String name;
    private String avatarUrl;
    private String type;
    private boolean isOnline;
    private Long masterId;
    private Integer creditScore;
}

