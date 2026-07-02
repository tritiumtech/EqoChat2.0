# Sprint 11 Implementation Notes: Actor Runtime Closure

Sprint 11 closes the remaining runtime adapter leaks that were still reachable after Sprint 10. The goal was not to rename legacy storage tables, but to ensure cross-module runtime behavior flows through Actor Model APIs and registry-backed subject identity.

## Scope

- Added `SubjectProfileApi` as the public profile and search boundary for subjects.
- Added `SubjectRelationshipApi` so Actor and World query relationship state through Contact API, not `user_friend` storage.
- Extended `SubjectDirectoryApi` with `listAssociatedSubjects(Long principalHumanId)` for principal-scoped subject discovery.
- Converted `/api/v1/users` service implementation into an actor compatibility wrapper.
- Made subject search registry-authoritative.
- Made World author read SQL use `subject_registry` only for author display identity.
- Removed frontend usage of legacy `/api/v1/users/search` and `/api/v1/users/{id}/public` clients.

## Runtime Boundary Changes

`SubjectProfileServiceImpl` now implements `SubjectProfileApi` and owns profile search/public-profile lookups. Search reads from `subject_registry` through `SubjectRegistryRepository.search`; it no longer falls back to `UserProfileMapper.findByDid/findByPhone/findByEmail` or `AgentProfileMapper.findByDid/findByName`.

`UserServiceImpl` now depends on `SubjectProfileApi` instead of user-profile implementation services. Legacy user search validates the historical query type, delegates to subject search, filters to `HUMAN`, and maps back to the old DTO shape. Legacy public profile delegates to `getPublicProfile(HUMAN, userId)`.

`WorldPostMapper` no longer joins `user_profile`, `agent_profile`, or owner `user_profile` for author identity fallback. Feed/detail reads use `subject_registry sr` and `owner_sr`; missing registry display names degrade to stable labels such as `HUMAN:{id}` or `AGENT:{id}`.

`ContactServiceImpl` implements `SubjectRelationshipApi` over existing `user_friend` storage. This keeps storage migration out of Sprint 11 while preventing Actor and World from depending on Contact internals.

`FriendRequestServiceImpl` now uses `SubjectDirectoryApi.listAssociatedSubjects` for default inbox scope. The Contact module no longer reads `AgentProfileMapper.findActiveByOwnerId` directly.

## Static Guard Expansion

`scripts/actor-static-guard.sh` now treats the following as P0:

- frontend legacy `/api/v1/users` client usage
- `UserServiceImpl` direct human-profile implementation dependencies
- `WorldPostMapper` source-profile author fallback joins
- Actor/World direct `UserFriendMapper` or `UserFriend` dependencies
- World implementation dependency on `eqochat-user`
- Contact direct `AgentProfileMapper` / `AgentProfile` / `eqochat-agent` dependency

Expected strict guard summary remains:

```text
P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28
```

## Verification

Verification performed during Sprint 11 implementation:

- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor,eqochat-business/eqochat-user-parent/eqochat-user -am test`
- PASS: `mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am test`
- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor,eqochat-business/eqochat-contact-parent/eqochat-contact,eqochat-business/eqochat-world-parent/eqochat-world -am test`
- PASS: `mvn test` from `backend`
- PASS: `npm run build:h5` from `frontend`

Post-Sprint-11 package and smoke verification should be rerun from the latest jar before declaring the sprint fully closed.

## Remaining Deliberate Compatibility

- `/api/v1/users` still exists as a compatibility route, but its service implementation is actor-backed.
- Contact still stores relationship data in `user_friend`; external modules consume it through `SubjectRelationshipApi`.
- `SubjectDirectoryServiceImpl` still refreshes missing registry rows from source tables. This is a migration/backfill adapter and should be disabled only after registry population is proven reliable by a later sprint.
- Actor module still owns source-table adapters for registry sync and canonical subject validation.
