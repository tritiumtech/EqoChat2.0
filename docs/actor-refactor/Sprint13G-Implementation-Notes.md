# Sprint 13G Implementation Notes: Subject Profile Explicit Viewer Cleanup

Sprint 13G removes the remaining Subject public-profile/search fallback that used the authenticated human as the viewer for friendship state.

## Scope

- Updated `SubjectProfileApi` search/public-profile contracts to require explicit `viewerSubjectId` and `viewerSubjectType`.
- Updated `SubjectController` search/public-profile endpoints to reject missing or `SYSTEM` viewer subjects with `subject.viewer.invalid`.
- Updated `SubjectProfileServiceImpl` to authorize the explicit viewer through `SubjectDirectoryApi.listAssociatedSubjects(principalHumanId)`.
- `SubjectProfileServiceImpl.isFriend(...)` now calls `SubjectRelationshipApi.areFriends(viewer, target)` with the explicit viewer subject.
- Updated frontend `subjectApi`, contact search, and contact profile pages to pass the active subject as viewer.
- Updated actor baseline smoke subject search/public-profile calls to pass an `AGENT` viewer.
- Added `subject.viewer.invalid` and `subject.viewer.forbidden` message keys.
- Added controller/service tests proving explicit `AGENT` viewer dispatch and missing/unauthorized viewer rejection.
- Added a static guard for `Subject profile implicit principal HUMAN viewer fallback`.

## Boundary Decision

Public profile data is globally addressable, but viewer-dependent fields such as `isFriend` are active-subject-specific. A logged-in human can authorize the viewer, but the service must not collapse viewer semantics back to `SubjectRef.human(principalHumanId)`.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am '-Dtest=SubjectProfileServiceImplActorContractTest,SubjectControllerPublicProfileTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local --server.port=18080`
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
  - Included explicit Subject viewer, Contact owner, Project viewer, World viewer/actor, and Chat viewer/creator smoke coverage.
