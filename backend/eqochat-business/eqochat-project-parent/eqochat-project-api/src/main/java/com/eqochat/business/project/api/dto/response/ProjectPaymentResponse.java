package com.eqochat.business.project.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.*;

/**
 * 项目侧栏支付返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPaymentResponse {
    private Long id;
    private Long amount;
    private Long recipientSubjectId;
    private SubjectType recipientSubjectType;
    private String recipientDisplayName;
    private String masterWallet;
    private String walletRouting;
    private Long directRecipientSubjectId;
    private SubjectType directRecipientSubjectType;
    private Long settlementSubjectId;
    private SubjectType settlementSubjectType;
    private Long settlementHumanId;
    private Boolean financialAutonomy;
    private String walletPolicyState;
    private String walletPolicyReason;
    private Long liableHumanId;
    private String liabilityRoute;
    private String liabilityReason;
    private String status;
    private String date;
}
