package com.eqochat.business.agent.controller;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityState;
import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.CapabilityQueryApi;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.SubjectRegistrySyncApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.business.agent.api.dto.request.AgentWalletUpdateRequest;
import com.eqochat.business.agent.entity.AgentBinding;
import com.eqochat.business.agent.entity.AgentProfile;
import com.eqochat.business.agent.api.dto.response.AgentMeResponse;
import com.eqochat.business.agent.mapper.AgentBindingMapper;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.credit.api.service.CreditEarningsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentProfileMapper agentProfileMapper;
    private final AgentBindingMapper agentBindingMapper;
    private final CreditEarningsService creditEarningsService;
    private final SubjectDirectoryApi subjectDirectoryApi;
    private final SubjectRegistrySyncApi subjectRegistrySyncApi;
    private final CapabilityQueryApi capabilityQueryApi;
    private final WalletPolicyApi walletPolicyApi;
    private final LiabilityPolicyApi liabilityPolicyApi;
    private final ObjectMapper objectMapper;

    @GetMapping("/me")
    public ApiResponse<List<AgentMeResponse>> listMyAgents() {
        Long ownerId = UserContext.requireCurrentUser();
        List<AgentProfile> agents = agentProfileMapper.findActiveByOwnerId(ownerId);
        if (agents == null || agents.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }

        List<AgentMeResponse> out = new ArrayList<>();
        for (AgentProfile agent : agents) {
            syncAgent(agent.getId());
            List<String> profileCapabilities = parseCapabilities(agent.getCapabilityTags());

            SubjectRef agentRef = SubjectRef.agent(agent.getId());
            SubjectSummaryResponse agentSubject = subjectDirectoryApi.getSubject(agentRef);
            Integer creditScore = agentSubject != null && agentSubject.getCreditScore() != null
                    ? agentSubject.getCreditScore()
                    : (agent.getCreditScore() != null ? agent.getCreditScore() : 0);

            long earnings = computeEarnings(agent.getId());
            WalletCapability wallet = walletPolicyApi.resolveWallet(agentRef);
            boolean walletEnabled = wallet != null && wallet.state() == CapabilityState.ENABLED;
            LiabilityChain liability = liabilityPolicyApi.resolveLiability(agentRef);
            CapabilitySet capabilitySet = capabilityQueryApi.getCapabilities(agentRef);
            List<AgentMeResponse.CapabilityPolicyItem> capabilityPolicy = toCapabilityPolicy(capabilitySet);
            List<String> capabilities = hasPolicyCapabilities(capabilitySet)
                    ? enabledCapabilityCodes(capabilitySet)
                    : profileCapabilities;
            AgentBinding binding = agentBindingMapper.findByAgentIdAndOwnerId(agent.getId(), agent.getOwnerId())
                    .orElse(null);
            boolean bindingLiabilityAccepted = binding != null && Boolean.TRUE.equals(binding.getLiabilityAccepted());
            SubjectSummaryResponse owner = agent.getOwnerId() != null
                    ? subjectDirectoryApi.getSubject(SubjectRef.human(agent.getOwnerId()))
                    : null;
            SubjectRef directRecipient = wallet != null && wallet.directRecipient() != null
                    ? wallet.directRecipient()
                    : agentRef;
            SubjectRef settlementSubject = wallet != null ? wallet.settlementSubject() : directRecipient;

            out.add(
                    AgentMeResponse.builder()
                            .id(agent.getId())
                            .name(agent.getName())
                            .avatarUrl(agent.getAvatarUrl())
                            .description(agent.getDescription())
                            .agentType(agent.getAgentType() != null ? agent.getAgentType().name() : null)
                            .permissionLevel(agent.getPermissionLevel())
                            .creditScore(creditScore)
                            .agentSubjectId(agentRef.id())
                            .agentSubjectType(agentRef.type())
                            .ownerSubjectId(agent.getOwnerId())
                            .ownerSubjectType(agent.getOwnerId() != null ? SubjectType.HUMAN : null)
                            .ownerId(agent.getOwnerId())
                            .ownerName(owner != null ? owner.getDisplayName() : null)
                            .ownerType(agent.getOwnerId() != null ? SubjectType.HUMAN.jsonValue() : null)
                            .capabilities(capabilities)
                            .profileCapabilities(profileCapabilities)
                            .capabilityPolicy(capabilityPolicy)
                            .liabilityAccepted(liability != null && liability.liableHumanId() != null)
                            .bindingLiabilityAccepted(bindingLiabilityAccepted)
                            .liableHumanId(liability != null ? liability.liableHumanId() : null)
                            .liabilityRoute(liability != null ? liability.route() : null)
                            .liabilityReason(liability != null ? liability.reason() : null)
                            .walletEnabled(walletEnabled)
                            .walletPolicyState(wallet != null && wallet.state() != null ? wallet.state().name() : null)
                            .walletRouting(wallet != null ? wallet.routing() : null)
                            .walletPolicyReason(wallet != null ? wallet.reason() : null)
                            .directRecipientSubjectId(directRecipient.id())
                            .directRecipientSubjectType(directRecipient.type())
                            .settlementSubjectId(settlementSubject != null ? settlementSubject.id() : null)
                            .settlementSubjectType(settlementSubject != null ? settlementSubject.type() : null)
                            .settlementHumanId(wallet != null ? wallet.settlementHumanId() : null)
                            .financialAutonomy(wallet != null ? Boolean.TRUE.equals(wallet.financialAutonomy()) : false)
                            .responsibilityChain(liability != null ? liability.route() : null)
                            .earnings(earnings)
                            .build()
            );
        }

        return ApiResponse.success(out);
    }

    @PostMapping("/{agentId}/wallet/enable")
    public ApiResponse<AgentMeResponse.WalletPolicyResponse> enableWallet(@PathVariable Long agentId) {
        Long ownerId = UserContext.requireCurrentUser();
        WalletCapability wallet = walletPolicyApi.enableAgentWallet(ownerId, agentId);
        return ApiResponse.success(toWalletPolicyResponse(wallet));
    }

    @PostMapping("/{agentId}/wallet/disable")
    public ApiResponse<AgentMeResponse.WalletPolicyResponse> disableWallet(
            @PathVariable Long agentId,
            @RequestBody(required = false) AgentWalletUpdateRequest request
    ) {
        Long ownerId = UserContext.requireCurrentUser();
        String reason = request != null ? request.getReason() : null;
        WalletCapability wallet = walletPolicyApi.disableAgentWallet(ownerId, agentId, reason);
        return ApiResponse.success(toWalletPolicyResponse(wallet));
    }

    private long computeEarnings(Long agentId) {
        if (agentId == null) return 0;
        return creditEarningsService.positiveChangeTotal(agentId, SubjectType.AGENT.name());
    }

    private void syncAgent(Long agentId) {
        try {
            subjectRegistrySyncApi.syncAgent(agentId);
        } catch (RuntimeException ignored) {
            // Registry sync is best-effort; agent_profile remains the source of truth.
        }
    }

    private List<String> parseCapabilities(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null) return Collections.emptyList();
            if (node.isArray()) {
                List<String> list = new ArrayList<>();
                for (JsonNode v : node) {
                    if (v != null && v.isTextual()) list.add(v.asText());
                }
                return list;
            }
            if (node.isObject()) {
                JsonNode caps = node.get("capabilities");
                if (caps != null && caps.isArray()) {
                    List<String> list = new ArrayList<>();
                    for (JsonNode v : caps) {
                        if (v != null && v.isTextual()) list.add(v.asText());
                    }
                    return list;
                }
            }
        } catch (IOException ignore) {
            // ignore parse errors
        }
        return Collections.emptyList();
    }

    private boolean hasPolicyCapabilities(CapabilitySet set) {
        return set != null && !set.capabilities().isEmpty();
    }

    private List<AgentMeResponse.CapabilityPolicyItem> toCapabilityPolicy(CapabilitySet set) {
        if (set == null || set.capabilities().isEmpty()) {
            return Collections.emptyList();
        }
        return set.capabilities().stream()
                .filter(item -> item != null && item.code() != null)
                .map(item -> AgentMeResponse.CapabilityPolicyItem.builder()
                        .code(item.code().name())
                        .state(item.state() != null ? item.state().name() : null)
                        .reason(item.reason())
                        .build())
                .toList();
    }

    private List<String> enabledCapabilityCodes(CapabilitySet set) {
        if (set == null || set.capabilities().isEmpty()) {
            return Collections.emptyList();
        }
        return set.capabilities().stream()
                .filter(this::isDisplayableCapability)
                .map(item -> item.code().name())
                .toList();
    }

    private boolean isDisplayableCapability(Capability item) {
        return item != null
                && item.code() != null
                && (item.state() == CapabilityState.ENABLED || item.state() == CapabilityState.PENDING_APPROVAL);
    }

    private AgentMeResponse.WalletPolicyResponse toWalletPolicyResponse(WalletCapability wallet) {
        SubjectRef directRecipient = wallet != null && wallet.directRecipient() != null
                ? wallet.directRecipient()
                : null;
        SubjectRef settlementSubject = wallet != null ? wallet.settlementSubject() : directRecipient;
        return AgentMeResponse.WalletPolicyResponse.builder()
                .walletEnabled(wallet != null && wallet.state() == CapabilityState.ENABLED)
                .walletPolicyState(wallet != null && wallet.state() != null ? wallet.state().name() : null)
                .walletRouting(wallet != null ? wallet.routing() : null)
                .walletPolicyReason(wallet != null ? wallet.reason() : null)
                .directRecipientSubjectId(directRecipient != null ? directRecipient.id() : null)
                .directRecipientSubjectType(directRecipient != null ? directRecipient.type() : null)
                .settlementSubjectId(settlementSubject != null ? settlementSubject.id() : null)
                .settlementSubjectType(settlementSubject != null ? settlementSubject.type() : null)
                .settlementHumanId(wallet != null ? wallet.settlementHumanId() : null)
                .financialAutonomy(wallet != null ? Boolean.TRUE.equals(wallet.financialAutonomy()) : false)
                .build();
    }
}
