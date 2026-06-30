package com.eqochat.business.chat.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @Size(max = 5000, message = "message.content.length")
    private String content;

    private String messageType = "TEXT";
    private Object metadata;
    private String replyToMessageId;
    private String intentData;

    @NotNull(message = "actor subject id is required")
    private Long actorSubjectId;

    @NotNull(message = "actor subject type is required")
    private SubjectType actorSubjectType;
}
