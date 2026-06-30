package com.eqochat.business.actor.api.model;

public record MilestoneBenefit(
        String title,
        Integer maxOwnedAgents,
        Long projectNoDepositLimit,
        Integer projectDepositRatePercent,
        Boolean walletEligible,
        Boolean canArbitrate,
        Boolean canParticipateRuleGovernance
) {
}
