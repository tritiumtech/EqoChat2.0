package com.eqochat.business.chat.api.dto.request;

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
}
