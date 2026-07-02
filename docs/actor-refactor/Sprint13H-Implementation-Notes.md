# Sprint 13H Implementation Notes: Realtime And Frontend Active Subject Fallback Cleanup

Sprint 13H removes the remaining frontend, WebSocket, and World read/write paths that silently derived a business subject from the authenticated human principal.

## Scope

- Updated `activeSubjectStore.currentSubject` so it no longer returns the logged-in human as a computed fallback.
- Removed first-login/no-stored-subject automatic `setHuman()` selection; Human remains available only through the Profile subject picker.
- Changed active-subject helper methods to fail fast with `active subject unavailable` instead of returning empty params.
- Updated chat state and WebSocket utilities so realtime send, typing, read receipt, and active-subject subscription require an explicit active subject.
- Changed WebSocket heartbeat to use a `SYSTEM` ping path instead of sending through a business subject fallback.
- Updated `App.vue` realtime startup to await active-subject loading and stop realtime when no active subject is available.
- Made frontend conversation read APIs require explicit viewer params.
- Removed World API token/user-info-derived HUMAN subject fallback; World viewer/actor params now fail fast.
- Updated World page/components to pass the active subject into feed, topics, detail, reply, follow, and upvote calls.
- Updated backend WebSocket session registration so connecting as a principal no longer automatically registers that human as the active subject.
- Updated backend WebSocket connection authorization so an empty associated-subject directory does not synthesize `SubjectRef.human(principalHumanId)`.
- Added static guard rules for WebSocket principal-HUMAN fallback, frontend WebSocket fallback, frontend World token-derived fallback, and frontend active-subject computed fallback.

## Boundary Decision

`principalHumanId` remains the authentication/session owner. Realtime delivery may subscribe a socket to subjects returned by `SubjectDirectoryApi.listAssociatedSubjects(principalHumanId)`, but the active business subject used for reads and writes must come from an explicit active-subject selection or request parameter.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat,eqochat-framework/eqochat-framework-websocket -am '-Dtest=ChatWebSocketHandlerActorContractTest,WebSocketMessageHandlerActorContractTest,WebSocketSessionManagerActorContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `npm run build:h5`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn test`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local --server.port=18080`
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
- Cleanup: stopped the `18080` Java process after smoke.
