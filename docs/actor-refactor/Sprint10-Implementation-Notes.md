# Sprint 10 Implementation Notes: Actor Model Closure

## Scope

Sprint 10 closes the remaining runtime gaps found after Sprint 9B/9C:

- Auth user info must use actor canonical points and credit facts.
- Registry sync and soft-delete filters need explicit contracts.
- Static guard must distinguish runtime regressions from historical migration text.
- Agent must not import Credit implementation mappers/entities.

## Backend

- Updated `AuthServiceImpl`.
  - `/auth/me`, login, refresh, and register responses now resolve `HUMAN:{id}` via `SubjectDirectoryApi`.
  - `creditScore` uses actor canonical credit when available and adapts legacy fallback to `300..850`.
  - `points` no longer falls back to `user_profile.credit_score` or `system_config demo.user.points.*`; missing actor points return `0`.

- Added `SubjectRegistrySyncApi` and implementation.
  - Human source writes best-effort sync/retire registry rows.
  - Agent list reads force-refresh the registry for visible active Agents.
  - Current Agent module has no create/update/delete profile write endpoint; future Agent write paths must call `syncAgent` or `retireAgent` at the write boundary.

- Hardened active profile SQL.
  - Added contract tests for User and Agent mapper `del_token = '0'` filters.

- Moved Agent earnings behind Credit API.
  - Added `CreditEarningsService` to `eqochat-credit-api`.
  - Added `CreditEarningsServiceImpl` in the Credit module.
  - Removed Agent module dependency on `eqochat-credit` implementation and direct `CreditRecordMapper`/`CreditRecord` imports.

- Removed unused World SQL `author_ai` projection.
  - World response still exposes `author.ai`, computed from canonical `SubjectType.AGENT`.

## Static Guard

- `scripts/actor-static-guard.sh` now reports:
  - `P0`
  - `WARN_RUNTIME`
  - `WARN_HISTORY`
  - total `WARN`
- Historical docs and Flyway migration text are classified as `WARN_HISTORY`.
- Current result after Sprint 10:
  - `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`

## Tests

Added:

- `AuthServiceImplActorContractTest`
- `UserProfileMapperActorContractTest`
- `AgentProfileMapperActorContractTest`
- `CreditEarningsServiceImplTest`

Updated:

- `AgentControllerActorPolicyTest`
- `AgentControllerPolicySurfaceTest`

## Verification

- PASS: Auth/registry/mapper/policy targeted test suites.
- PASS: Agent/Credit/World targeted test suites.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`

## Remaining Accepted Boundaries

- `user_profile` and `agent_profile` remain source ownership tables.
- `SubjectDirectoryServiceImpl` still falls back to source tables on registry misses to support partially migrated environments.
- `SubjectProfileServiceImpl.search` still has source-profile fallback after registry search.
- World read SQL still has source-table fallback joins after `subject_registry`; runtime identity is still rendered through `SubjectDirectoryApi`.
- `/api/v1/users` remains a human-only compatibility surface; frontend subject flows use `/api/v1/subjects`.
