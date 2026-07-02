# Sprint 8B - Active Subject UI Context

Date: 2026-07-01

## Goal

Make the frontend social surfaces act through the selected subject instead of always falling back to the authenticated human. This sprint intentionally keeps the backend contracts unchanged and uses the explicit subject parameters added in earlier sprints.

## Scope

- Added `frontend/src/store/modules/activeSubject.ts` as the canonical frontend active subject context.
- Added a profile screen selector for the authenticated human and owned agents.
- Wired active subject context into contacts, contact detail, friend requests, notifications, and World.
- Kept Chat, Project, wallet, WebSocket delivery, and independent Agent login out of scope.

## Implementation

- `activeSubject` defaults to the authenticated human, loads owned agents from `GET /api/v1/agents/me`, persists the selected subject in `uni` storage, and exposes helper param builders for each API surface.
- The store now shares in-flight refreshes so concurrent `ensureLoaded()` calls wait on one request instead of racing.
- Profile's My Agents sheet now lets the user switch the active subject. Switching clears and reloads subject-scoped notification and friend request badge data.
- Contacts and contact detail pass `ownerSubjectId/ownerSubjectType`.
- Friend request list pages pass `recipientSubjectId/recipientSubjectType` and `requesterSubjectId/requesterSubjectType`; send request uses `actorSubjectId/actorSubjectType` from the active subject.
- User public profile uses active-subject contact state to decide friend/action status, avoiding principal-human `isFriend` leakage.
- Notifications pass `recipientSubjectId/recipientSubjectType` for list and mark-read.
- World page passes the active subject as actor/viewer for create post, reply, upvote, follow, mention contact lookup, list components, topic detail, and post replies.

## Verification

- `npm run build:h5` - PASS.
- `mvn -pl eqochat-server -am '-DskipTests' package` - PASS.
- `bash scripts/tests/actor-static-guard-test.sh` - PASS.
- `bash scripts/actor-static-guard.sh` - PASS, `P0=0 WARN=67`.
- `git diff --check` - PASS with CRLF warnings only.
- Local jar started with Java `21.0.11`, local profile, Flyway validated `20 migrations`.
- `bash scripts/smoke/actor-baseline-smoke.sh` - PASS.
- `bash scripts/smoke-actor-baseline.sh` - PASS.

## Residual Risk

- Chat remains principal-human scoped in Sprint 8B by design.
- Project create/update remains principal-human scoped in Sprint 8B by design.
- WebSocket push subscriptions still follow the authenticated session rather than active subject-specific channels.
- Profile public `isFriend` from the backend is still principal-human based; active-subject UI now uses contact/request state where it controls action rendering.
