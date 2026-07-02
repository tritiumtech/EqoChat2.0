# Sprint 7E Implementation Notes: Friend Request Explicit Subject Inbox

Date: 2026-07-01

## Scope

Sprint 7E makes Friend Request inbox endpoints accept explicit subject parameters while preserving existing default behavior.

- `GET /api/v1/friend-requests/received` accepts optional `recipientSubjectId + recipientSubjectType`.
- `GET /api/v1/friend-requests/sent` accepts optional `requesterSubjectId + requesterSubjectType`.
- Missing subject parameters keep the existing behavior: aggregate the authenticated human plus active owned Agents.
- Explicit Agent subject access is authorized by `LiabilityPolicyApi`; the Agent must resolve to the current principal human.
- Returned request rows continue to expose canonical requester/recipient subject fields.

## Non-Scope

- No friend request storage migration.
- No change to send/accept/reject semantics.
- No Agent switcher UI.
- No Subject Registry.
- No `USER` compatibility alias.

## Key Changes

- `FriendRequestController` now parses optional explicit inbox subject query parameters and rejects partial or `SYSTEM` subject parameters with `friend_request.subject.invalid`.
- `FriendRequestService` now exposes overloaded `listReceived(principalHumanId, recipient)` and `listSent(principalHumanId, requester)` methods.
- `FriendRequestServiceImpl` uses the old aggregate principal subject list when no explicit subject is provided.
- When explicit subject is provided, `FriendRequestServiceImpl` authorizes that subject through liability policy and queries only that subject.
- Frontend `friendRequestApi.listReceived` and `listSent` accept optional subject params without changing existing callers.
- Runtime smoke now checks explicit Agent received/sent request inbox queries.

## Tests

Added `FriendRequestControllerSubjectInboxTest` covering:

- default received inbox remains principal-scoped when no subject params are provided;
- explicit Agent requester is passed to sent inbox service call;
- partial and `SYSTEM` inbox subject params are rejected before service calls.

Extended `FriendRequestServiceImplActorContractTest` covering:

- default received inbox includes principal human and active owned Agents;
- explicit Agent sent inbox is authorized and scoped to that Agent only;
- unauthorized explicit Agent inbox is rejected before mapper queries.

## Verification Checklist

- PASS: `cd backend && mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
  - Contact implementation module result: `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`.
- PASS: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`
  - Reactor result: `BUILD SUCCESS`.
- PASS: `cd frontend && npm run build:h5`
  - H5 build result: `DONE Build complete`.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh`
  - Summary: `P0=0 WARN=67`.
- PASS: `git diff --check`
  - Only CRLF normalization warnings, no whitespace errors.
- PASS: latest backend jar restarted with `--spring.profiles.active=local`
  - Java: `21.0.11`.
  - Flyway: `Successfully validated 20 migrations`; current schema version `20`.
- PASS: `bash scripts/smoke/actor-baseline-smoke.sh`
  - Includes `PASS friend requests agent received` and `PASS friend requests agent sent`.
- PASS: `bash scripts/smoke-actor-baseline.sh`
  - Includes `PASS friend requests agent received` and `PASS friend requests agent sent`.
- PASS: Evaluator explicit `PASS`
  - Findings: none blocking.
  - Residual risk: local smoke allows empty Agent friend request inboxes, so runtime coverage proves endpoint success and scoped response shape rather than non-empty Agent request data.

## Known Residuals

- Friend Request APIs are still human-session APIs. They allow selecting a requester or recipient subject, but they do not authenticate as that subject.
- Runtime smoke allows empty Agent request inboxes because local seed data may not include Agent-specific friend requests.
- The default aggregate inbox remains for compatibility with existing UI flows until an Agent switcher exists.
