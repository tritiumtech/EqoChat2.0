# Sprint 13J Implementation Notes

Sprint 13J closes Project write API vocabulary around principal and actor identity.

## Changes

- Renamed Project write service parameters from `viewerId` to `principalHumanId`.
- Kept Project read APIs as `principalHumanId + viewerSubjectId/viewerSubjectType`.
- Split write-path access checking into `ensureActorCanAccessProject(...)` so task/payment/bid/transfer writes no longer call a viewer-named helper with an actor subject.
- Kept audit columns such as `createBy` and `updateBy` as principal-human audit facts, not business owner/viewer identity.
- Made frontend Project write payload actor fields required so TypeScript callers cannot omit `actorSubjectId/actorSubjectType`.
- Added controller-level dispatch tests proving Project writes pass the logged-in human as `principalHumanId` while preserving explicit owner/actor fields from the request body.
- Extended the actor static guard to block Project write service regressions that reintroduce `Long viewerId` as the authenticated principal parameter.

## Verification

- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am '-Dtest=ProjectServiceImplActorContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am '-Dtest=ProjectControllerViewerSubjectTest,ProjectServiceImplActorContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `npm run build:h5`

## Contract Note

Project writes still authenticate through the human principal, but the business actor is explicit in request bodies for bid updates, ownership transfer, task creation, and payment creation. Project creation uses explicit owner subject fields. The principal-human id is only the authenticated liability/audit anchor.
