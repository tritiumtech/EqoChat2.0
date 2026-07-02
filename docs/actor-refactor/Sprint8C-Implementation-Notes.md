# Sprint 8C Implementation Notes - Chat Active Subject UI

## Scope

Sprint 8C extends the Sprint 8B active subject context into the chat UI, WebSocket send path, and Chat HTTP viewer/creator contract.

The WebSocket connection and JWT session remain principal-human based. Chat message, typing, read-receipt, conversation list/detail, message history, and conversation creation calls now carry the current active subject selected in the UI.

## Changes

- `frontend/src/utils/websocket.ts`
  - Added `WebSocketSubjectRef`.
  - Added optional subject passthrough for generic `send`, chat message, typing, and read receipt helpers.
  - Preserved fallback to principal `HUMAN` when no subject is supplied.
- `frontend/src/composables/useWebSocket.ts`
  - Exposed optional subject arguments through `sendMessage`, `sendTyping`, and `sendReadReceipt`.
- `frontend/src/pages/chat/chat-room.vue`
  - Loads `activeSubjectStore` on page show before history rendering.
  - Uses active subject viewer params for conversation detail and message history.
  - Uses active subject for history `isSelf` mapping.
  - Uses active subject for outbound optimistic messages, WebSocket chat sends, HTTP fallback sends, typing events, read receipts, retry sends, and local-message replacement.
  - Ignores typing events from the current active subject instead of only the principal human.
- `frontend/src/store/modules/chat.ts`
  - Tracks current active chat subject for unread self-filtering.
  - Resolves current subject from `activeSubjectStore` when handling realtime messages.
- `backend/eqochat-business/eqochat-chat-parent`
  - Added optional `viewerSubjectId/viewerSubjectType` to list/detail/messages endpoints.
  - Added optional `creatorSubjectId/creatorSubjectType` to conversation creation.
  - Agent viewer/creator subjects are authorized through `LiabilityPolicyApi`.
  - WebSocket typing events now require liability ownership and conversation participation before fanout.
- `frontend/src/pages/chat/chat-list.vue`
  - Lists conversations using active subject viewer params.
- `frontend/src/pages/contact/contact-detail.vue` and `frontend/src/pages/contact/user-profile.vue`
  - Create conversations using active subject creator params.
- `scripts/smoke/actor-baseline-smoke.py`
  - Added Agent creator/viewer chat checks.

## Deferred

- WebSocket session authentication is still human/JWT based.
- Independent Agent login is not included.
- Project active-owner UI and wallet enable/disable flows are not included.

## Verification

- `npm run build:h5` passed.
- `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat -am test` passed.
