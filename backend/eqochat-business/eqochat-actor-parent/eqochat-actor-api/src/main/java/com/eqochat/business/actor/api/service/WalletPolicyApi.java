package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.WalletCapability;

public interface WalletPolicyApi {

    WalletCapability resolveWallet(SubjectRef subject);

    WalletCapability enableAgentWallet(Long principalHumanId, Long agentId);

    WalletCapability disableAgentWallet(Long principalHumanId, Long agentId, String reason);
}
