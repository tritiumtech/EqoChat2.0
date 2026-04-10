package com.eqochat.business.project.api.dto.request;

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
}

