package com.eqochat.business.project.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 创建项目请求。
 */
@Data
public class CreateProjectRequest {

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private Long bid;

    @NotNull
    private Long ownerSubjectId;

    @NotNull
    private SubjectType ownerSubjectType;
}
