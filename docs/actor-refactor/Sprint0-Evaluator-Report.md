# Sprint 0 Evaluator Report

Status: ready for final Evaluator audit.

## Local Verification Snapshot

Executed on 2026-06-29 15:41 Asia/Shanghai.

| Check | Result |
| --- | --- |
| `mvn -pl eqochat-server -am -DskipTests package` | PASS, reactor build success |
| `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh` | PASS |
| `ALLOW_EXISTING=1 scripts/actor-static-guard.sh` | PASS in baseline report mode |
| `scripts/smoke/actor-baseline-smoke.sh` | PASS |
| `curl -I http://localhost:3000` | PASS, HTTP 200 |

## Smoke Coverage

The baseline smoke currently verifies:

- phone login with demo user `13900000001`
- `GET /api/v1/auth/me`
- `GET /api/v1/agents/me`
- `GET /api/v1/contacts`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- project sidebar tasks, payments, files
- `GET /api/v1/world/posts`
- `POST /api/v1/world/posts`
- `POST /api/v1/world/posts/{postId}/replies`
- `POST /api/v1/conversations`
- `GET /api/v1/conversations`
- `GET /api/v1/conversations/{conversationId}/messages`
- `POST /api/v1/conversations/{conversationId}/messages`
- `POST /api/v1/conversations/{conversationId}/read`

The smoke creates one `ACTOR_SMOKE` World post, one reply, and one chat message per run.

## Static Guard Baseline

`ALLOW_EXISTING=1 scripts/actor-static-guard.sh` reported:

- P0: 17
- WARN: 27

The P0 set is treated as known existing debt, not as approved future design. Main clusters:

- Chat `senderType("USER")`, `ParticipantType.USER`, `ReaderType.USER`
- Notification `RecipientType.USER` and `SenderType.USER`
- Frontend chat/WebSocket `senderType: 'USER'`
- World mention notification sender/recipient legacy usage

## Known Baseline Findings

- Email login endpoint exists but is currently not permitted by security config; the official smoke uses phone login.
- Static guard is expected to report legacy P0 findings under `ALLOW_EXISTING=1`.
- Sprint 0 does not fix Chat/Notification/Credit `USER` hardcodes; it records them as future Sprint gates.
- There are no automated unit/integration tests in the backend test tree yet; Sprint-specific tests must be introduced as refactor work touches modules.
