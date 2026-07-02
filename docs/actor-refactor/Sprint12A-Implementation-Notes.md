# Sprint 12A Implementation Notes: Project Module Boundary Closure

Sprint 12A removes a compile-time escape hatch from the Project module.

## Scope

- Removed `eqochat-project` dependencies on `eqochat-user` and `eqochat-agent` implementation modules.
- Kept `eqochat-user-api` because Project still accepts an authenticated principal human context from the existing user/auth layer.
- Added a static guard P0 rule blocking Project runtime code or POM from depending on user/agent implementation mappers, entities, service implementations, or implementation artifacts.

## Rationale

`ProjectServiceImpl` already uses Actor APIs for subject identity, wallet routing, liability, and display lookup:

- `SubjectDirectoryApi`
- `WalletPolicyApi`
- `LiabilityPolicyApi`

The lingering implementation dependencies were therefore not needed for runtime behavior. Leaving them in the POM made it too easy for future Project changes to reintroduce direct `user_profile` / `agent_profile` coupling.

## Verification

Required gates:

- `bash scripts/tests/actor-static-guard-test.sh`
- `bash scripts/actor-static-guard.sh backend frontend`
- `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test`
- broader backend/package/smoke if runtime behavior changes later in the sprint
