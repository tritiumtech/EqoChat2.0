package com.eqochat.dto.response;

import lombok.*;

/**
 * 项目分享链接返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectShareLinkResponse {
    private String url;
}

