# Sprint 8D Implementation Notes - Project Active Subject UI

## Scope

Sprint 8D extends active subject context into Project list/detail/sidebar/share/create flows.

Project authentication still uses the principal-human JWT. Project business visibility now supports an explicit `viewerSubjectId/viewerSubjectType`, and project creation uses the selected active subject as owner.

## Changes

- `frontend/src/store/modules/activeSubject.ts`
  - Added `projectViewerParams()` and `projectOwnerParams()`.
- `frontend/src/api/modules/project.ts`
  - Added `ProjectViewerParams`.
  - Added optional viewer params to Project list/detail/share/sidebar endpoints.
- `frontend/src/pages/project/project.vue`
  - Loads active subject before Project reads.
  - Lists projects through active subject viewer params.
  - Loads detail/sidebar/share through active subject viewer params.
  - Creates projects with active subject owner params.
  - Treats the current active subject as owner only when `ownerSubjectId/ownerSubjectType` exactly match.
- `backend/eqochat-business/eqochat-project-parent`
  - Added Project service/controller overloads for explicit viewer subject.
  - Agent viewer is authorized through `LiabilityPolicyApi`.
  - Explicit viewer list uses exact subject identity and does not fall back to the principal human's aggregate project set.
  - Added tests for Agent viewer list, numeric-id collision isolation, and unauthorized Agent viewer rejection.
- `scripts/smoke/actor-baseline-smoke.py`
  - Added Agent-owned project creation.
  - Added Agent viewer list/detail checks.
  - Added Human viewer isolation check for the newly created Agent-owned project.

## Deferred

- Project write operations other than create still express authority as principal-human plus backend owner/liability checks.
- Explicit `actorSubjectId/actorSubjectType` for bid updates, ownership transfer, task creation, and payment creation is deferred to a later Project write-contract sprint.
- Independent Agent login/JWT and WebSocket Agent sessions remain deferred.

## Verification

- `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test` passed.
