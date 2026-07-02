# Sprint 12E Implementation Notes: WebSocket Subject Runtime Boundary

Sprint 12E makes WebSocket runtime delivery subject-aware while keeping the authenticated connection principal as a human.

## Scope

- Added multi-subject session tracking in `WebSocketSessionManager`.
- Split subject delivery registration from active subject switching:
  - `registerSubjectSession` adds a subject to the socket delivery index.
  - `registerActiveSubjectSession` still changes the current active subject for explicit `SUBJECT_SUBSCRIBE`.
- Updated `ChatWebSocketHandler` connection establishment to load `SubjectDirectoryApi.listAssociatedSubjects(principalHumanId)`.
- Auto-registered the connected socket for the principal human and owned agent subjects.
- Auto-joined existing conversation subject indexes for every associated subject.
- Kept principal-human presence and authentication semantics unchanged.
- Updated `WebSocketSender.broadcastToConversationSubjects` to send at most one message per underlying socket when multiple subjects on the same socket are in the same conversation.
- Hardened reconnect semantics so a stale replaced socket closing cannot unregister the current socket for the same principal human.
- Cleared `UserContext` after connection setup and after every WebSocket message dispatch so pooled WebSocket threads do not leak principal context.
- Added actor contract tests for:
  - associated HUMAN + AGENT registration during connection establishment,
  - fallback to HUMAN when the subject directory has no associated subjects,
  - active subject switching retaining the principal HUMAN delivery subscription,
  - broadcast de-duplication for one socket subscribed to multiple conversation subjects.
  - stale replaced socket close behavior,
  - per-message `UserContext` cleanup.

## Boundary Decision

`principalHumanId` remains the authentication and liability owner for the socket. It is no longer the only runtime delivery identity.

On connect, the socket is subscribed for all associated subjects so agent inboxes and conversation fanout work without waiting for a manual frontend subject switch. The active subject remains `HUMAN:{principalHumanId}` until the client sends `SUBJECT_SUBSCRIBE`; this avoids silently switching the active actor just because an owned agent exists.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat,eqochat-framework/eqochat-framework-websocket -am test`

## Remaining Work

- Presence is still principal-human scoped. A later sprint can add explicit active-subject presence if product behavior needs agent online status.
- Contact group tables still have human-shaped ownership and membership fields.
- Credit audit actor fields still need subject-aware operator/reporter/reviewer semantics.
- Actor registry read-through fallback remains a deliberate migration bridge, not the final registry-only runtime.
