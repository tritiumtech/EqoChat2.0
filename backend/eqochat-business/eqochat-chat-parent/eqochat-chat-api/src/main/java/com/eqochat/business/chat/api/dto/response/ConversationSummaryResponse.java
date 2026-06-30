package com.eqochat.business.chat.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationSummaryResponse {
    
    private Long id;
    private String title;
    private String avatarUrl;
    private String conversationType;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private Boolean online;

    private Long targetSubjectId;
    private SubjectType targetSubjectType;
}
