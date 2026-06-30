package com.eqochat.business.chat.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkConversationReadRequest {
    @NotNull
    private Long messageId;

    @NotNull(message = "reader subject id is required")
    private Long readerSubjectId;

    @NotNull(message = "reader subject type is required")
    private SubjectType readerSubjectType;
}
