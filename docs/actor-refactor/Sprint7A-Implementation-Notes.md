# Sprint 7A Implementation Notes: Agent Management Policy Surface

Date: 2026-07-01

## Scope

Sprint 7A refactors `/api/v1/agents/me` from controller-assembled Agent facts into the Actor Model policy surface.

- Agent identity is exposed as `agentSubjectId + agentSubjectType`.
- Owner identity is exposed as `ownerSubjectId + ownerSubjectType`.
- Liability facts come from `LiabilityPolicyApi`.
- Wallet routing facts come from `WalletPolicyApi`.
- Behavior capability facts come from `CapabilityQueryApi`.
- Display facts continue to come from `SubjectDirectoryApi`.
- Legacy frontend fields remain as compatibility aliases while canonical fields are introduced.

## Non-Scope

- No new Agent wallet enable/disable workflow.
- No new `subject_registry`.
- No Project, World, Chat, Contact, Credit, or Notification storage migration.
- No Agent switcher UI.
- No compatibility alias for `USER`.
- No change to Sprint 6C World read/engagement behavior.

## Key Changes

- `AgentMeResponse` now returns canonical policy fields:
  - `agentSubjectId/agentSubjectType`
  - `ownerSubjectId/ownerSubjectType`
  - `liableHumanId`
  - `liabilityRoute/liabilityReason`
  - `walletPolicyState/walletRouting/walletPolicyReason`
  - `directRecipientSubjectId/directRecipientSubjectType`
  - `settlementSubjectId/settlementSubjectType`
  - `settlementHumanId`
  - `financialAutonomy`
- Existing compatibility fields remain:
  - `ownerId`
  - `ownerType`
  - `walletEnabled`
  - `responsibilityChain`
  - `liabilityAccepted`
  - `capabilities`
- `responsibilityChain` is now an alias of `liabilityRoute`.
- `walletEnabled` is now derived from `WalletCapability.state == ENABLED`.
- `liabilityAccepted` is now policy-derived: it is true only when `LiabilityPolicyApi.resolveLiability(agentRef)` resolves a `liableHumanId`.
- The raw binding flag is exposed separately as `bindingLiabilityAccepted`.
- `capabilities` now prefers policy-derived enabled or pending capability codes from `CapabilityQueryApi`.
- Raw `agent_profile.capability_tags` are exposed separately as `profileCapabilities` and only used as a fallback when policy returns no capability set.
- `capabilityPolicy` exposes capability code, state, and reason for UI or audit surfaces.
- Frontend `MyAgentItem` now includes the canonical Agent policy fields.
- The Python baseline smoke now asserts `/agents/me` canonical policy fields in addition to legacy compatibility fields.

## Tests

Added `AgentControllerPolicySurfaceTest` covering:

- owner-routed Agent wallet surface;
- direct Agent wallet surface;
- unresolved liability with explicit reason;
- policy capability source taking precedence over `capability_tags`;
- no emitted `USER` subject type.

## Verification Checklist

- PASS: `cd backend && mvn -pl eqochat-business/eqochat-agent-parent/eqochat-agent -am '-Dtest=AgentControllerPolicySurfaceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
  - Result: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
- PASS: `cd backend && mvn -pl eqochat-business/eqochat-agent-parent/eqochat-agent -am test`
  - Result: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` in the Agent implementation module.
- PASS: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`
  - First attempt failed at Spring Boot repackage because the old backend process held `eqochat-server-1.0.0.jar`.
  - After stopping the old Java process on port `8080`, the package build passed.
- PASS: `cd frontend && npm run build:h5`.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`.
- PASS: `bash scripts/actor-static-guard.sh`.
  - Result: `P0=0 WARN=67`, files scanned `1023`.
- PASS: `git diff --check`.
  - Output only showed Git CRLF normalization warnings; no whitespace errors were reported.
- PASS: latest backend jar started with `--spring.profiles.active=local`.
  - Flyway validated `20` migrations and reported schema version `20` up to date.
- PASS: `bash scripts/smoke/actor-baseline-smoke.sh`.
  - Runtime smoke included `agents canonical policy`.
- PASS: `bash scripts/smoke-actor-baseline.sh`.
  - Compatibility entry point also included `agents canonical policy`.

## Known Residuals

- `/agents/me` remains a human-session endpoint listing the authenticated human's owned agents. It does not yet accept an arbitrary owner subject.
- `capability_tags` still exists on `agent_profile` as profile metadata, but is no longer authoritative for capability decisions on this endpoint.
- Agent wallet enable/disable remains governed by existing `agent_wallet_state` and policy services; this sprint only exposes policy facts.
- Agent management UI still renders legacy fields, but the frontend API type now has the canonical fields needed for a later UI migration.
