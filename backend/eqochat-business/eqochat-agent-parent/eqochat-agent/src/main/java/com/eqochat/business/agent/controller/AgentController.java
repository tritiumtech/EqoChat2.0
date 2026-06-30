package com.eqochat.business.agent.controller;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.CapabilityState;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.business.agent.entity.AgentBinding;
import com.eqochat.business.agent.entity.AgentProfile;
import com.eqochat.business.agent.api.dto.response.AgentMeResponse;
import com.eqochat.business.agent.mapper.AgentBindingMapper;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.credit.entity.CreditRecord;
import com.eqochat.business.credit.mapper.CreditRecordMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final CreditRecordMapper creditRecordMapper;
    private final SubjectDirectoryApi subjectDirectoryApi;
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
            List<String> capabilities = parseCapabilities(agent.getCapabilityTags());

            SubjectRef agentRef = SubjectRef.agent(agent.getId());
            SubjectSummaryResponse agentSubject = subjectDirectoryApi.getSubject(agentRef);
            Integer creditScore = agentSubject != null && agentSubject.getCreditScore() != null
                    ? agentSubject.getCreditScore()
                    : (agent.getCreditScore() != null ? agent.getCreditScore() : 0);

            long earnings = computeEarnings(agent.getId());
            WalletCapability wallet = walletPolicyApi.resolveWallet(agentRef);
            boolean walletEnabled = wallet != null && wallet.state() == CapabilityState.ENABLED;
            LiabilityChain liability = liabilityPolicyApi.resolveLiability(agentRef);
            AgentBinding binding = agentBindingMapper.findByAgentIdAndOwnerId(agent.getId(), agent.getOwnerId())
                    .orElse(null);
            boolean liabilityAccepted = binding != null && Boolean.TRUE.equals(binding.getLiabilityAccepted());
            SubjectSummaryResponse owner = agent.getOwnerId() != null
                    ? subjectDirectoryApi.getSubject(SubjectRef.human(agent.getOwnerId()))
                    : null;

            out.add(
                    AgentMeResponse.builder()
                            .id(agent.getId())
                            .name(agent.getName())
                            .avatarUrl(agent.getAvatarUrl())
                            .description(agent.getDescription())
                            .agentType(agent.getAgentType() != null ? agent.getAgentType().name() : null)
                            .permissionLevel(agent.getPermissionLevel())
                            .creditScore(creditScore)
                            .ownerId(agent.getOwnerId())
                            .ownerName(owner != null ? owner.getDisplayName() : null)
                            .ownerType("human")
                            .capabilities(capabilities)
                            .liabilityAccepted(liabilityAccepted)
                            .walletEnabled(walletEnabled)
                            .walletRouting(wallet != null ? wallet.routing() : null)
                            .responsibilityChain(liability != null ? liability.route() : null)
                            .earnings(earnings)
                            .build()
            );
        }

        return ApiResponse.success(out);
    }

    private long computeEarnings(Long agentId) {
        if (agentId == null) return 0;
        List<CreditRecord> records = creditRecordMapper.findBySubject(agentId, CreditRecord.SubjectType.AGENT.name());
        if (records == null || records.isEmpty()) return 0;
        return records.stream()
                .filter(r -> r != null && r.getChangeAmount() != null && r.getChangeAmount() > 0)
                .mapToLong(r -> r.getChangeAmount().longValue())
                .sum();
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
}
