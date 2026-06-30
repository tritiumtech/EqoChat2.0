package com.eqochat.business.world.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorldPostReplyRequest {

    @NotNull
    private Long actorSubjectId;

    @NotNull
    private SubjectType actorSubjectType;

    @NotBlank
    @Size(max = 8000)
    private String content;

    /**
     * 父回复ID，null 表示对动态本身的一级评论。
     */
    private Long parentId;
}
