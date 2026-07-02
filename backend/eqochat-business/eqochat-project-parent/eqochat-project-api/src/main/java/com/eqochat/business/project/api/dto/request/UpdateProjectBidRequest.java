package com.eqochat.business.project.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 更新项目竞价（bid）请求。
 */
@Data
public class UpdateProjectBidRequest {

    @NotNull
    @Positive
    private Long newBid;

    @NotNull
    private Long actorSubjectId;

    @NotNull
    private SubjectType actorSubjectType;
}
