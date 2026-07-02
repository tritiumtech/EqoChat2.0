# Actor Smoke Scripts

`actor-baseline-smoke.sh` is the Sprint 0 regression smoke for the Actor Model refactor.

Run from the repository root:

```bash
scripts/smoke/actor-baseline-smoke.sh
```

The legacy root entry point is also kept:

```bash
scripts/smoke-actor-baseline.sh
```

Environment variables:

- `BASE_URL`: backend URL, default `http://localhost:8080`
- `PHONE`: demo login phone, default `13900000001`
- `PASSWORD`: demo password, default `Test1234`
- `TARGET_USER_ID`: human user used for single-chat smoke, default `11`
- `TARGET_SUBJECT_ID`: target subject id used for single-chat smoke, default `11`
- `TARGET_SUBJECT_TYPE`: target subject type used for single-chat smoke, default `HUMAN`
- `SMOKE_TAG`: marker for generated smoke data, default `ACTOR_SMOKE`

The script creates one World post, one reply, and one chat message on every run. These records are intentionally marked with `ACTOR_SMOKE` so they can be filtered later if needed.

The Agent check verifies both the legacy `/api/v1/agents/me` fields and the Sprint 7A canonical policy fields (`agentSubject*`, `ownerSubject*`, wallet policy, direct recipient, liability route).

The Agent Wallet check verifies Sprint 8F owner-controlled wallet writes by disabling an enabled owned Agent wallet, confirming `/api/v1/agents/me` reflects `AGENT_TO_OWNER`, enabling it again, and confirming `AGENT_DIRECT` canonical wallet facts.

The Subject check verifies canonical `/api/v1/subjects` search/public profile responses and asserts the retired legacy `/api/v1/users/search` route returns HTTP 404.

The Project write check verifies Sprint 8G runtime coverage for Agent actor payment creation, frozen payment wallet/liability facts, ownership transfer from Agent to Human, and owner-only rejection for the old Agent actor after transfer.

The Contact check verifies the default human-owned list and the Sprint 7B explicit Agent owner query (`ownerSubjectId + ownerSubjectType`). Agent-owned results may be empty in a local seed, but the endpoint must succeed and must not emit legacy `USER` subject types.

The Notification check verifies the Sprint 7C explicit Agent recipient query (`recipientSubjectId + recipientSubjectType`). Agent recipient inboxes may be empty in a local seed, but the endpoint must succeed and must not emit legacy `USER` subject types.

The Credit check verifies the Sprint 7D Credit Profile score surface by loading a contact target's `/api/v1/credits/subject` profile and asserting `creditScore` is in the PRD `300-850` range.

The Friend Request check verifies the Sprint 7E explicit Agent inbox queries for `/api/v1/friend-requests/received` and `/api/v1/friend-requests/sent`. Agent request lists may be empty in a local seed, but the endpoints must succeed, remain scoped to the requested Agent subject when rows exist, and must not emit legacy `USER` subject types.

Prerequisites:

- Backend is running with the local/demo seed data.
- Python 3 is available as `python`, `python3`, or `py -3`.
- `jq` is not required; JSON parsing is handled by the Python standard library.
- The local login seed from `V10__sprint1a_seed_data.sql` exists.

When the script runs inside WSL and `BASE_URL` points to `localhost`, it automatically retries the Windows host address from `/etc/resolv.conf` or `host.docker.internal`. This keeps the default usable when the backend jar is started from Windows.

Sprint 10 expectations:

- `/api/v1/auth/me` and login responses expose actor-directory points and PRD-scale credit score.
- Static guard summary should report `P0=0` and `WARN_RUNTIME=0`; historical migration/document warnings may remain as `WARN_HISTORY`.

Known Sprint 0 debt:

- `POST /api/v1/auth/login/email` exists in the controller but is not currently permitted by `SecurityConfig`; this smoke uses phone login until that is fixed deliberately.
