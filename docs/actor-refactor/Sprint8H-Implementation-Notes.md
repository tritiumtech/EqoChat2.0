# Sprint 8H - Notification and WebSocket Active Subject Delivery

Date: 2026-07-01

## Scope

Sprint 8H moves realtime delivery from principal-human-only fanout toward actor-subject delivery while keeping the current login boundary as `principalHumanId + active subject`.

## Implemented

- Added canonical recipient subject fields to `WebSocketMessage.BaseMessage`.
- Added WebSocket control messages:
  - `SUBJECT_SUBSCRIBE`
  - `SUBJECT_SUBSCRIBED`
- Extended `WebSocketSessionManager` with active-subject session indexes:
  - `SubjectType:subjectId -> sessions`
  - `sessionId -> active subject`
  - `conversationId -> participant subject keys`
- Kept principal-human session APIs for existing compatibility.
- Updated chat realtime fanout:
  - conversation saved-message fanout now targets conversation participant subjects, not only human principal IDs.
  - typing fanout targets participant subjects and stamps recipient subject fields.
  - read receipts target the stored sender subject, including `AGENT` senders.
- Added subject subscription authorization:
  - `HUMAN` subscription must match `principalHumanId`.
  - `AGENT` subscription must resolve liability to the current principal human.
  - `SYSTEM` and legacy `USER` are rejected.
- Added notification realtime producer:
  - `NotificationServiceImpl.sendNotification` pushes `NOTIFICATION` to the recipient subject after insert succeeds.
  - realtime payload reuses the canonical REST response fields.
- Updated frontend WebSocket client:
  - tracks active subject.
  - sends `SUBJECT_SUBSCRIBE` on connect/reconnect and active-subject change.
  - supports canonical actor subject for chat send/typing/read.
- Added frontend active-subject realtime filtering:
  - notification store drops mismatched recipient subject pushes.
  - chat store drops mismatched recipient subject pushes.
  - profile subject switch resubscribes the WebSocket and reloads subject-scoped data.

## Tests and Verification

- `mvn -pl eqochat-framework/eqochat-framework-websocket -am test`
- `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat -am test`
- `mvn -pl eqochat-business/eqochat-notification-parent/eqochat-notification -am test`
- `npm run build:h5`
- `mvn -pl eqochat-server -am '-DskipTests' package`
- `bash scripts/tests/actor-static-guard-test.sh`
- `bash scripts/actor-static-guard.sh` -> `P0=0 WARN=67`
- `bash scripts/smoke/actor-baseline-smoke.sh`
- `bash scripts/smoke-actor-baseline.sh`

## Notes

- This sprint does not introduce independent Agent JWT login. The runtime boundary remains principal human authentication plus active-subject subscription.
- Subject subscription is single-active-subject per WebSocket session. Switching to an Agent moves the session from the previous subject index to the new one.
- Remaining realtime work for a later sprint: dedicated WebSocket smoke around `SUBJECT_SUBSCRIBE`, Agent notification push, and Agent chat fanout with an actual WebSocket client.

## Next

Proceed to Sprint 9A: `subject_registry` read model. The goal is a canonical cross-module subject profile source so World/Contact/Project/Chat can stop depending on Agent mirror user profiles and ad hoc joins.
