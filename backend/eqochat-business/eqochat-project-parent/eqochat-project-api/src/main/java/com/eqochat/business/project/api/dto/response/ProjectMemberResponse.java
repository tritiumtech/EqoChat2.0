package com.eqochat.business.project.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.*;

/**
 * 项目成员返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private Long memberSubjectId;
    private SubjectType memberSubjectType;
    private String name;
    private String avatarUrl;
    private boolean isOnline;
    private Long associatedHumanId;
    private String associatedHumanName;
    private Long liableHumanId;
    private Integer creditScore;
}
