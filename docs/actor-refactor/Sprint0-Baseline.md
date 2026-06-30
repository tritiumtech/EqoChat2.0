# Sprint 0 Baseline

Sprint 0 freezes the current working state before the Actor Model refactor. The tree is intentionally not clean: prior Sprint 1A seed data, Actor foundation files, and frontend display updates are part of the current baseline and must not be reverted.

## Current Runtime

- Backend: `http://localhost:8080`, Spring profile `local`
- Frontend H5: `http://localhost:3000`
- Demo login: phone `13900000001`, password `Test1234`
- Email login debt: `POST /api/v1/auth/login/email` exists but is currently blocked by security config; use `POST /api/v1/auth/login` for smoke.

## Protected API Baseline

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/agents/me`
- `GET /api/v1/contacts`
- `GET /api/v1/contacts/{contactId}`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `GET /api/v1/projects/{projectId}/sidebar/tasks`
- `GET /api/v1/projects/{projectId}/sidebar/payments`
- `GET /api/v1/projects/{projectId}/sidebar/files`
- `GET /api/v1/world/posts`
- `POST /api/v1/world/posts`
- `POST /api/v1/world/posts/{postId}/replies`
- `GET /api/v1/conversations`
- `POST /api/v1/conversations`
- `GET /api/v1/conversations/{conversationId}/messages`
- `POST /api/v1/conversations/{conversationId}/messages`
- `POST /api/v1/conversations/{conversationId}/read`

## Demo Records

- Human login subject: John Doe, id `2`
- Human chat/contact target: Sarah Chen, id `11`
- Agent examples: Nova `101`, Luna `102`
- Project examples: `10001` through `10004`
- World seed posts include human and agent authors; feed must keep returning at least one `author.type == "agent"`.

## Known Actor Debt

- Chat still hardcodes `USER` in participant, sender, read receipt, and WebSocket paths.
- Notification still stores and queries recipients by `recipient_id` without consistently using recipient type.
- Credit still mixes `USER/AGENT` with the target `HUMAN/AGENT/SYSTEM` vocabulary.
- World display can render agent authors, but create/reply/mention/upvote/follow paths remain human-centric.
- Contact display uses `SubjectDirectoryApi`, but add/contact graph still depends on `user_friend` and mirror user compatibility.
- Project has useful `HUMAN/AGENT` structure, but many write paths still require the current human user.
- Some Agent demo data still depends on mirror `user_profile` rows. This is a transition mechanism, not a future design.

## Sprint 0 Gates

Run these before entering Sprint 1:

```bash
mvn -pl eqochat-server -am -DskipTests package
bash -n scripts/smoke/actor-baseline-smoke.sh scripts/actor-static-guard.sh
ALLOW_EXISTING=1 scripts/actor-static-guard.sh
scripts/smoke/actor-baseline-smoke.sh
```

The static guard currently reports existing P0 findings in baseline mode. Those findings are not accepted as future design; they are the reduction target for the refactor Sprints.
