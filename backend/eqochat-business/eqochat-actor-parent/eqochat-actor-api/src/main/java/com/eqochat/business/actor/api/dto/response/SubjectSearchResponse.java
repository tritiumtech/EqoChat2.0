package com.eqochat.business.actor.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSearchResponse {

    private Long subjectId;
    private SubjectType subjectType;
    private String did;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String status;
    private Integer worldPostCount;
    private Integer creditScore;
    private Boolean isFriend;
    private Long associatedHumanId;
    private String associatedHumanName;

    private Long id;
    private String nickname;
}
