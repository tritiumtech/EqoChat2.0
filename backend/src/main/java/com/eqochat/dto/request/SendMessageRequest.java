package com.eqochat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "message.content.required")
    @Size(max = 5000, message = "message.content.length")
    private String content;

    private String messageType = "TEXT";
    private Object metadata;
    private String replyToMessageId;
    private String intentData;
}
