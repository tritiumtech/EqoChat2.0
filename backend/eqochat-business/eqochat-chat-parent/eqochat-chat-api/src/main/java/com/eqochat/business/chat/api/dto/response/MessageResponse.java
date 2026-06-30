package com.eqochat.business.chat.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    
    private Long id;
    private Long conversationId;
    private Long senderSubjectId;
    private SubjectType senderSubjectType;
    private Long liableHumanId;
    private String messageType;
    private String content;
    private MessageAttachmentResponse attachment;
    private LocalDateTime createTime;
}
