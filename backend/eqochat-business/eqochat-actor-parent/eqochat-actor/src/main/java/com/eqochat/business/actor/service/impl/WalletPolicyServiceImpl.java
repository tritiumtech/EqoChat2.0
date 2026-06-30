package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletPolicyServiceImpl implements WalletPolicyApi {

    private static final int AGENT_DIRECT_WALLET_POINTS = 500;

    private final ActorSubjectValidator subjectValidator;
    private final ActorDataAccess dataAccess;

    @Override
    public WalletCapability resolveWallet(SubjectRef subject) {
        if (subject == null) {
            return WalletCapability.unavailable(null, "subject is null");
        }
        if (subject.id() == null) {
            return WalletCapability.unavailable(subject, "subject id is null");
        }
        if (subject.isHuman()) {
            ActorSubjectValidation validation = subjectValidator.validateHuman(subject.id());
            return validation.valid()
                    ? WalletCapability.humanEnabled(subject.id())
                    : WalletCapability.unavailable(subject, validation.reason());
        }
        if (!subject.isAgent()) {
            return WalletCapability.unavailable(subject, "system subject has no wallet");
        }

        ActorSubjectValidation validation = subjectValidator.validateAgentLiability(subject.id());
        if (!validation.valid()) {
            return WalletCapability.unavailable(subject, validation.reason());
        }

        Long agentId = validation.agent().getId();
        Long ownerId = validation.human().getId();
        int points = dataAccess.currentPoints(SubjectRef.agent(agentId), 0);
        if (points < AGENT_DIRECT_WALLET_POINTS) {
            return WalletCapability.agentToOwner(agentId, ownerId, "agent points below 500");
        }

        ActorDataAccess.AgentWalletState walletState = dataAccess.agentWalletState(agentId);
        if (walletState != null) {
            if (!walletState.walletEnabled()) {
                return WalletCapability.agentToOwner(agentId, ownerId, reasonOrDefault(walletState.statusReason()));
            }
            if (walletState.enabledBy() == null || !walletState.enabledBy().equals(ownerId)) {
                return WalletCapability.agentToOwner(agentId, ownerId, "agent wallet was not enabled by current owner");
            }
            return WalletCapability.agentDirect(agentId);
        }

        if (dataAccess.sourceConfigWalletEnabled(validation.agent().getSourceConfig())) {
            return WalletCapability.agentDirect(agentId, "legacy/local source_config wallet enabled fallback");
        }

        return WalletCapability.agentToOwner(agentId, ownerId, "owner has not enabled agent wallet");
    }

    private static String reasonOrDefault(String reason) {
        return reason == null || reason.isBlank() ? "owner has not enabled agent wallet" : reason;
    }
}
