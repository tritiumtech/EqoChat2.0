package com.eqochat.business.project.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建项目支付记录请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectPaymentRequest {

    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于0")
    private Long amount;

    @NotNull(message = "收款主体不能为空")
    private Long recipientSubjectId;

    @NotNull(message = "收款主体类型不能为空")
    private SubjectType recipientSubjectType;

    private String status;

    private String date;

    @NotNull(message = "actor subject is required")
    private Long actorSubjectId;

    @NotNull(message = "actor subject type is required")
    private SubjectType actorSubjectType;
}
