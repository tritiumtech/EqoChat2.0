package com.eqochat.business.chat.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class CreateConversationRequest {

    @NotNull(message = "目标主体不能为空")
    private Long targetSubjectId;

    @NotNull(message = "目标主体类型不能为空")
    private SubjectType targetSubjectType;
    
    private String title;
    private String avatarUrl;
}
