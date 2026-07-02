package com.eqochat.business.agent.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMeResponse {

    private Long id;
    private String name;
    private String avatarUrl;
    private String description;
    private String agentType;
    private String permissionLevel;
    private Integer creditScore;
    private Long agentSubjectId;
    private SubjectType agentSubjectType;
    private Long ownerSubjectId;
    private SubjectType ownerSubjectType;
    private Long ownerId;
    private String ownerName;
    private String ownerType;

    private List<String> capabilities;
    private List<String> profileCapabilities;
    private List<CapabilityPolicyItem> capabilityPolicy;

    private boolean liabilityAccepted;
    private Boolean bindingLiabilityAccepted;
    private Long liableHumanId;
    private String liabilityRoute;
    private String liabilityReason;
    private boolean walletEnabled;
    private String walletPolicyState;
    private String walletRouting;
    private String walletPolicyReason;
    private Long directRecipientSubjectId;
    private SubjectType directRecipientSubjectType;
    private Long settlementSubjectId;
    private SubjectType settlementSubjectType;
    private Long settlementHumanId;
    private Boolean financialAutonomy;
    private String responsibilityChain;
    private long earnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapabilityPolicyItem {
        private String code;
        private String state;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletPolicyResponse {
        private boolean walletEnabled;
        private String walletPolicyState;
        private String walletRouting;
        private String walletPolicyReason;
        private Long directRecipientSubjectId;
        private SubjectType directRecipientSubjectType;
        private Long settlementSubjectId;
        private SubjectType settlementSubjectType;
        private Long settlementHumanId;
        private Boolean financialAutonomy;
    }
}
