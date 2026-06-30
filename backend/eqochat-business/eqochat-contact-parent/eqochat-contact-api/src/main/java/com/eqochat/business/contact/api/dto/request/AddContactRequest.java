package com.eqochat.business.contact.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加联系人请求
 */
@Data
public class AddContactRequest {

    @NotNull(message = "目标主体不能为空")
    private Long targetSubjectId;

    @NotNull(message = "目标主体类型不能为空")
    private SubjectType targetSubjectType;
}
