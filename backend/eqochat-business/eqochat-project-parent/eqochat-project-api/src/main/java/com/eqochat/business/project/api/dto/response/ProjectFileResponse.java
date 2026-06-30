package com.eqochat.business.project.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
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
    private Long uploaderSubjectId;
    private SubjectType uploaderSubjectType;
    private String uploaderDisplayName;
    private String size;
    private String date;
    private String downloadUrl;
}
