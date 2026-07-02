package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.framework.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WalletPolicyServiceImpl implements WalletPolicyApi {

    private static final int AGENT_DIRECT_WALLET_POINTS = 500;
    private static final int STATUS_REASON_MAX_LENGTH = 200;

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
        ActorDataAccess.AgentWalletState walletState = dataAccess.agentWalletState(agentId);
        if (walletState != null && !walletState.walletEnabled()) {
            return WalletCapability.agentToOwner(agentId, ownerId, reasonOrDefault(walletState.statusReason()));
        }

        int points = dataAccess.currentPoints(SubjectRef.agent(agentId), 0);
        if (points < AGENT_DIRECT_WALLET_POINTS) {
            return WalletCapability.agentToOwner(agentId, ownerId, "agent points below 500");
        }

        if (walletState != null) {
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

    @Override
    public WalletCapability enableAgentWallet(Long principalHumanId, Long agentId) {
        ActorSubjectValidation validation = requireOwnedAgent(principalHumanId, agentId);
        int points = dataAccess.currentPoints(SubjectRef.agent(agentId), 0);
        if (points < AGENT_DIRECT_WALLET_POINTS) {
            throw BizException.of("agent.wallet.points_insufficient");
        }
        boolean updated = dataAccess.upsertAgentWalletState(
                agentId,
                true,
                principalHumanId,
                "owner enabled after reaching 500 behavior points"
        );
        if (!updated) {
            throw BizException.of("agent.wallet.state_unavailable");
        }
        return resolveWallet(SubjectRef.agent(agentId));
    }

    @Override
    public WalletCapability disableAgentWallet(Long principalHumanId, Long agentId, String reason) {
        requireOwnedAgent(principalHumanId, agentId);
        String statusReason = boundedReason(reason, "owner disabled agent wallet");
        boolean updated = dataAccess.upsertAgentWalletState(agentId, false, principalHumanId, statusReason);
        if (!updated) {
            throw BizException.of("agent.wallet.state_unavailable");
        }
        return resolveWallet(SubjectRef.agent(agentId));
    }

    private static String reasonOrDefault(String reason) {
        return reason == null || reason.isBlank() ? "owner has not enabled agent wallet" : reason;
    }

    private static String boundedReason(String reason, String fallback) {
        String value = StringUtils.hasText(reason) ? reason.trim() : fallback;
        return value.length() <= STATUS_REASON_MAX_LENGTH ? value : value.substring(0, STATUS_REASON_MAX_LENGTH);
    }

    private ActorSubjectValidation requireOwnedAgent(Long principalHumanId, Long agentId) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (agentId == null) {
            throw BizException.of("agent.required");
        }
        ActorSubjectValidation validation = subjectValidator.validateAgentLiability(agentId);
        if (!validation.valid()) {
            throw BizException.of("agent.wallet.forbidden");
        }
        if (validation.human() == null || !principalHumanId.equals(validation.human().getId())) {
            throw BizException.of("agent.wallet.forbidden");
        }
        return validation;
    }
}
