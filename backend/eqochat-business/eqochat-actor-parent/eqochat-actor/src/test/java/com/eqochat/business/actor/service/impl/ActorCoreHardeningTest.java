package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.CapabilityCode;
import com.eqochat.business.actor.api.model.CapabilityState;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.MilestoneBenefit;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.MilestonePolicyApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.framework.common.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActorCoreHardeningTest {

    @Mock
    ActorSourceRepository actorSourceRepository;

    ActorSubjectValidator validator;
    FakeActorDataAccess dataAccess;

    @BeforeEach
    void setUp() {
        validator = new ActorSubjectValidator(actorSourceRepository);
        dataAccess = new FakeActorDataAccess();
    }

    @Test
    void subjectRefRequiresCanonicalTypeAndRejectsLegacyUser() {
        assertThat(SubjectType.from("HUMAN")).isEqualTo(SubjectType.HUMAN);

        assertThatThrownBy(() -> SubjectType.from("USER")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubjectRef(7L, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void creditAdapterClampsLegacyScores() {
        assertThat(dataAccess.adaptCreditScore(null)).isEqualTo(300);
        assertThat(dataAccess.adaptCreditScore(-1)).isEqualTo(300);
        assertThat(dataAccess.adaptCreditScore(0)).isEqualTo(300);
        assertThat(dataAccess.adaptCreditScore(50)).isEqualTo(575);
        assertThat(dataAccess.adaptCreditScore(100)).isEqualTo(850);
        assertThat(dataAccess.adaptCreditScore(900)).isEqualTo(850);
    }

    @Test
    void tableMissingFalseIsNotCachedPermanently() {
        ScriptedJdbcTemplate jdbcTemplate = new ScriptedJdbcTemplate();
        jdbcTemplate.tableExists("subject_point_ledger", 0, 1);
        jdbcTemplate.ledgerPoints = 425;
        ActorDataAccess access = new ActorDataAccess(jdbcTemplate, new ObjectMapper(), false);

        assertThat(access.currentPoints(SubjectRef.human(7L), 0)).isZero();
        assertThat(access.currentPoints(SubjectRef.human(7L), 0)).isEqualTo(425);
        assertThat(jdbcTemplate.tableChecks).isEqualTo(2);
        assertThat(jdbcTemplate.ledgerLookups).isEqualTo(1);
    }

    @Test
    void demoPointsFallbackRequiresExplicitFlag() {
        ScriptedJdbcTemplate disabledJdbcTemplate = new ScriptedJdbcTemplate();
        disabledJdbcTemplate.tableExists("subject_point_ledger", 1);
        disabledJdbcTemplate.ledgerThrows = true;
        disabledJdbcTemplate.demoPoints = 777;

        ActorDataAccess disabled = new ActorDataAccess(disabledJdbcTemplate, new ObjectMapper(), false);

        assertThat(disabled.currentPoints(SubjectRef.human(7L), 0)).isZero();
        assertThat(disabledJdbcTemplate.systemConfigLookups).isZero();

        ScriptedJdbcTemplate enabledJdbcTemplate = new ScriptedJdbcTemplate();
        enabledJdbcTemplate.tableExists("subject_point_ledger", 1);
        enabledJdbcTemplate.ledgerThrows = true;
        enabledJdbcTemplate.demoPoints = 777;

        ActorDataAccess enabled = new ActorDataAccess(enabledJdbcTemplate, new ObjectMapper(), true);

        assertThat(enabled.currentPoints(SubjectRef.human(7L), 0)).isEqualTo(777);
        assertThat(enabledJdbcTemplate.systemConfigLookups).isEqualTo(1);
    }

    @Test
    void liabilityReturnsAgentToHumanOnlyForActiveAcceptedOwnerBinding() {
        givenValidAgent(101L, 2L);
        LiabilityPolicyServiceImpl policy = new LiabilityPolicyServiceImpl(validator);

        LiabilityChain chain = policy.resolveLiability(SubjectRef.agent(101L));

        assertThat(chain.liableHumanId()).isEqualTo(2L);
        assertThat(chain.reason()).isNull();
    }

    @Test
    void liabilityReturnsUnresolvedReasonWhenAgentOrOwnerBindingIsInvalid() {
        LiabilityPolicyServiceImpl policy = new LiabilityPolicyServiceImpl(validator);
        when(actorSourceRepository.findAgent(101L)).thenReturn(Optional.of(agent(101L, 2L, ActorSourceRepository.AgentStatus.SUSPENDED)));

        assertThat(policy.resolveLiability(SubjectRef.agent(101L)).reason()).isEqualTo("agent is not active");

        when(actorSourceRepository.findAgent(102L)).thenReturn(Optional.of(agent(102L, 2L, ActorSourceRepository.AgentStatus.ACTIVE)));
        when(actorSourceRepository.findHuman(2L)).thenReturn(Optional.of(user(2L, ActorSourceRepository.HumanStatus.BANNED)));
        assertThat(policy.resolveLiability(SubjectRef.agent(102L)).reason()).isEqualTo("owner human is not active");

        when(actorSourceRepository.findAgent(103L)).thenReturn(Optional.of(agent(103L, 2L, ActorSourceRepository.AgentStatus.ACTIVE)));
        when(actorSourceRepository.findHuman(2L)).thenReturn(Optional.of(user(2L, ActorSourceRepository.HumanStatus.ACTIVE)));
        when(actorSourceRepository.findOwnerBinding(103L, 2L))
                .thenReturn(Optional.of(binding(103L, 2L, ActorSourceRepository.BindingType.OPERATOR, ActorSourceRepository.BindingStatus.ACTIVE, true)));
        assertThat(policy.resolveLiability(SubjectRef.agent(103L)).reason()).isEqualTo("owner binding type is not OWNER");

        when(actorSourceRepository.findAgent(104L)).thenReturn(Optional.of(agent(104L, 2L, ActorSourceRepository.AgentStatus.ACTIVE)));
        when(actorSourceRepository.findOwnerBinding(104L, 2L))
                .thenReturn(Optional.of(binding(104L, 2L, ActorSourceRepository.BindingType.OWNER, ActorSourceRepository.BindingStatus.ACTIVE, false)));
        assertThat(policy.resolveLiability(SubjectRef.agent(104L)).reason()).isEqualTo("owner liability is not accepted");
    }

    @Test
    void walletRejectsNullAndSystemWithoutAgentToOwnerRoute() {
        WalletPolicyServiceImpl policy = new WalletPolicyServiceImpl(validator, dataAccess);

        assertThat(policy.resolveWallet(null).routing()).isEqualTo("NONE");
        assertThat(policy.resolveWallet(SubjectRef.system(0L)).reason()).isEqualTo("system subject has no wallet");
    }

    @Test
    void walletCapabilityExposesCanonicalSettlementSubject() {
        assertThat(WalletCapability.humanEnabled(2L).settlementSubject()).isEqualTo(SubjectRef.human(2L));
        assertThat(WalletCapability.agentToOwner(101L, 2L, "owner wallet").settlementSubject())
                .isEqualTo(SubjectRef.human(2L));
        assertThat(WalletCapability.agentDirect(101L).settlementSubject()).isEqualTo(SubjectRef.agent(101L));
    }

    @Test
    void walletUsesHardenedAgentChecksAndWalletStateBeforeLegacySourceConfig() {
        ActorSourceRepository.Agent agent = agent(101L, 2L, ActorSourceRepository.AgentStatus.ACTIVE, "{\"wallet\":\"enabled\"}");
        when(actorSourceRepository.findAgent(101L)).thenReturn(Optional.of(agent));
        when(actorSourceRepository.findHuman(2L)).thenReturn(Optional.of(user(2L, ActorSourceRepository.HumanStatus.ACTIVE)));
        when(actorSourceRepository.findOwnerBinding(101L, 2L))
                .thenReturn(Optional.of(binding(101L, 2L, ActorSourceRepository.BindingType.OWNER, ActorSourceRepository.BindingStatus.ACTIVE, true)));
        dataAccess.points.put(SubjectRef.agent(101L), 600);
        dataAccess.walletStates.put(101L, new ActorDataAccess.AgentWalletState(false, 2L, "wallet disabled"));
        dataAccess.sourceConfigWalletEnabled.put(agent.getSourceConfig(), true);

        WalletCapability wallet = new WalletPolicyServiceImpl(validator, dataAccess).resolveWallet(SubjectRef.agent(101L));

        assertThat(wallet.routing()).isEqualTo("AGENT_TO_OWNER");
        assertThat(wallet.reason()).isEqualTo("wallet disabled");
    }

    @Test
    void walletAllowsAgentDirectWhenStateOwnerAndPointsPass() {
        givenValidAgent(101L, 2L);
        dataAccess.points.put(SubjectRef.agent(101L), 500);
        dataAccess.walletStates.put(101L, new ActorDataAccess.AgentWalletState(true, 2L, "enabled"));

        WalletCapability wallet = new WalletPolicyServiceImpl(validator, dataAccess).resolveWallet(SubjectRef.agent(101L));

        assertThat(wallet.routing()).isEqualTo("AGENT_DIRECT");
        assertThat(wallet.state()).isEqualTo(CapabilityState.ENABLED);
    }

    @Test
    void ownerCanEnableAgentWalletAfterPointsThreshold() {
        givenValidAgent(101L, 2L);
        dataAccess.points.put(SubjectRef.agent(101L), 500);

        WalletCapability wallet = new WalletPolicyServiceImpl(validator, dataAccess).enableAgentWallet(2L, 101L);

        assertThat(wallet.routing()).isEqualTo("AGENT_DIRECT");
        assertThat(dataAccess.walletStates.get(101L).walletEnabled()).isTrue();
        assertThat(dataAccess.walletStates.get(101L).enabledBy()).isEqualTo(2L);
    }

    @Test
    void enableAgentWalletRejectsLowPointsAndNonOwnerPrincipal() {
        givenValidAgent(101L, 2L);
        dataAccess.points.put(SubjectRef.agent(101L), 499);
        WalletPolicyServiceImpl policy = new WalletPolicyServiceImpl(validator, dataAccess);

        assertThatThrownBy(() -> policy.enableAgentWallet(2L, 101L))
                .isInstanceOf(BizException.class)
                .hasMessage("agent.wallet.points_insufficient");

        dataAccess.points.put(SubjectRef.agent(101L), 500);
        assertThatThrownBy(() -> policy.enableAgentWallet(9L, 101L))
                .isInstanceOf(BizException.class)
                .hasMessage("agent.wallet.forbidden");
    }

    @Test
    void ownerCanDisableAgentWalletWithoutPointsThreshold() {
        givenValidAgent(101L, 2L);
        dataAccess.points.put(SubjectRef.agent(101L), 0);
        dataAccess.walletStates.put(101L, new ActorDataAccess.AgentWalletState(true, 2L, "enabled"));

        WalletCapability wallet = new WalletPolicyServiceImpl(validator, dataAccess)
                .disableAgentWallet(2L, 101L, "risk review");

        assertThat(wallet.routing()).isEqualTo("AGENT_TO_OWNER");
        assertThat(wallet.reason()).isEqualTo("risk review");
        assertThat(dataAccess.walletStates.get(101L).walletEnabled()).isFalse();
        assertThat(dataAccess.walletStates.get(101L).statusReason()).isEqualTo("risk review");
    }

    @Test
    void disableAgentWalletBoundsLongReasonToStorageLimit() {
        givenValidAgent(101L, 2L);
        String longReason = IntStream.range(0, 220).mapToObj(i -> "x").collect(Collectors.joining());

        WalletCapability wallet = new WalletPolicyServiceImpl(validator, dataAccess)
                .disableAgentWallet(2L, 101L, longReason);

        assertThat(wallet.reason()).hasSize(200);
        assertThat(dataAccess.walletStates.get(101L).statusReason()).hasSize(200);
    }

    @Test
    void sourceConfigWalletFallbackRequiresExplicitFlag() {
        ActorSourceRepository.Agent agent = agent(101L, 2L, ActorSourceRepository.AgentStatus.ACTIVE, "{\"wallet\":\"enabled\"}");
        when(actorSourceRepository.findAgent(101L)).thenReturn(Optional.of(agent));
        when(actorSourceRepository.findHuman(2L)).thenReturn(Optional.of(user(2L, ActorSourceRepository.HumanStatus.ACTIVE)));
        when(actorSourceRepository.findOwnerBinding(101L, 2L))
                .thenReturn(Optional.of(binding(101L, 2L, ActorSourceRepository.BindingType.OWNER, ActorSourceRepository.BindingStatus.ACTIVE, true)));
        dataAccess.points.put(SubjectRef.agent(101L), 600);
        dataAccess.sourceConfigWalletEnabled.put(agent.getSourceConfig(), true);
        WalletPolicyServiceImpl policy = new WalletPolicyServiceImpl(validator, dataAccess);

        WalletCapability disabled = policy.resolveWallet(SubjectRef.agent(101L));

        assertThat(disabled.routing()).isEqualTo("AGENT_TO_OWNER");
        assertThat(disabled.reason()).isEqualTo("owner has not enabled agent wallet");

        dataAccess.demoFallbackEnabled = true;
        WalletCapability enabled = policy.resolveWallet(SubjectRef.agent(101L));

        assertThat(enabled.routing()).isEqualTo("AGENT_DIRECT");
        assertThat(enabled.reason()).isEqualTo("legacy/local source_config wallet enabled fallback");
    }

    @Test
    void milestoneDoesNotGrantOrdinaryBenefitsForMissingInactiveInvalidOrSystemSubjects() {
        MilestonePolicyServiceImpl policy = new MilestonePolicyServiceImpl(validator, dataAccess);
        dataAccess.points.put(SubjectRef.human(1L), 5000);
        dataAccess.points.put(SubjectRef.agent(101L), 2000);
        dataAccess.points.put(SubjectRef.agent(102L), 2000);
        when(actorSourceRepository.findHuman(1L)).thenReturn(Optional.empty());
        when(actorSourceRepository.findHuman(2L)).thenReturn(Optional.of(user(2L, ActorSourceRepository.HumanStatus.ACTIVE)));
        when(actorSourceRepository.findAgent(101L)).thenReturn(Optional.of(agent(101L, 2L, ActorSourceRepository.AgentStatus.SUSPENDED)));
        when(actorSourceRepository.findAgent(102L)).thenReturn(Optional.of(agent(102L, 2L, ActorSourceRepository.AgentStatus.ACTIVE)));
        when(actorSourceRepository.findOwnerBinding(102L, 2L)).thenReturn(Optional.empty());

        assertUnknownNoPrivileges(policy.resolveMilestone(SubjectRef.human(1L)));
        assertUnknownNoPrivileges(policy.resolveMilestone(SubjectRef.agent(101L)));
        assertUnknownNoPrivileges(policy.resolveMilestone(SubjectRef.agent(102L)));
        assertUnknownNoPrivileges(policy.resolveMilestone(SubjectRef.system(0L)));
    }

    @Test
    void capabilityQueryDoesNotEnableMissingInactiveOrSystemSubjects() {
        CapabilityQueryServiceImpl policy = new CapabilityQueryServiceImpl(
                validator,
                mock(WalletPolicyApi.class),
                mock(MilestonePolicyApi.class)
        );
        when(actorSourceRepository.findHuman(1L)).thenReturn(Optional.empty());
        when(actorSourceRepository.findAgent(101L)).thenReturn(Optional.of(agent(101L, 2L, ActorSourceRepository.AgentStatus.SUSPENDED)));

        assertThat(policy.getCapability(SubjectRef.human(1L), CapabilityCode.LOGIN).state()).isEqualTo(CapabilityState.DISABLED);
        assertThat(policy.getCapability(SubjectRef.agent(101L), CapabilityCode.SEND_MESSAGE).reason()).isEqualTo("agent is not active");
        assertThat(policy.getCapability(SubjectRef.system(0L), CapabilityCode.POST_WORLD).state()).isEqualTo(CapabilityState.DISABLED);
    }

    @Test
    void capabilityQueryEnablesOnlyValidSubjects() {
        WalletPolicyApi walletPolicyApi = mock(WalletPolicyApi.class);
        MilestonePolicyApi milestonePolicyApi = mock(MilestonePolicyApi.class);
        CapabilityQueryServiceImpl policy = new CapabilityQueryServiceImpl(validator, walletPolicyApi, milestonePolicyApi);
        givenValidAgent(101L, 2L);
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(101L)))
                .thenReturn(WalletCapability.agentToOwner(101L, 2L, "agent points below 500"));
        when(milestonePolicyApi.resolveMilestone(SubjectRef.agent(101L)))
                .thenReturn(new MilestoneBenefit("Resident", 0, 0L, 100, false, false, false));

        assertThat(policy.getCapabilities(SubjectRef.agent(101L)).enabled(CapabilityCode.SEND_MESSAGE)).isTrue();
    }

    private static void assertUnknownNoPrivileges(MilestoneBenefit benefit) {
        assertThat(benefit.title()).isEqualTo("Unknown");
        assertThat(benefit.maxOwnedAgents()).isZero();
        assertThat(benefit.projectNoDepositLimit()).isZero();
        assertThat(benefit.walletEligible()).isFalse();
        assertThat(benefit.canArbitrate()).isFalse();
        assertThat(benefit.canParticipateRuleGovernance()).isFalse();
    }

    private void givenValidAgent(Long agentId, Long ownerId) {
        when(actorSourceRepository.findAgent(agentId)).thenReturn(Optional.of(agent(agentId, ownerId, ActorSourceRepository.AgentStatus.ACTIVE)));
        when(actorSourceRepository.findHuman(ownerId)).thenReturn(Optional.of(user(ownerId, ActorSourceRepository.HumanStatus.ACTIVE)));
        when(actorSourceRepository.findOwnerBinding(agentId, ownerId))
                .thenReturn(Optional.of(binding(agentId, ownerId, ActorSourceRepository.BindingType.OWNER, ActorSourceRepository.BindingStatus.ACTIVE, true)));
    }

    private static ActorSourceRepository.Human user(Long id, ActorSourceRepository.HumanStatus status) {
        return new ActorSourceRepository.Human(id, null, null, null, null, null, null, status, null);
    }

    private static ActorSourceRepository.Agent agent(Long id, Long ownerId, ActorSourceRepository.AgentStatus status) {
        return agent(id, ownerId, status, null);
    }

    private static ActorSourceRepository.Agent agent(
            Long id,
            Long ownerId,
            ActorSourceRepository.AgentStatus status,
            String sourceConfig
    ) {
        return new ActorSourceRepository.Agent(id, null, ownerId, null, null, null, status, null, null, sourceConfig);
    }

    private static ActorSourceRepository.Binding binding(
            Long agentId,
            Long ownerId,
            ActorSourceRepository.BindingType type,
            ActorSourceRepository.BindingStatus status,
            boolean liabilityAccepted
    ) {
        return new ActorSourceRepository.Binding(agentId, ownerId, type, status, liabilityAccepted);
    }

    static final class FakeActorDataAccess extends ActorDataAccess {
        final Map<SubjectRef, Integer> points = new HashMap<>();
        final Map<Long, AgentWalletState> walletStates = new HashMap<>();
        final Map<String, Boolean> sourceConfigWalletEnabled = new HashMap<>();
        boolean demoFallbackEnabled;

        FakeActorDataAccess() {
            super(new JdbcTemplate(), new ObjectMapper(), false);
        }

        @Override
        int currentPoints(SubjectRef ref, Integer fallback) {
            return points.getOrDefault(ref, fallback != null ? fallback : 0);
        }

        @Override
        AgentWalletState agentWalletState(Long agentId) {
            return walletStates.get(agentId);
        }

        @Override
        boolean upsertAgentWalletState(Long agentId, boolean enabled, Long operatorHumanId, String reason) {
            walletStates.put(agentId, new AgentWalletState(enabled, enabled ? operatorHumanId : null, reason));
            return true;
        }

        @Override
        boolean sourceConfigWalletEnabled(String raw) {
            return demoFallbackEnabled && sourceConfigWalletEnabled.getOrDefault(raw, false);
        }
    }

    static final class ScriptedJdbcTemplate extends JdbcTemplate {
        final Map<String, Deque<Integer>> tableExists = new HashMap<>();
        Integer ledgerPoints;
        Integer demoPoints;
        boolean ledgerThrows;
        int tableChecks;
        int ledgerLookups;
        int systemConfigLookups;

        void tableExists(String tableName, Integer... results) {
            tableExists.put(tableName, new ArrayDeque<>(Arrays.asList(results)));
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
            if (sql.contains("INFORMATION_SCHEMA.TABLES")) {
                tableChecks++;
                String tableName = (String) args[0];
                Deque<Integer> results = tableExists.get(tableName);
                Integer count = results == null || results.isEmpty() ? 0 : results.removeFirst();
                return requiredType.cast(count);
            }
            if (sql.contains("subject_point_ledger")) {
                ledgerLookups++;
                if (ledgerThrows || ledgerPoints == null) {
                    throw new EmptyResultDataAccessException(1);
                }
                return requiredType.cast(ledgerPoints);
            }
            if (sql.contains("system_config")) {
                systemConfigLookups++;
                if (demoPoints == null) {
                    throw new EmptyResultDataAccessException(1);
                }
                return requiredType.cast(demoPoints);
            }
            throw new EmptyResultDataAccessException(1);
        }
    }
}
