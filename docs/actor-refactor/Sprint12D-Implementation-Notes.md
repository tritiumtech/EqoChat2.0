# Sprint 12D Implementation Notes: Actor Implementation Dependency Closure

Sprint 12D removes Actor's compile-time dependency on User and Agent implementation modules.

## Scope

- Added Actor-owned `ActorSourceRepository` for source-profile reads from `user_profile`, `agent_profile`, and `agent_binding`.
- Changed `ActorSubjectValidator` to validate humans, agents, and owner liability through Actor-owned source records.
- Changed `SubjectDirectoryServiceImpl` registry miss refresh to use `ActorSourceRepository` instead of source implementation mappers.
- Changed `SubjectRegistryRepository` upsert methods to accept Actor-owned source records.
- Removed `eqochat-actor` dependencies on `eqochat-user-api`, `eqochat-user`, and `eqochat-agent`.
- Updated Actor tests to mock `ActorSourceRepository` rather than user/agent implementation mappers/entities.
- Added a static guard P0 rule blocking Actor direct human/agent implementation or API coupling.

## Boundary Decision

Actor may still read source tables as an anti-corruption adapter for registry refresh, validation, and local fallback behavior. That adapter is now owned by Actor and uses stable SQL rows, not user/agent implementation Java types.

This keeps registry miss fallback behavior without making Actor compile against implementation modules.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`

## Remaining Work

- Decide whether Actor should eventually be registry-only at runtime, removing source-table fallback entirely after registry sync is authoritative.
- WebSocket session and presence runtime remain principal-human first.
- Contact group tables still have human-shaped ownership and membership fields.
- Credit audit actor fields still need subject-aware operator/reporter/reviewer semantics.
