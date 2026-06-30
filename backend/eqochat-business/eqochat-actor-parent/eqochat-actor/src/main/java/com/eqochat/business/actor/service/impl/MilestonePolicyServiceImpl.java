package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.CreditProfileSummary;
import com.eqochat.business.actor.api.model.MilestoneBenefit;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.service.MilestonePolicyApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MilestonePolicyServiceImpl implements MilestonePolicyApi {

    private final ActorSubjectValidator subjectValidator;
    private final ActorDataAccess dataAccess;

    @Override
    public MilestoneBenefit resolveMilestone(SubjectRef subject) {
        if (subject == null || subject.id() == null) {
            return base(false, 0, 0, "Unknown");
        }

        ActorSubjectValidation validation = subjectValidator.validateOrdinaryCapabilities(subject);
        if (!validation.valid()) {
            return base(false, 0, 0, "Unknown");
        }

        if (subject.isHuman()) {
            return humanMilestone(subject, validation.human().getCreditScore());
        } else if (subject.isAgent()) {
            return agentMilestone(subject, validation.agent().getCreditScore());
        }

        return base(false, 0, 0, "Unknown");
    }

    private MilestoneBenefit agentMilestone(SubjectRef subject, Integer legacyCredit) {
        int points = dataAccess.currentPoints(subject, 0);
        CreditProfileSummary credit = dataAccess.creditProfile(subject, legacyCredit);
        int score = credit.score() != null ? credit.score() : 300;

        boolean walletEligible = points >= 500 && score >= 575;
        if (points >= 2000) {
            return benefit("Guardian", 0, 250000L, 0, walletEligible, true, false);
        }
        if (points >= 1000) {
            return benefit("Pillar", 0, 100000L, 10, walletEligible, false, false);
        }
        if (points >= 500) {
            return benefit("Contributor", 0, 50000L, 20, walletEligible, false, false);
        }
        return benefit("Resident", 0, 0L, 100, false, false, false);
    }

    private MilestoneBenefit humanMilestone(SubjectRef subject, Integer legacyCredit) {
        int points = dataAccess.currentPoints(subject, 0);
        CreditProfileSummary credit = dataAccess.creditProfile(subject, legacyCredit);
        int score = credit.score() != null ? credit.score() : 300;

        if (points >= 5000) {
            return benefit("Legend", Integer.MAX_VALUE, 1000000L, 0, true, true, true);
        }
        if (points >= 2000) {
            return benefit("Guardian", 50, 500000L, 0, true, true, false);
        }
        if (points >= 1000 && score >= 575) {
            return benefit("Pillar", 10, 200000L, 5, true, false, false);
        }
        if (points >= 500 && score >= 575) {
            return benefit("Contributor", 5, 100000L, 10, true, false, false);
        }
        if (points >= 200) {
            return benefit("Resident", 2, 25000L, 30, true, false, false);
        }
        return benefit(points >= 50 ? "Visitor" : "Newcomer", 1, 0L, 100, true, false, false);
    }

    private static MilestoneBenefit base(boolean walletEligible, int maxOwnedAgents, long noDepositLimit, String title) {
        return benefit(title, maxOwnedAgents, noDepositLimit, 100, walletEligible, false, false);
    }

    private static MilestoneBenefit benefit(
            String title,
            Integer maxOwnedAgents,
            Long noDepositLimit,
            Integer depositRatePercent,
            Boolean walletEligible,
            Boolean canArbitrate,
            Boolean canParticipateRuleGovernance
    ) {
        return new MilestoneBenefit(
                title,
                maxOwnedAgents,
                noDepositLimit,
                depositRatePercent,
                walletEligible,
                canArbitrate,
                canParticipateRuleGovernance
        );
    }
}
