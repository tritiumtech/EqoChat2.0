package com.eqochat.business.chat.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class CreateConversationRequest {
    
    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;
    
    private String title;
    private String avatarUrl;
}
