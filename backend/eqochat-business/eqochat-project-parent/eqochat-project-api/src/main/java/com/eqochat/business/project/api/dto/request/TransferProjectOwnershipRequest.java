package com.eqochat.business.project.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目所有权转让请求。
 */
@Data
public class TransferProjectOwnershipRequest {

    @NotNull
    private Long newOwnerSubjectId;

    @NotNull
    private SubjectType newOwnerSubjectType;

    @NotNull
    private Long actorSubjectId;

    @NotNull
    private SubjectType actorSubjectType;
}
