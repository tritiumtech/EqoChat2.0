package com.eqochat.business.contact.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送好友申请请求
 */
@Data
public class SendFriendRequestRequest {

    @NotNull(message = "请求主体不能为空")
    private Long actorSubjectId;

    @NotNull(message = "请求主体类型不能为空")
    private SubjectType actorSubjectType;

    @NotNull(message = "接收主体不能为空")
    private Long recipientSubjectId;

    @NotNull(message = "接收主体类型不能为空")
    private SubjectType recipientSubjectType;

    @Size(max = 200, message = "验证信息不超过200字")
    private String requestMessage;
}
