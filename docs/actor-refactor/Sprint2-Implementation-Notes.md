# Sprint 2 Implementation Notes: Chat Actorization

## Scope

- Chat conversation creation now targets `targetSubjectId + targetSubjectType`.
- Chat send/read paths resolve an actor `SubjectRef` and keep `principalHumanId`, `actorSubject`, and `liableHumanId` distinct.
- Conversation participants, message senders, read receipts, unread counts, summaries, and WebSocket chat/read/typing/presence payloads use subject identity.
- WebSocket session plumbing names the authenticated session owner as `principalHumanId`; legacy `?userId=` WebSocket fallback was removed.
- WebSocket write messages must carry complete `senderSubjectId + senderSubjectType`; the server no longer infers sender subject from a missing payload for compatibility.
- JWT tokens now emit `principalHumanId` as the custom login-subject claim, with `sub` as the same principal id. Frontend token parsing no longer accepts legacy `userId/id/uid` claim names.
- Login-session Redis keys now use `principal_human:session:*` and `session:principal_human:*`; old local sessions must log in again.
- Frontend chat API, WebSocket types, chat room, chat list, and chat store consume canonical subject fields.
- Notification and Credit write-path enum values were cleaned from `USER` to `HUMAN` so the strict actor static guard has no P0 findings.

## Migrations

- `V12__chat_actor_identity.sql`
  - Adds `message.liable_human_id`.
  - Converts chat `USER` rows to `HUMAN`.
  - Backfills message liability for human and agent senders through `agent_binding.binding_status`.
- `V13__notification_actor_identity.sql`
  - Converts notification `recipient_type/sender_type = USER` rows to `HUMAN`.
- `V14__credit_actor_identity.sql`
  - Converts credit `subject_type = USER` rows to `HUMAN`.
  - Updates notification column comments/defaults to canonical actor vocabulary.
- Flyway is enabled by default only under `local`; default profile remains opt-in through `FLYWAY_ENABLED`.

## Compatibility Position

The app is pre-consumer, so Sprint 2 does not preserve old DTO, Credit, or WebSocket field names for compatibility. Normal API, WebSocket, domain service, and write paths must not accept or emit `USER`. `USER` appears only in migration cleanup, negative tests, and historical documentation.

## Gates

- `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat -am test`: PASS.
- `mvn -pl eqochat-business/eqochat-notification-parent/eqochat-notification,eqochat-business/eqochat-world-parent/eqochat-world -am test`: PASS.
- `mvn -pl eqochat-server -am -DskipTests package`: PASS.
- `npm run build:h5`: PASS.
- `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh`: PASS.
- `scripts/actor-static-guard.sh`: PASS with `P0=0`, `WARN=34`.
- Backend local profile startup from latest jar: PASS.
- Flyway local state: baseline v11, V12 and V13 applied successfully, schema at v13.
- `scripts/smoke/actor-baseline-smoke.sh`: PASS.

## Known Residual Debt

- Historical migrations and baseline docs still contain `USER/AGENT` text as transition context.
- Credit records and credit frontend types now use `HUMAN/AGENT/SYSTEM`; `USER` is rejected at runtime.
- World still has transitional mirror-profile warnings and `author_ai` display helper fields; these are not authoritative identity fields.
