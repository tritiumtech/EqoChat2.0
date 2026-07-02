package com.eqochat.business.actor.api.model;

public record WalletCapability(
        CapabilityState state,
        String routing,
        Long settlementHumanId,
        SubjectRef directRecipient,
        Boolean financialAutonomy,
        String reason
) {

    public SubjectRef settlementSubject() {
        if (settlementHumanId != null) {
            return SubjectRef.human(settlementHumanId);
        }
        return directRecipient;
    }

    public static WalletCapability humanEnabled(Long humanId) {
        return new WalletCapability(
                CapabilityState.ENABLED,
                "HUMAN_WALLET",
                humanId,
                SubjectRef.human(humanId),
                true,
                "human wallet enabled by default"
        );
    }

    public static WalletCapability unavailable(SubjectRef subject, String reason) {
        return new WalletCapability(
                CapabilityState.DISABLED,
                "NONE",
                null,
                subject,
                false,
                reason
        );
    }

    public static WalletCapability agentToOwner(Long agentId, Long ownerId, String reason) {
        return new WalletCapability(
                CapabilityState.DISABLED,
                "AGENT_TO_OWNER",
                ownerId,
                SubjectRef.agent(agentId),
                false,
                reason
        );
    }

    public static WalletCapability agentDirect(Long agentId) {
        return agentDirect(agentId, "agent wallet enabled by owner and milestone policy");
    }

    public static WalletCapability agentDirect(Long agentId, String reason) {
        return new WalletCapability(
                CapabilityState.ENABLED,
                "AGENT_DIRECT",
                null,
                SubjectRef.agent(agentId),
                true,
                reason
        );
    }
}
