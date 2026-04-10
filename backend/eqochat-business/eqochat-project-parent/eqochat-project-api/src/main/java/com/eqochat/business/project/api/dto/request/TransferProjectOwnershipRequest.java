package com.eqochat.business.project.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目所有权转让请求。
 */
@Data
public class TransferProjectOwnershipRequest {

    @NotNull
    private Long toMemberId;

    /**
     * HUMAN | AGENT
     */
    @NotBlank
    @Size(max = 20)
    private String toMemberType;
}

