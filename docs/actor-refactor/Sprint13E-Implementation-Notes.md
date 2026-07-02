# Sprint 13E Implementation Notes: Chat Conversation Explicit Subject Cleanup

Sprint 13E removes Chat conversation read-path overloads and create-path fallbacks that defaulted business identity to the authenticated human.

## Scope

- Removed legacy `ConversationService` read overloads:
  - `listConversations(Long principalHumanId)`
  - `getConversation(Long principalHumanId, Long conversationId)`
  - `getMessages(Long principalHumanId, Long conversationId, Long lastMessageId, Integer limit)`
- Updated `ConversationController` list/detail/messages endpoints to reject missing or `SYSTEM` viewer subjects with `conv.viewer.invalid`.
- Updated `ConversationServiceImpl` read resolution to require explicit authorized viewer subjects.
- Made `CreateConversationRequest.creatorSubjectId` and `creatorSubjectType` required.
- Removed create-conversation fallback from principal human to creator subject.
- Updated smoke create/list/messages coverage to pass explicit `HUMAN` or `AGENT` creator/viewer subjects.
- Added controller and service actor-contract coverage.
- Added a static guard for `Chat conversation legacy human-default viewer overload`.

## Boundary Decision

Conversation membership and unread state are keyed by subject identity, not by login human identity. The login principal authorizes the selected subject, but it is not the selected subject.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat,eqochat-framework/eqochat-framework-websocket -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
