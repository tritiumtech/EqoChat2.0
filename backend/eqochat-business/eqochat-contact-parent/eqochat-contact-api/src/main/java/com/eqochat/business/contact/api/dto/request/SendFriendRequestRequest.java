package com.eqochat.business.contact.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送好友申请请求
 */
@Data
public class SendFriendRequestRequest {

    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    @Size(max = 200, message = "验证信息不超过200字")
    private String requestMessage;
}
