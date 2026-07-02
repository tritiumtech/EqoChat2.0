# Sprint 13B Implementation Notes: WebSocket Subject Presence

Sprint 13B moves WebSocket presence and conversation online state from principal-human-only semantics to subject-aware semantics.

## Scope

- Added `WebSocketSessionManager.isSubjectOnline(subjectId, subjectType)` using the existing subject session index.
- Updated `ChatWebSocketHandler` presence broadcasts to emit one presence event per subscribed subject on a connection.
- Presence payloads and base-message sender identity now carry the canonical subject id/type, including `AGENT`.
- Presence fanout is scoped to online subject sessions in shared conversations, avoiding global principal-human presence broadcasts.
- Connection close captures the closing session's subject keys before unregistering, then broadcasts `OFFLINE` only for subjects with no remaining open subject session.
- Updated single-conversation summaries to compute `online` through subject sessions for both `HUMAN` and `AGENT` targets.
- Kept `principalHumanId` as the authentication/session owner and preserved principal-human session APIs for login, replacement, and kickout behavior.
- Added static guard rules blocking:
  - hard-coded HUMAN subject payloads in Chat presence,
  - human-only `isPrincipalHumanOnline(target.id())` summary lookups.

## Boundary Decision

WebSocket authentication remains principal-human based. Presence is now a subject runtime concern: a human-owned socket can advertise the human subject and associated agent subjects that are subscribed on that socket.

This does not create independent Agent login/JWT sessions. Agent presence currently means an owned/associated agent is reachable through an open principal-human socket.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat,eqochat-framework/eqochat-framework-websocket -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local --server.port=18080`
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
