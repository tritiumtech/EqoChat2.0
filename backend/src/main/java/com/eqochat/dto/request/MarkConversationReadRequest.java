package com.eqochat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkConversationReadRequest {
    @NotNull
    private Long messageId;
}

