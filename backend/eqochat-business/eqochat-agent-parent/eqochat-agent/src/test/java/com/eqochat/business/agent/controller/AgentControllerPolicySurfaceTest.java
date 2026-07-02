package com.eqochat.business.agent.controller;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityCode;
import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.CapabilityQueryApi;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.SubjectRegistrySyncApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
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
class AgentControllerPolicySurfaceTest {

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
    void listMyAgentsExposesCanonicalOwnerRoutedPolicySurface() {
        AgentProfile agent = agent(101L, 9L, "[\"legacy-tag\"]");
        SubjectRef agentRef = SubjectRef.agent(101L);

        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(agentRef)).thenReturn(subject(101L, SubjectType.AGENT, "Nova", 612));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L))).thenReturn(subject(9L, SubjectType.HUMAN, "Ava", 700));
        when(walletPolicyApi.resolveWallet(agentRef))
                .thenReturn(WalletCapability.agentToOwner(101L, 9L, "owner has not enabled agent wallet"));
        when(liabilityPolicyApi.resolveLiability(agentRef)).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(capabilityQueryApi.getCapabilities(agentRef)).thenReturn(new CapabilitySet(List.of(
                Capability.enabled(CapabilityCode.SEND_MESSAGE),
                Capability.pending(CapabilityCode.PAY_PROJECT_DEPOSIT, "payment routes through owner wallet"),
                Capability.disabled(CapabilityCode.LOGIN, "agent does not use the human login flow")
        )));
        when(agentBindingMapper.findByAgentIdAndOwnerId(101L, 9L)).thenReturn(Optional.of(binding(101L, 9L, false)));
        when(creditEarningsService.positiveChangeTotal(101L, "AGENT")).thenReturn(32L);

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.getAgentSubjectId()).isEqualTo(101L);
        assertThat(response.getAgentSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getOwnerSubjectId()).isEqualTo(9L);
        assertThat(response.getOwnerSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getOwnerType()).isEqualTo("human");
        assertThat(response.getCreditScore()).isEqualTo(612);
        assertThat(response.getCapabilities()).containsExactly("SEND_MESSAGE", "PAY_PROJECT_DEPOSIT");
        assertThat(response.getProfileCapabilities()).containsExactly("legacy-tag");
        assertThat(response.getCapabilityPolicy())
                .extracting(AgentMeResponse.CapabilityPolicyItem::getCode)
                .containsExactly("SEND_MESSAGE", "PAY_PROJECT_DEPOSIT", "LOGIN");
        assertThat(response.isLiabilityAccepted()).isTrue();
        assertThat(response.getBindingLiabilityAccepted()).isFalse();
        assertThat(response.getLiableHumanId()).isEqualTo(9L);
        assertThat(response.getLiabilityRoute()).isEqualTo("agent:101->human:9");
        assertThat(response.getResponsibilityChain()).isEqualTo("agent:101->human:9");
        assertThat(response.isWalletEnabled()).isFalse();
        assertThat(response.getWalletPolicyState()).isEqualTo("DISABLED");
        assertThat(response.getWalletRouting()).isEqualTo("AGENT_TO_OWNER");
        assertThat(response.getWalletPolicyReason()).isEqualTo("owner has not enabled agent wallet");
        assertThat(response.getDirectRecipientSubjectId()).isEqualTo(101L);
        assertThat(response.getDirectRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementSubjectId()).isEqualTo(9L);
        assertThat(response.getSettlementSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getSettlementHumanId()).isEqualTo(9L);
        assertThat(response.getFinancialAutonomy()).isFalse();
        assertThat(response.getEarnings()).isEqualTo(32L);
        verify(capabilityQueryApi).getCapabilities(agentRef);
    }

    @Test
    void listMyAgentsExposesDirectWalletPolicySurface() {
        AgentProfile agent = agent(102L, 9L, "[\"profile-only\"]");
        SubjectRef agentRef = SubjectRef.agent(102L);

        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(agentRef)).thenReturn(subject(102L, SubjectType.AGENT, "Orion", 690));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L))).thenReturn(subject(9L, SubjectType.HUMAN, "Ava", 700));
        when(walletPolicyApi.resolveWallet(agentRef)).thenReturn(WalletCapability.agentDirect(102L));
        when(liabilityPolicyApi.resolveLiability(agentRef)).thenReturn(LiabilityChain.agentToHuman(102L, 9L));
        when(capabilityQueryApi.getCapabilities(agentRef)).thenReturn(new CapabilitySet(List.of(
                Capability.enabled(CapabilityCode.WALLET),
                Capability.enabled(CapabilityCode.RECEIVE_PAYMENT)
        )));
        when(agentBindingMapper.findByAgentIdAndOwnerId(102L, 9L)).thenReturn(Optional.of(binding(102L, 9L, true)));
        when(creditEarningsService.positiveChangeTotal(102L, "AGENT")).thenReturn(0L);

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.isWalletEnabled()).isTrue();
        assertThat(response.getWalletPolicyState()).isEqualTo("ENABLED");
        assertThat(response.getWalletRouting()).isEqualTo("AGENT_DIRECT");
        assertThat(response.getDirectRecipientSubjectId()).isEqualTo(102L);
        assertThat(response.getDirectRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementSubjectId()).isEqualTo(102L);
        assertThat(response.getSettlementSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementHumanId()).isNull();
        assertThat(response.getFinancialAutonomy()).isTrue();
        assertThat(response.isLiabilityAccepted()).isTrue();
        assertThat(response.getBindingLiabilityAccepted()).isTrue();
        assertThat(response.getCapabilities()).containsExactly("WALLET", "RECEIVE_PAYMENT");
    }

    @Test
    void unresolvedLiabilityIsVisibleAndDoesNotClaimAccepted() {
        AgentProfile agent = agent(103L, 9L, "{\"capabilities\":[\"legacy-fallback\"]}");
        SubjectRef agentRef = SubjectRef.agent(103L);

        when(agentProfileMapper.findActiveByOwnerId(9L)).thenReturn(List.of(agent));
        when(subjectDirectoryApi.getSubject(agentRef)).thenReturn(subject(103L, SubjectType.AGENT, "Echo", 401));
        when(subjectDirectoryApi.getSubject(SubjectRef.human(9L))).thenReturn(subject(9L, SubjectType.HUMAN, "Ava", 700));
        when(walletPolicyApi.resolveWallet(agentRef))
                .thenReturn(WalletCapability.unavailable(agentRef, "owner liability is not accepted"));
        when(liabilityPolicyApi.resolveLiability(agentRef))
                .thenReturn(LiabilityChain.unresolved(agentRef, "owner liability is not accepted"));
        when(capabilityQueryApi.getCapabilities(agentRef)).thenReturn(new CapabilitySet(List.of()));
        when(agentBindingMapper.findByAgentIdAndOwnerId(103L, 9L)).thenReturn(Optional.of(binding(103L, 9L, true)));
        when(creditEarningsService.positiveChangeTotal(103L, "AGENT")).thenReturn(0L);

        AgentMeResponse response = controller.listMyAgents().getData().getFirst();

        assertThat(response.isLiabilityAccepted()).isFalse();
        assertThat(response.getBindingLiabilityAccepted()).isTrue();
        assertThat(response.getLiableHumanId()).isNull();
        assertThat(response.getLiabilityRoute()).isEqualTo("agent:103->human:unknown");
        assertThat(response.getLiabilityReason()).isEqualTo("owner liability is not accepted");
        assertThat(response.getWalletPolicyState()).isEqualTo("DISABLED");
        assertThat(response.getWalletRouting()).isEqualTo("NONE");
        assertThat(response.getCapabilities()).containsExactly("legacy-fallback");
        assertThat(List.of(
                response.getAgentSubjectType().name(),
                response.getOwnerSubjectType().name()
        )).doesNotContain("USER");
    }

    private static AgentProfile agent(Long id, Long ownerId, String capabilityTags) {
        return AgentProfile.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Agent " + id)
                .description("Agent description")
                .agentType(AgentProfile.AgentType.GENERAL)
                .status(AgentProfile.AgentStatus.ACTIVE)
                .permissionLevel("standard")
                .creditScore(50)
                .capabilityTags(capabilityTags)
                .build();
    }

    private static AgentBinding binding(Long agentId, Long ownerId, boolean liabilityAccepted) {
        return AgentBinding.builder()
                .agentId(agentId)
                .ownerId(ownerId)
                .bindingType(AgentBinding.BindingType.OWNER)
                .bindingStatus(AgentBinding.BindingStatus.ACTIVE)
                .liabilityAccepted(liabilityAccepted)
                .build();
    }

    private static SubjectSummaryResponse subject(Long id, SubjectType type, String displayName, Integer creditScore) {
        return SubjectSummaryResponse.builder()
                .id(id)
                .type(type)
                .displayName(displayName)
                .status(SubjectStatus.ACTIVE)
                .creditScore(creditScore)
                .build();
    }

}
