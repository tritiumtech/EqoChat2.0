package com.eqochat.business.project.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.*;

/**
 * 项目侧栏任务返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTaskResponse {
    private Long id;
    private String title;
    private Long assigneeSubjectId;
    private SubjectType assigneeSubjectType;
    private String assigneeDisplayName;
    private String deadline;
    private String status;
    private String priority;
}
