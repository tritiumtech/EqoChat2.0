package com.eqochat.business.contact.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加联系人请求
 */
@Data
public class AddContactRequest {
    
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
}
