package com.eqochat.business.actor.api.model;

public record CreditProfileSummary(
        Integer score,
        String rating,
        Integer disputeCount,
        Integer projectsCompleted,
        Integer successRate
) {
}
