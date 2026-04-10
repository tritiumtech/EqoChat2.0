package com.eqochat.business.project.api.dto.response;

import lombok.*;

/**
 * 项目侧栏文件返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFileResponse {
    private Long id;
    private String name;
    private String type;
    private String uploadedBy;
    private boolean isAgent;
    private String size;
    private String date;
    private String downloadUrl;
}

