package com.eqochat.dto.response;

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
    private LocalDateTime createTime;
}
