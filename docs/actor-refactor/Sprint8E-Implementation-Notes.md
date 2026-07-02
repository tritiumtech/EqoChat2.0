# Sprint 8E Implementation Notes - Project Write Actor Contract

## Scope

Sprint 8E closes the Project write actor contract.

Project authentication still uses the principal-human JWT. Project write authorization now requires an explicit business actor subject in the request body, and Project owner-only writes require that actor to exactly match the current project owner subject.

## Changes

- `backend/eqochat-business/eqochat-project-parent/eqochat-project-api`
  - `UpdateProjectBidRequest`, `TransferProjectOwnershipRequest`, `CreateProjectTaskRequest`, and `CreateProjectPaymentRequest` now require `actorSubjectId/actorSubjectType`.
- `backend/eqochat-business/eqochat-project-parent/eqochat-project`
  - Replaced optional actor resolution on Project writes with `requireActorSubject(...)`.
  - Removed the owner-only fallback that allowed principal-human liability to stand in for the project owner actor.
  - Bid update and ownership transfer require `actor == ownerRef(project)`.
  - Task and payment creation require an authorized actor that can access the project; assignee/recipient membership remains checked with `subject_id + subject_type`.
  - Added tests for missing actor rejection and for blocking a principal HUMAN actor from mutating an Agent-owned project.
- `frontend/src/pages/project/project.vue`
  - Project write requests include active-subject actor params.
  - Owner-only UI actions are gated by exact active subject owner match.
  - Project detail/sidebar loading now records the active subject key and ignores stale request results after subject switches.
  - Same-page active subject changes clear old Project detail/sidebar state and reload with the new viewer.
- `scripts/smoke/actor-baseline-smoke.py`
  - Adds Agent viewer `share-link` and sidebar `tasks/payments/files` coverage.
  - Adds Agent owner bid update with explicit Agent actor.
  - Adds negative coverage proving the principal HUMAN actor cannot bid-update an Agent-owned project.
  - Adds Agent actor task creation and sidebar visibility checks.

## Deferred

- Project ownership transfer and payment creation are covered by unit tests but not yet by runtime smoke.
- Independent Agent login/JWT and WebSocket Agent sessions remain deferred.
- Wallet enable/disable product flow is the next P0 sprint after this Project write contract.

## Verification

- `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test` passed.
- `npm run build:h5` passed.
- `mvn -pl eqochat-server -am '-DskipTests' package` passed.
- `python -m py_compile scripts/smoke/actor-baseline-smoke.py` passed.
