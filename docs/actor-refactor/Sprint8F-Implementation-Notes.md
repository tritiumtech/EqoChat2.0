# Sprint 8F Implementation Notes - Agent Wallet Enable/Disable

## Scope

Sprint 8F adds the product write flow for owner-controlled Agent wallet state.

Authentication still uses the principal-human JWT. Wallet writes are owner-only policy operations: the current human principal must be the Agent's liable owner, and the persisted `agent_wallet_state` row is the runtime source of truth for enabled/disabled wallet routing.

## Changes

- `backend/eqochat-business/eqochat-actor-parent`
  - `WalletPolicyApi` now exposes `enableAgentWallet(...)` and `disableAgentWallet(...)`.
  - `WalletPolicyServiceImpl` enforces owner-only wallet writes through Agent liability validation.
  - Enable requires at least 500 Agent behavior points.
  - Disable is allowed for the owner even when the Agent is below the 500-point enable threshold.
  - `ActorDataAccess` upserts `agent_wallet_state` with audit fields and status reason.
- `backend/eqochat-business/eqochat-agent-parent`
  - Added `AgentWalletUpdateRequest`.
  - Added `POST /api/v1/agents/{agentId}/wallet/enable`.
  - Added `POST /api/v1/agents/{agentId}/wallet/disable`.
  - Wallet write endpoints return canonical wallet facts via `AgentMeResponse.WalletPolicyResponse`.
- `backend/eqochat-server/src/main/resources`
  - Added i18n message keys for Agent wallet policy failures.
- `frontend/src/pages/profile/profile.vue`
  - Adds My Agents wallet enable/disable controls.
  - Refreshes the active subject and agent list after wallet state changes.
- `scripts/smoke/actor-baseline-smoke.py`
  - Disables an enabled owned Agent wallet and verifies `AGENT_TO_OWNER`.
  - Enables the same Agent wallet again and verifies `AGENT_DIRECT`.
  - Re-reads `/api/v1/agents/me` after each write to confirm the read surface matches policy state.

## Deferred

- Project ownership transfer and payment creation runtime smoke remain Sprint 8G.
- Subject-specific WebSocket/push delivery remains Sprint 8H.
- Independent Agent login/JWT remains a later auth-boundary decision.

## Verification

- `mvn -pl eqochat-business/eqochat-agent-parent/eqochat-agent -am test` passed during implementation.
