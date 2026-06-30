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
- `SMOKE_TAG`: marker for generated smoke data, default `ACTOR_SMOKE`

The script creates one World post, one reply, and one chat message on every run. These records are intentionally marked with `ACTOR_SMOKE` so they can be filtered later if needed.

Prerequisites:

- Backend is running with the local/demo seed data.
- `curl` and `jq` are available.
- The local login seed from `V10__sprint1a_seed_data.sql` exists.

Known Sprint 0 debt:

- `POST /api/v1/auth/login/email` exists in the controller but is not currently permitted by `SecurityConfig`; this smoke uses phone login until that is fixed deliberately.
