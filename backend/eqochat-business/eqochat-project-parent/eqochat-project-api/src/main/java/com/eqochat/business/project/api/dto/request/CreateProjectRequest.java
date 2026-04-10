package com.eqochat.business.project.api.dto.request;

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
}

