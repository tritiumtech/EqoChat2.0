# Sprint 13K Implementation Notes

Sprint 13K consolidates wallet settlement actor facts in the Actor API model.

## Changes

- Added `WalletCapability.settlementSubject()` as the canonical settlement subject accessor.
- Updated Agent wallet responses to consume `wallet.settlementSubject()` instead of locally rebuilding a HUMAN settlement subject from `settlementHumanId`.
- Updated Project payment creation to persist settlement subject facts from `WalletCapability.settlementSubject()`.
- Kept `settlementHumanId` as a compatibility/audit field for human-routed settlement while preserving canonical `settlementSubjectId/settlementSubjectType`.
- Added model-level tests for human wallet, agent-direct wallet, and agent-to-owner wallet settlement subject semantics.
- Extended the actor static guard to block caller-side `settlementSubject(WalletCapability, ...)` helper regressions.

## Demo Fallback Decision

Legacy `source_config` wallet fallback remains disabled by default and is only reachable through the explicit `eqochat.actor.demo-fallback.enabled` gate. Production wallet authority is `agent_wallet_state + milestone/points + liability`; demo fallback is not a production actor-model rule.

## Verification

- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor,eqochat-business/eqochat-agent-parent/eqochat-agent,eqochat-business/eqochat-project-parent/eqochat-project -am '-Dtest=ActorCoreHardeningTest,AgentControllerActorPolicyTest,AgentControllerPolicySurfaceTest,ProjectServiceImplActorContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am '-Dtest=ProjectControllerViewerSubjectTest,ProjectServiceImplActorContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## Contract Note

Wallet policy callers must not reconstruct settlement subjects from `settlementHumanId`. The settlement subject is part of the wallet capability returned by Actor policy. Project payment rows freeze the returned direct-recipient, settlement-subject, wallet-state, and liability facts at creation time.
