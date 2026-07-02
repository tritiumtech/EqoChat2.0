package com.eqochat.business.agent.controller;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityCode;
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
import com.eqochat.business.agent.api.dto.response.AgentMeResponse;
import com.eqochat.business.agent.entity.AgentBinding;
import com.eqochat.business.agent.entity.AgentProfile;
import com.eqochat.business.agent.mapper.AgentBindingMapper;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.credit.api.service.CreditEarningsService;
import com.eqochat.framework.common.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentControllerActorPolicyTest {

    @Mock
    AgentProfileMapper agentProfileMapper;
    @Mock
    AgentBindingMapper agentBindingMapper;
    @Mock
    CreditEarningsService creditEarningsService;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    SubjectRegistrySyncApi subjectRegistrySyncApi;
    @Mock
    CapabilityQueryApi capabilityQueryApi;
    @Mock
    WalletPolicyApi walletPolicyApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;

    AgentController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(9L);
        controller = new AgentController(
                agentProfileMapper,
                agentBindingMapper,
                creditEarningsService,
                subjectDirectoryApi,
                subjectRegistrySyncApi,
                capabilityQueryApi,
                walletPolicyApi,
                liabilityPolicyApi,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listMyAgentsReturnsCanonicalPolicyFieldsForEnabledWallet() {
        AgentProfile agent = agent(101L, 9L, "[\"legacy_chat\"]");
        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L)))
                .thenReturn(subject(101L, SubjectType.AGENT, "Agent Alpha", 620));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L)))
                .thenReturn(subject(9L, SubjectType.HUMAN, "Owner Human", 700));
        when(creditEarningsService.positiveChangeTotal(101L, "AGENT")).thenReturn(0L);
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(101L)))
                .thenReturn(WalletCapability.agentDirect(101L, "milestone approved"));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L)))
                .thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(capabilityQueryApi.getCapabilities(SubjectRef.agent(101L))).thenReturn(new CapabilitySet(List.of(
                Capability.enabled(CapabilityCode.POST_WORLD),
                Capability.enabled(CapabilityCode.SEND_MESSAGE),
                Capability.disabled(CapabilityCode.LOGIN, "agent does not login")
        )));
        when(agentBindingMapper.findByAgentIdAndOwnerId(101L, 9L))
                .thenReturn(Optional.of(binding(101L, 9L, true)));

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.getAgentSubjectId()).isEqualTo(101L);
        assertThat(response.getAgentSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getOwnerSubjectId()).isEqualTo(9L);
        assertThat(response.getOwnerSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getOwnerId()).isEqualTo(9L);
        assertThat(response.getOwnerType()).isEqualTo("human");
        assertThat(response.isLiabilityAccepted()).isTrue();
        assertThat(response.getBindingLiabilityAccepted()).isTrue();
        assertThat(response.getLiableHumanId()).isEqualTo(9L);
        assertThat(response.getLiabilityRoute()).isEqualTo("agent:101->human:9");
        assertThat(response.getLiabilityReason()).isNull();
        assertThat(response.isWalletEnabled()).isTrue();
        assertThat(response.getWalletPolicyState()).isEqualTo("ENABLED");
        assertThat(response.getWalletRouting()).isEqualTo("AGENT_DIRECT");
        assertThat(response.getWalletPolicyReason()).isEqualTo("milestone approved");
        assertThat(response.getDirectRecipientSubjectId()).isEqualTo(101L);
        assertThat(response.getDirectRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementSubjectId()).isEqualTo(101L);
        assertThat(response.getSettlementSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementHumanId()).isNull();
        assertThat(response.getFinancialAutonomy()).isTrue();
        assertThat(response.getResponsibilityChain()).isEqualTo("agent:101->human:9");
        assertThat(response.getProfileCapabilities()).containsExactly("legacy_chat");
        assertThat(response.getCapabilities()).containsExactly("POST_WORLD", "SEND_MESSAGE");
        assertThat(response.getCapabilityPolicy())
                .extracting(AgentMeResponse.CapabilityPolicyItem::getCode)
                .containsExactly("POST_WORLD", "SEND_MESSAGE", "LOGIN");
        verify(capabilityQueryApi).getCapabilities(SubjectRef.agent(101L));
        verify(subjectRegistrySyncApi).syncAgent(101L);
    }

    @Test
    void listMyAgentsReturnsDisabledWalletFactsAndDoesNotFallbackWhenPolicySetExists() {
        AgentProfile agent = agent(102L, 9L, "[\"legacy_wallet\"]");
        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(102L)))
                .thenReturn(subject(102L, SubjectType.AGENT, "Agent Beta", 610));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L)))
                .thenReturn(subject(9L, SubjectType.HUMAN, "Owner Human", 700));
        when(creditEarningsService.positiveChangeTotal(102L, "AGENT")).thenReturn(0L);
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(102L)))
                .thenReturn(WalletCapability.agentToOwner(102L, 9L, "owner wallet required"));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(102L)))
                .thenReturn(LiabilityChain.agentToHuman(102L, 9L));
        when(capabilityQueryApi.getCapabilities(SubjectRef.agent(102L))).thenReturn(new CapabilitySet(List.of(
                Capability.disabled(CapabilityCode.WALLET, "owner wallet required")
        )));
        when(agentBindingMapper.findByAgentIdAndOwnerId(102L, 9L))
                .thenReturn(Optional.of(binding(102L, 9L, false)));

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.isWalletEnabled()).isFalse();
        assertThat(response.getWalletPolicyState()).isEqualTo("DISABLED");
        assertThat(response.getWalletRouting()).isEqualTo("AGENT_TO_OWNER");
        assertThat(response.getWalletPolicyReason()).isEqualTo("owner wallet required");
        assertThat(response.getDirectRecipientSubjectId()).isEqualTo(102L);
        assertThat(response.getDirectRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementSubjectId()).isEqualTo(9L);
        assertThat(response.getSettlementSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getSettlementHumanId()).isEqualTo(9L);
        assertThat(response.getFinancialAutonomy()).isFalse();
        assertThat(response.isLiabilityAccepted()).isTrue();
        assertThat(response.getBindingLiabilityAccepted()).isFalse();
        assertThat(response.getCapabilities()).isEmpty();
        assertThat(response.getProfileCapabilities()).containsExactly("legacy_wallet");
        assertThat(response.getCapabilityPolicy())
                .extracting(AgentMeResponse.CapabilityPolicyItem::getCode)
                .containsExactly("WALLET");
    }

    @Test
    void listMyAgentsExposesUnresolvedLiability() {
        AgentProfile agent = agent(103L, 9L, null);
        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(103L)))
                .thenReturn(subject(103L, SubjectType.AGENT, "Agent Gamma", 600));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L)))
                .thenReturn(subject(9L, SubjectType.HUMAN, "Owner Human", 700));
        when(creditEarningsService.positiveChangeTotal(103L, "AGENT")).thenReturn(0L);
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(103L)))
                .thenReturn(WalletCapability.agentDirect(103L));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(103L)))
                .thenReturn(LiabilityChain.unresolved(SubjectRef.agent(103L), "owner binding missing"));
        when(capabilityQueryApi.getCapabilities(SubjectRef.agent(103L)))
                .thenReturn(new CapabilitySet(List.of(Capability.enabled(CapabilityCode.POST_WORLD))));
        when(agentBindingMapper.findByAgentIdAndOwnerId(103L, 9L)).thenReturn(Optional.empty());

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.getLiableHumanId()).isNull();
        assertThat(response.getLiabilityRoute()).isEqualTo("agent:103->human:unknown");
        assertThat(response.getLiabilityReason()).isEqualTo("owner binding missing");
        assertThat(response.isLiabilityAccepted()).isFalse();
        assertThat(response.getBindingLiabilityAccepted()).isFalse();
    }

    @Test
    void listMyAgentsFallsBackToProfileCapabilitiesOnlyWhenPolicySetIsEmpty() {
        AgentProfile agent = agent(104L, 9L, "{\"capabilities\":[\"legacy_chat\",\"legacy_tools\"]}");
        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(104L)))
                .thenReturn(subject(104L, SubjectType.AGENT, "Agent Delta", 605));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L)))
                .thenReturn(subject(9L, SubjectType.HUMAN, "Owner Human", 700));
        when(creditEarningsService.positiveChangeTotal(104L, "AGENT")).thenReturn(0L);
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(104L)))
                .thenReturn(WalletCapability.agentDirect(104L));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(104L)))
                .thenReturn(LiabilityChain.agentToHuman(104L, 9L));
        when(capabilityQueryApi.getCapabilities(SubjectRef.agent(104L)))
                .thenReturn(new CapabilitySet(List.of()));
        when(agentBindingMapper.findByAgentIdAndOwnerId(104L, 9L)).thenReturn(Optional.empty());

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.getCapabilities()).containsExactly("legacy_chat", "legacy_tools");
        assertThat(response.getProfileCapabilities()).containsExactly("legacy_chat", "legacy_tools");
        assertThat(response.getCapabilityPolicy()).isEmpty();
    }

    @Test
    void enableAndDisableWalletDelegateToOwnerPolicyAndReturnCanonicalFacts() {
        when(walletPolicyApi.enableAgentWallet(9L, 101L)).thenReturn(WalletCapability.agentDirect(101L));

        AgentMeResponse.WalletPolicyResponse enabled = controller.enableWallet(101L).getData();

        assertThat(enabled.isWalletEnabled()).isTrue();
        assertThat(enabled.getWalletPolicyState()).isEqualTo("ENABLED");
        assertThat(enabled.getWalletRouting()).isEqualTo("AGENT_DIRECT");
        assertThat(enabled.getDirectRecipientSubjectId()).isEqualTo(101L);
        assertThat(enabled.getDirectRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(enabled.getSettlementSubjectId()).isEqualTo(101L);
        assertThat(enabled.getSettlementSubjectType()).isEqualTo(SubjectType.AGENT);
        verify(walletPolicyApi).enableAgentWallet(9L, 101L);

        when(walletPolicyApi.disableAgentWallet(9L, 101L, "risk review"))
                .thenReturn(WalletCapability.agentToOwner(101L, 9L, "risk review"));

        AgentWalletUpdateRequest request = new AgentWalletUpdateRequest();
        request.setReason("risk review");
        AgentMeResponse.WalletPolicyResponse disabled = controller.disableWallet(101L, request).getData();

        assertThat(disabled.isWalletEnabled()).isFalse();
        assertThat(disabled.getWalletPolicyState()).isEqualTo("DISABLED");
        assertThat(disabled.getWalletRouting()).isEqualTo("AGENT_TO_OWNER");
        assertThat(disabled.getWalletPolicyReason()).isEqualTo("risk review");
        assertThat(disabled.getSettlementSubjectId()).isEqualTo(9L);
        assertThat(disabled.getSettlementSubjectType()).isEqualTo(SubjectType.HUMAN);
        verify(walletPolicyApi).disableAgentWallet(9L, 101L, "risk review");
    }

    private static AgentProfile agent(Long id, Long ownerId, String capabilityTags) {
        return AgentProfile.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Agent " + id)
                .description("test agent")
                .agentType(AgentProfile.AgentType.ASSISTANT)
                .status(AgentProfile.AgentStatus.ACTIVE)
                .permissionLevel("standard")
                .creditScore(500)
                .capabilityTags(capabilityTags)
                .build();
    }

    private static SubjectSummaryResponse subject(Long id, SubjectType type, String name, Integer creditScore) {
        return SubjectSummaryResponse.builder()
                .id(id)
                .type(type)
                .displayName(name)
                .creditScore(creditScore)
                .build();
    }

    private static AgentBinding binding(Long agentId, Long ownerId, boolean liabilityAccepted) {
        return AgentBinding.builder()
                .agentId(agentId)
                .ownerId(ownerId)
                .liabilityAccepted(liabilityAccepted)
                .build();
    }
}
