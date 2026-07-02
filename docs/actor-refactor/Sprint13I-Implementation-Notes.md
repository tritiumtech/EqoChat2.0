# Sprint 13I Implementation Notes: Subject Directory Principal Representation Contract

Sprint 13I makes `SubjectDirectoryApi.listAssociatedSubjects(principalHumanId)` registry-authoritative for principal-scoped representation authority.

## Scope

- Documented `SubjectDirectoryApi.listAssociatedSubjects(...)` as a principal-scoped representation-authority query.
- Clarified that the principal's own `HUMAN` subject is returned only when present in `subject_registry`; owned or delegated subjects also come from `subject_registry`.
- Removed the local `SubjectRef.human(principalHumanId)` synthesis from `SubjectDirectoryServiceImpl.listAssociatedSubjects(...)`.
- Added contract tests proving registered principal-HUMAN self representation, de-duplication, no local HUMAN synthesis when the registry omits it, and invalid principals returning no associated subjects without querying the registry.

## Boundary Decision

This is not a viewer/actor fallback. It is an authorization query over the subject registry: a logged-in human can represent its registered self subject and registered associated subjects, but every business read/write path still has to pass the selected subject explicitly. Services that need active identity must reject missing viewer/actor/owner params instead of calling `SubjectRef.human(principalHumanId)` locally.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am '-Dtest=SubjectDirectoryServiceImplRegistryContractTest,SubjectProfileServiceImplActorContractTest,SubjectControllerPublicProfileTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn test`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local --server.port=18080`
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
- Cleanup: stopped the `18080` Java process after smoke.
