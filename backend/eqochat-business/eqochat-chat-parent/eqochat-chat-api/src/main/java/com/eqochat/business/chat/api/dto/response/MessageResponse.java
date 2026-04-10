package com.eqochat.business.chat.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderType;
    private String messageType;
    private String content;
    private MessageAttachmentResponse attachment;
    private LocalDateTime createTime;
}
