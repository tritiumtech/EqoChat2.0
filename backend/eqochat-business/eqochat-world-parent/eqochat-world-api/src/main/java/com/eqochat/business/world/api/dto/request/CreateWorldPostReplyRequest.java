package com.eqochat.business.world.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorldPostReplyRequest {

    @NotBlank
    @Size(max = 8000)
    private String content;

    /**
     * 父回复ID，null 表示对动态本身的一级评论。
     */
    private Long parentId;
}

