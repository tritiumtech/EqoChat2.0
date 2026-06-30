package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityCode;
import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.CapabilityState;
import com.eqochat.business.actor.api.model.MilestoneBenefit;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.CapabilityQueryApi;
import com.eqochat.business.actor.api.service.MilestonePolicyApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CapabilityQueryServiceImpl implements CapabilityQueryApi {

    private final ActorSubjectValidator subjectValidator;
    private final WalletPolicyApi walletPolicyApi;
    private final MilestonePolicyApi milestonePolicyApi;

    @Override
    public Capability getCapability(SubjectRef ref, CapabilityCode code) {
        CapabilitySet set = getCapabilities(ref);
        Capability capability = set.get(code);
        return capability != null ? capability : Capability.disabled(code, "capability is not defined");
    }

    @Override
    public CapabilitySet getCapabilities(SubjectRef ref) {
        ActorSubjectValidation validation = subjectValidator.validateOrdinaryCapabilities(ref);
        if (!validation.valid()) {
            return disabledAll(validation.reason());
        }

        WalletCapability wallet = walletPolicyApi.resolveWallet(ref);
        MilestoneBenefit milestone = milestonePolicyApi.resolveMilestone(ref);

        List<Capability> items = new ArrayList<>();
        if (ref.isHuman()) {
            items.add(Capability.enabled(CapabilityCode.LOGIN));
            items.add(Capability.enabled(CapabilityCode.CREATE_AGENT));
            items.add(Capability.enabled(CapabilityCode.POST_WORLD));
            items.add(Capability.enabled(CapabilityCode.SEND_MESSAGE));
            items.add(Capability.enabled(CapabilityCode.JOIN_PROJECT));
            items.add(Capability.enabled(CapabilityCode.OWN_PROJECT));
            items.add(Capability.enabled(CapabilityCode.BID_PROJECT));
            items.add(Capability.enabled(CapabilityCode.PAY_PROJECT_DEPOSIT));
            items.add(Capability.enabled(CapabilityCode.RECEIVE_PAYMENT));
            items.add(new Capability(CapabilityCode.WALLET, wallet.state(), wallet.reason()));
            items.add(Capability.enabled(CapabilityCode.AUTONOMOUS_ACTION));
            items.add(Boolean.TRUE.equals(milestone.canArbitrate())
                    ? Capability.enabled(CapabilityCode.ARBITRATE)
                    : Capability.disabled(CapabilityCode.ARBITRATE, "milestone not reached"));
            items.add(Boolean.TRUE.equals(milestone.canParticipateRuleGovernance())
                    ? Capability.enabled(CapabilityCode.PARTICIPATE_RULE_GOVERNANCE)
                    : Capability.disabled(CapabilityCode.PARTICIPATE_RULE_GOVERNANCE, "milestone not reached"));
            return new CapabilitySet(items);
        }

        items.add(Capability.disabled(CapabilityCode.LOGIN, "agent does not use the human login flow"));
        items.add(Capability.disabled(CapabilityCode.CREATE_AGENT, "agent creation is owned by humans"));
        items.add(Capability.enabled(CapabilityCode.POST_WORLD));
        items.add(Capability.enabled(CapabilityCode.SEND_MESSAGE));
        items.add(Capability.enabled(CapabilityCode.JOIN_PROJECT));
        items.add(Capability.enabled(CapabilityCode.OWN_PROJECT));
        items.add(Capability.enabled(CapabilityCode.BID_PROJECT));
        items.add(wallet.state() == CapabilityState.ENABLED
                ? Capability.enabled(CapabilityCode.PAY_PROJECT_DEPOSIT)
                : Capability.pending(CapabilityCode.PAY_PROJECT_DEPOSIT, "payment routes through owner wallet"));
        items.add(Capability.enabled(CapabilityCode.RECEIVE_PAYMENT));
        items.add(new Capability(CapabilityCode.WALLET, wallet.state(), wallet.reason()));
        items.add(Capability.enabled(CapabilityCode.AUTONOMOUS_ACTION));
        items.add(Boolean.TRUE.equals(milestone.canArbitrate())
                ? Capability.enabled(CapabilityCode.ARBITRATE)
                : Capability.disabled(CapabilityCode.ARBITRATE, "milestone not reached"));
        items.add(Capability.disabled(CapabilityCode.PARTICIPATE_RULE_GOVERNANCE, "agent governance is not enabled"));
        return new CapabilitySet(items);
    }

    private static CapabilitySet disabledAll(String reason) {
        List<Capability> disabled = java.util.Arrays.stream(CapabilityCode.values())
                .map(code -> Capability.disabled(code, reason))
                .toList();
        return new CapabilitySet(disabled);
    }
}
